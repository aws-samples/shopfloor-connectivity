// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.j1939

import com.amazonaws.sfc.canbus.CanInterface
import com.amazonaws.sfc.canbus.RawCanSocket
import com.amazonaws.sfc.canbus.TimestampedCanFrame
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.j1939.config.*
import com.amazonaws.sfc.j1939.config.J1939AdapterConfiguration.Companion.CONFIG_MAX_RETAIN_PERIOD
import com.amazonaws.sfc.j1939.config.J1939AdapterConfiguration.Companion.CONFIG_MAX_RETAIN_SIZE
import com.amazonaws.sfc.j1939.protocol.*
import com.amazonaws.sfc.j1939.protocol.J1939Decoder.Companion.bytesToLongBigEndian
import com.amazonaws.sfc.j1939.protocol.J1939Decoder.Companion.bytesToLongLittleEndian
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
import com.amazonaws.sfc.system.DateTime.systemDateTime
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.MemoryMonitor.Companion.getUsedMemoryMB
import com.amazonaws.sfc.util.asHexString
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.isJobCancellationException
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.commons.lang3.SystemUtils
import java.time.Instant
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.DurationUnit


class J1939Adapter(
    private val adapterID: String,
    private val configuration: J1939Configuration,
    private val logger: Logger
) : ProtocolAdapter {

    private val className = this::class.simpleName.toString()

    class Frame(val canId: Int, val data: ByteArray, val timestamp: Instant, val canSocketName: String) {
        override fun toString(): String {
            return "CanID ${canId.asHexString()}, Data [${data.asHexString()}], socket $canSocketName, $timestamp"
        }
    }


    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
        if (!SystemUtils.IS_OS_UNIX) {
            logger.getCtxErrorLog(className, "init")("Unsupported OS: This J1939 Adapter requires POSIX compliant OS implementing SocketCan")
            exitProcess(1)
        }
    }

    private val adapterConfiguration by lazy {
        val config = configuration.j1939ProtocolAdapters[adapterID]
        if (config == null) {
            logger.getCtxErrorLog(className, "adapterConfiguration")("\"$adapterID\" is not a valid adapter, configured adapters are ${configuration.j1939ProtocolAdapters.keys}")
        }
        config
    }

    // channel to send data changes to the coroutine that is handling these changes
    private val receivedDataFrames = Channel<Frame>(
        adapterConfiguration?.receivedDataChannelSize ?: J1939AdapterConfiguration.DEFAULT_RECEIVED_DATA_CHANNEL_SIZE
    )

    private val scope = buildScope("J1939 Protocol Handler")

    private var waitUntilForCanSocket : MutableMap<String, Instant?> = sources.keys.associate { it to null }.toMutableMap()

    private fun pgnAsString(pgnId: UInt): String {
        val name = j1939Dbc?.pgnByPgnId(pgnId)?.name
        return "[${if (name != null) "$name:" else ""}$pgnId(0x${pgnId.asHexString().trimStart('0')})]"
    }

    private fun addressAsString(address: UByte): String = "${address}(0x${address.asHexString()})"

    private val j1939Dbc: J1939Dbc? =
        if (adapterConfiguration?.dbcFile != null) {
            try {
                J1939Dbc(adapterConfiguration!!.dbcFile!!, logger).load()
            } catch (e: Exception) {
                logger.getCtxErrorLog(className, "dbcHelper")("Error loading dbc file ${adapterConfiguration!!.dbcFile!!.name}, $e")
                null
            }
        } else null


    // Map indexed by PGN with entries containing the sources with channels for this PGN
    private val pgnSourcesMap: Map<UInt, Map<String, J1939SourceConfiguration>> =
        configuration.sources.flatMap { (sourceId, sourceConfig) ->
            sequence {
                sourceConfig.channels.map { (channelId, channelConfig) ->
                    val pgnId: UInt? = pgnForChannel(channelId, channelConfig)?.pngId
                    if (pgnId != null) yield(pgnId to Pair(sourceId, sourceConfig))
                }
            }
        }.groupBy { it.first }.map { it.key to it.value.associate { it.second } }.toMap()


    // Map containing the SPNs indexed by source/channel
    private val channelSpnMap: Map<String, Map<String, List<J1939Signal>>> = sources.map { (sourceId, sourceConfig) ->
        sourceId to sourceConfig.channels.map { (channelId, channelConfig) ->
            channelId to signalsForChannel(channelId, channelConfig)
        }.toMap()
    }.toMap()

    private val adapterMetricDimensions = mapOf(MetricsCollector.METRICS_DIMENSION_TYPE to className)

    private val sources
        get() = configuration.sources.filter { it.value.protocolAdapterID in configuration.j1939ProtocolAdapters.keys }

    override val metricsCollector: MetricsCollector? by lazy {
        val metricsConfigurations =
            configuration.j1939ProtocolAdapters.map { it.key to (it.value.metrics ?: MetricsSourceConfiguration()) }
                .toMap()
        if (configuration.isCollectingMetrics) {
            logger.metricsCollectorMethod = collectMetricsFromLogger
            MetricsCollector(
                metricsConfig = configuration.metrics,
                metricsSourceType = MetricsSourceType.PROTOCOL_ADAPTER,
                metricsSourceConfigurations = metricsConfigurations,
                staticDimensions = ADAPTER_METRIC_DIMENSIONS,
                logger = logger
            )
        } else null
    }

    private val collectMetricsFromLogger: MetricsCollectorMethod? =
        if (configuration.isCollectingMetrics) {
            { metricsList ->
                try {
                    val dimensions = mapOf(METRICS_DIMENSION_SOURCE to adapterID) + adapterMetricDimensions
                    val dataPoints =
                        metricsList.map { MetricsDataPoint(it.metricsName, dimensions, it.metricUnit, it.metricsValue) }
                    runBlocking {
                        metricsCollector?.put(adapterID, dataPoints)
                    }
                } catch (e: java.lang.Exception) {
                    logger.getCtxErrorLogEx(className, "collectMetricsFromLogger")("Error collecting metrics from logger", e)
                }
            }
        } else null


    // Store received data
    private val sourceDataStores: Map<String, SourceDataStore<ChannelReadValue>> = sources.keys.associate { sourceID ->
        sourceID to if (adapterConfiguration?.readMode == J1939ReadMode.KEEP_LAST)
            SourceDataValuesStore()
        else
            SourceDataMultiValuesStore<ChannelReadValue>(
                adapterConfiguration?.maxRetainSize ?: 0,
                adapterConfiguration?.maxRetainPeriod ?: 0
            )
            { channel, duration, size, full ->
                if (full) {
                    val ctxWarningLog = logger.getCtxWarningLog(className, "sourceDataStores")
                    if (size != null)
                        ctxWarningLog("Source \"$sourceID\", channel \"$channel\" number of kept values reached maximum of $size values, oldest values are dropped, consider a larger $CONFIG_MAX_RETAIN_SIZE for adapter or a faster reading interval.")
                    else
                        ctxWarningLog("Source \"$sourceID\", channel \"$channel\" expired items older than configured $CONFIG_MAX_RETAIN_PERIOD $duration are being dropped, consider a larger a faster reading interval.")
                } else {
                    val ctxInfoLog = logger.getCtxInfoLog(className, "sourceDataStores")
                    if (size != null)
                        ctxInfoLog("Source \"$sourceID\", channel \"$channel\" number of kept values is now again below maximum of $size values.")
                    else
                        ctxInfoLog("Source \"$sourceID\", channel \"$channel\" no more expired values older than ${duration}are being dropped.")
                }
            }
    }

    private val canSocketsUsedBySources: List<String> =
        if (adapterConfiguration != null) {
            adapterConfiguration!!.canSockets.keys.filter{
                it in sources.values.map { s -> s.adapterCanSocket }
            }
        } else emptyList()


    private val readCanSocketTasks: List<Job> =
        canSocketsUsedBySources.map { canSocketName ->
            scope.launch {
                readCanSocketTask(canSocketName)
            }
        }


    private suspend fun readCanSocketTask(canSocketName: String) {

        val log = logger.getCtxLoggers(className, "canbusReader-$canSocketName")

        val canSocketConfig = adapterConfiguration?.canSockets?.get(canSocketName)
        if (canSocketConfig == null){
            log.error("Can socket $canSocketName is not configured for adapter $adapterID, configured can sockets are ${adapterConfiguration!!.canSockets.keys}")
            return
        }
        val socketName = canSocketConfig.socketName

        var socket: RawCanSocket? = null
        while (scope.isActive && adapterConfiguration != null) {
            try {

                log.trace("Opening socket \"$socketName\"}")
                val readTimestamp = adapterConfiguration!!.readTimeStamp
                socket = RawCanSocket()
                socket.open()

                if (readTimestamp) {
                    socket.timestampEnabled = true
                }

                val canbusInterface = CanInterface(socketName)
                socket.bind(canbusInterface)

                while (scope.isActive) {
                    if (readTimestamp) {
                        val frame: TimestampedCanFrame = socket.receiveTimestampedFrom(canbusInterface)
                        receivedDataFrames.send(Frame(frame.canId.id, frame.data, frame.timestamp, socketName))
                    } else {
                        val frame = socket.receiveFrom(canbusInterface)
                        receivedDataFrames.send(Frame(frame.canId.id, frame.data, systemDateTime(), socketName))
                    }
                }
            } catch (e: Exception) {
                if (!e.isJobCancellationException) {

                    val openingError = e.message?.contains(REGEX_PORT_NOT_FOUND) == true

                    val waitPeriod = if (openingError) adapterConfiguration!!.waitAfterOpenErrors else adapterConfiguration!!.waitAfterReadErrors
                    val waitUntil = Instant.ofEpochMilli(systemDateTime().toEpochMilli() + waitPeriod.toLong(DurationUnit.MILLISECONDS))
                    waitUntilForCanSocket[canSocketName] = waitUntil

                    if (openingError)
                        log.info(("Could not find CAN socket\"${socketName}\", paused reading from ${socketName} for ${adapterConfiguration!!.waitAfterOpenErrors} until $waitUntil"))
                    else
                        log.error("Error reading from CAN socket \"${socketName}\", paused reading from ${socketName} for ${adapterConfiguration!!.waitAfterReadErrors} until $waitUntil, $e ")

                    delay(waitUntilForCanSocket[canSocketName]!!.toEpochMilli() - systemDateTime().toEpochMilli())
                }
            } finally {
                socket?.close()
            }
        }
    }


    private val receivedJ1939FrameHandler =
        scope.launch(context = Dispatchers.Default, name = "Receive J1939 Frame Handler") {
            receivedPacketTask(receivedDataFrames, this)
        }

    private suspend fun receivedPacketTask(channel: Channel<Frame>, scope: CoroutineScope) {
        while (scope.isActive) {
            var frame: Frame? = null
            try {
                frame = channel.receive()
                handleDataReceived(frame)
            } catch (e: Exception) {
                if (!e.isJobCancellationException) logger.getCtxErrorLogEx(className, "changedDataWorker")("Error processing received canbus frame $frame", e)
            }
        }
    }


    override suspend fun read(sourceID: String, channels: List<String>?): SourceReadResult {

        val log = logger.getCtxLoggers(className, "read")

        val socketUsedBySource = socketUsedBySource(sourceID)
        val waitUntilForSourceSocket = waitUntilForCanSocket[socketUsedBySource]


        if (waitUntilForSourceSocket != null && waitUntilForSourceSocket.isAfter(systemDateTime())) {
            log.trace("Waiting until $waitUntilForCanSocket for reading from adapter $adapterID")
            return SourceReadSuccess(emptyMap())
        } else {
            log.trace("Reading from adapter $adapterID continued")
            if (socketUsedBySource != null ) waitUntilForCanSocket[socketUsedBySource] = systemDateTime()
        }

        val dimensions = mapOf(METRICS_DIMENSION_SOURCE to "$adapterID:$sourceID") + adapterMetricDimensions

        // Get the store where received values for this source are stored
        val store = sourceDataStores[sourceID]

        val start = systemDateTime().toEpochMilli()

        // Get the values and return result
        val data = (store?.read(channels) ?: emptyList()).associate {
            it.first to it.second
        }

        val readDurationInMillis = (systemDateTime().toEpochMilli() - start).toDouble()

        createMetrics(adapterID, dimensions, readDurationInMillis, data.size)

        val d = data.map {
            it.key to ChannelReadValue(it.value)
        }.toMap()

        return SourceReadSuccess(d, systemDateTime())
    }

    private fun socketUsedBySource(sourceID: String): String? = adapterConfiguration?.canSockets?.get(sources[sourceID]?.adapterCanSocket)?.socketName


    private fun createMetrics(
        protocolAdapterID: String,
        metricDimensions: MetricDimensions?,
        readDurationInMillis: Double,
        values: Int
    ) {
        metricsCollector?.put(
            protocolAdapterID,
            metricsCollector?.buildValueDataPoint(
                protocolAdapterID,
                MetricsCollector.METRICS_MEMORY,
                getUsedMemoryMB().toDouble(),
                MetricUnits.MEGABYTES,
                metricDimensions
            ),
            metricsCollector?.buildValueDataPoint(
                protocolAdapterID,
                MetricsCollector.METRICS_READS,
                1.0,
                MetricUnits.COUNT,
                metricDimensions
            ),
            metricsCollector?.buildValueDataPoint(
                protocolAdapterID,
                MetricsCollector.METRICS_READ_DURATION,
                readDurationInMillis,
                MetricUnits.MILLISECONDS,
                metricDimensions
            ),
            metricsCollector?.buildValueDataPoint(
                protocolAdapterID,
                MetricsCollector.METRICS_VALUES_READ,
                values.toDouble(),
                MetricUnits.COUNT,
                metricDimensions
            ),
            metricsCollector?.buildValueDataPoint(
                protocolAdapterID,
                MetricsCollector.METRICS_READ_SUCCESS,
                1.0,
                MetricUnits.COUNT,
                metricDimensions
            )
        )
    }

    override suspend fun stop(timeout: Duration) {
        scope.cancel()
        readCanSocketTasks.forEach { it.cancel() }
        receivedJ1939FrameHandler.cancel()

        // clear data stores
        sourceDataStores.values.forEach {
            it.clear()
        }
    }

    private val sourceBroadCastData = mutableMapOf<Pair<String, UByte>, ProtocolDataTransfer>()

    private fun handleDataReceived(frame: Frame) {

        val log = logger.getCtxLoggers(className, "handleDataReceived")

        log.trace("Received J1939 frame $frame")

        val canFrameIdentifier = CanFrameIdentifier(frame.canId.toUInt())
        log.trace("Can frame identifier: $canFrameIdentifier")

        when (val pgnId = canFrameIdentifier.pgn) {

            TRANSPORT_PROTOCOL_CONNECTION_MANAGEMENT -> {
                handleTransportProtocolConnectionManagement(canFrameIdentifier, frame)
            }

            TRANSPORT_PROTOCOL_DATA_TRANSFER -> {
                handleTransportProtocolDataTransfer(canFrameIdentifier, frame)
            }

            else -> {
                if (anySourceUsingPngFromAddress(pgnId, frame.canSocketName, canFrameIdentifier.sourceAddress)) {
                    handleSingleFrameData(canFrameIdentifier, frame)
                } else {
                    if (j1939Dbc?.pgnByPgnId(pgnId) != null)
                        log.trace(
                            "Dropping PGN ${pgnAsString(pgnId)} frame as there is no source configured to read this PGN from source address ${addressAsString(canFrameIdentifier.sourceAddress)}"
                        )
                    else
                        log.trace("Dropping PGN ${pgnAsString(pgnId)} frame as it is not a configured PGN in ${adapterConfiguration?.dbcFile?.absoluteFile?.name}")
                }
            }
        }
    }

    private fun anySourceUsingPngFromAddress(pgnId: UInt, canSocketName: String, sourceAddress: UByte): Boolean =
        sourcesUsingPgnFromSocketAndAddress(pgnId, canSocketName, sourceAddress).isNotEmpty()

    private fun sourcesUsingPgnFromSocketAndAddress(pgnId: UInt, socket: String, sourceAddress: UByte): Map<String, J1939SourceConfiguration> {
        return pgnSourcesMap[pgnId]?.filter { (it.value.sourceAddress == sourceAddress || it.value.sourceAddress == null) &&
                (adapterConfiguration?.canSockets?.get(it.value.adapterCanSocket)?.socketName == socket) }
                ?: emptyMap()
    }

    private fun handleSingleFrameData(canFrameIdentifier: CanFrameIdentifier, frame: Frame) {

        val log = logger.getCtxLoggers(className, "handlePng")

        val sourceAddressStr = addressAsString(canFrameIdentifier.sourceAddress)


        val pgn = j1939Dbc?.pgnByCanId(canFrameIdentifier.canId)
        if (pgn == null) {
            log.trace("Unknown PGN ${pgnAsString(canFrameIdentifier.pgn)} for from source address $sourceAddressStr")
            return
        }

        //   val data = payload.data.sliceArray(1..payload.data.size - 1)
        log.trace("Received ${pgn.name} PGN from can socket ${frame.canSocketName}, source address $sourceAddressStr\"data is [${frame.data.asHexString()}]")

        processPgnData(canFrameIdentifier.pgn, frame.canSocketName, canFrameIdentifier.sourceAddress, frame.data, frame.timestamp)

    }


    fun extractBits(bytes: ByteArray, signals: List<J1939Signal>): ByteArray {

        val result = bytes.copyOf()

        // Clear all bits in the result array
        for (i in result.indices) {
            result[i] = 0
        }

        // Process each bit range
        for (signal in signals) {
            // Process each bit in the current range
            for (bitIndex in signal.startBit until signal.startBit + signal.length) {
                val byteIndex = bitIndex / 8
                val bitPosition = 7 - (bitIndex % 8)

                // Get the bit from source array
                val bitValue = (bytes[byteIndex].toInt() shr bitPosition) and 1

                // Set the bit in result array if it was 1
                if (bitValue == 1) {
                    result[byteIndex] = (result[byteIndex].toInt() or (1 shl bitPosition)).toByte()
                }
            }
        }

        return result
    }



    private fun processPgnData(pgnId: UInt, canSocketName: String, sourceAddress: UByte, data: ByteArray, timestamp: Instant) {

        val pgn = j1939Dbc?.pgnByPgnId(pgnId) ?: return

        sourcesUsingPgnFromSocketAndAddress(pgnId, canSocketName, sourceAddress).keys.forEach { sourceId ->

            val channel = getChannelForPgnID(sourceId, pgnId)
            val channelID = channel?.first
            val store = sourceDataStores[sourceId]
            val rawFormat = (channel?.second?.rawFormat)?:sources[sourceId]?.rawFormat

            if (rawFormat != null && channelID != null) {

                val dataBits =  if (rawFormat.isMasked) {
                    val channelSignals = (channelSpnMap[sourceId]?.get(channelID)?:emptyList())
                    extractBits(data, channelSignals)
                } else {
                    data
                }

                val rawData: Any = when (rawFormat) {
                    J1939RawFormat.BYTES, J1939RawFormat.BYTES_MASKED -> dataBits
                    J1939RawFormat.LITTLE_ENDIAN, J1939RawFormat.LITTLE_ENDIAN_MASKED -> bytesToLongLittleEndian(dataBits)
                    J1939RawFormat.BIG_ENDIAN, J1939RawFormat.BIG_ENDIAN_MASKED -> bytesToLongBigEndian(dataBits)
                }
                store?.add(channelID, ChannelReadValue(rawData, timestamp))
                return
            }

            channelSpnMap[sourceId]?.forEach { (channelId, channelSignals) ->
                val pgnData = sequence {
                    channelSignals.forEach { signal ->
                        if (pgn.signals.contains(signal)) {
                            val value = J1939Decoder.decode(signal, data)
                            if (value != null) yield(signal.name to value)
                        }
                    }
                }.toMap()

                if (pgnData.isNotEmpty()) {
                    val channelValue = if (channelSignals.size == 1) {
                        pgnData.values.first()
                    } else {
                        pgnData
                    }
                    store?.add(channelId, ChannelReadValue(channelValue, timestamp))
                }
            }

        }
    }

    private fun getChannelForPgnID(sourceId: String, pgnId: UInt): Pair<String, J1939ChannelConfiguration>? {
        val channelEntry = sources[sourceId]?.channels?.filter { pgnForChannel(it.key, it.value)?.pngId == pgnId }?.entries?.firstOrNull()
        return channelEntry?.toPair()
    }


    private fun handleTransportProtocolConnectionManagement(canFrameIdentifier: CanFrameIdentifier, frame: Frame) {
        val log = logger.getCtxLoggers(className, "startTransportProtocolBroadcast")
        if (isBroadCastAnnouncement(frame)) {
            val bam = ProtocolDataTransfer.fromPayload(frame)
            log.trace("Received Transport Protocol Broadcast BAM from socket ${frame.canSocketName} source address , $bam")
            if (anySourceUsingPngFromAddress(bam.pgnId, frame.canSocketName, canFrameIdentifier.sourceAddress)) {
                sourceBroadCastData[frame.canSocketName to canFrameIdentifier.sourceAddress] = bam
                log.trace("Received Transport Protocol Broadcast BAM from socket ${frame.canSocketName}, source address, $bam")
            } else {
                bam.skip = true
                log.trace(
                    "Dropping transport protocol connection management frame, as there is no source configured to read this PGN ${pgnAsString(bam.pgnId)} from source address for socket ${frame.canSocketName}${
                        addressAsString(
                            canFrameIdentifier.sourceAddress)
                    }")
            }
            sourceBroadCastData[frame.canSocketName to canFrameIdentifier.sourceAddress] = bam

        }
    }

    private fun handleTransportProtocolDataTransfer(
        canFrameIdentifier: CanFrameIdentifier,
        frame: Frame,
    ) {
        val log = logger.getCtxLoggers(className, "handleTransportProtocolData")

        val sourceAddress = canFrameIdentifier.sourceAddress
        val sourceAddressStr = addressAsString(sourceAddress)
        val sourceTransportProtocolData = sourceBroadCastData[frame.canSocketName to sourceAddress]

        if (sourceTransportProtocolData == null) {
            log.trace("No active broadcast for source address $sourceAddressStr from socket ${frame.canSocketName}")
            return
        }

        val packetNumber = frame.data[0].toInt()
        if (sourceTransportProtocolData.skip) {
            log.trace("Dropping data transfer frame $packetNumber ${pgnAsString(canFrameIdentifier.pgn)} as PGN ${pgnAsString(sourceTransportProtocolData.pgnId)} is not used in a configured source from source address $sourceAddressStr from socket ${frame.canSocketName}")
            return
        }

        val data = frame.data.sliceArray(1..frame.data.size - 1)
        if (sourceTransportProtocolData.buffer != null && packetNumber * 7 + data.size <= sourceTransportProtocolData.buffer.size) {
            try {
                data.copyInto(
                    sourceTransportProtocolData.buffer,
                    sourceTransportProtocolData.receivedPackets * 7,
                    0,
                    data.size
                )
            } catch (e: Exception) {
                log.error("Error copying data into buffer, $e")
            }
            log.trace("Received data transfer packet $packetNumber from source address $sourceAddressStr) from socket ${frame.canSocketName}, data is [${data.map { data.asHexString() }}]")
        }
        sourceTransportProtocolData.receivedPackets += 1

        if (sourceTransportProtocolData.receivedPackets >= sourceTransportProtocolData.packets) {
            handleCompletedTransportProtocolDataTransfer(canFrameIdentifier, sourceTransportProtocolData, frame.canSocketName, frame.timestamp)
        }
    }

    private fun handleCompletedTransportProtocolDataTransfer(
        canFrameIdentifier: CanFrameIdentifier,
        sourceTransportProtocolData: ProtocolDataTransfer,
        canSocket: String,
        timestamp: Instant
    ) {

        val log = logger.getCtxLoggers(className, "completeOfTransportProtocolBroadcast")


        val sourceAddress = canFrameIdentifier.sourceAddress
        val sourceAddressStr = addressAsString(sourceAddress)

        val pgn = j1939Dbc?.pgnByCanId(sourceTransportProtocolData.pgnId)

        val pgnStr = pgnAsString(sourceTransportProtocolData.pgnId)
        if (pgn == null) {
            log.trace("Unknown PGN $pgnStr) for from source address $sourceAddressStr")
            return
        }
        if (sourceTransportProtocolData.buffer == null) {
            log.warning("No data for PGN $pgnStr from source address $sourceAddressStr")
            return
        }

        val lastSignalByte = pgn.signals.maxBy { it.startBit + it.length }
        val maxPayloadDataLen = (lastSignalByte.startBit.plus(lastSignalByte.length)) / 8
        val data: ByteArray = sourceTransportProtocolData.buffer.sliceArray(0..maxPayloadDataLen - 1)

        log.trace("Transport protocol data for PGN $pgnStr is ${data.asHexString()}")

        processPgnData(sourceTransportProtocolData.pgnId, canSocket, canFrameIdentifier.sourceAddress, data, timestamp)
        sourceBroadCastData.remove(canSocket to sourceAddress)
    }


    private fun isBroadCastAnnouncement(frame: Frame): Boolean = frame.data[0] == 0x20.toByte()

    private fun pgnForChannel(channelName: String, channel: J1939ChannelConfiguration): J1939PGN? {
        val errLog = logger.getCtxErrorLog(className, "pgnForChannel")
        val png = try {
            if (isNumeric(channel.pgn)) {
                val n = getUInt(channel.pgn.split(".").first())
                if (n != null)
                    j1939Dbc?.pgnByPgnId(n)
                else null
            } else {
                j1939Dbc?.pgnByName(channel.pgn)
            }
        } catch (e: Exception) {
            errLog("Error getting PGN for channel \"$channelName\", $e")
            return null
        }
        if (png == null) {
            errLog("PGN \"${channel.pgn}\"  not found for channel \"$channelName\" in DBC file")
        }
        return png
    }

    private fun signalsForChannel(channelName: String, channel: J1939ChannelConfiguration): List<J1939Signal> {

        val errLog = logger.getCtxErrorLog(className, "signalsForChannel")

        val pgn = pgnForChannel(channelName, channel)
        if (pgn == null) return emptyList()

        if (channel.spnList == null) {
            return pgn.signals
        }

        return sequence {
            channel.spnList!!.map {
                try {
                    if (isNumeric(it)) {
                        val id = getUInt(it.split(".").first())
                        if (id != null) {
                            val spn = j1939Dbc?.spnById(pgn.pngId, id)
                            if (spn == null) {
                                errLog("SPN \"$it\" not found for channel \"$channelName\" in DBC file")
                            } else {
                                val signal = pgn.signals.find { it.name == spn.name }
                                if (signal == null) {
                                    errLog("SPN \"$it\" not found in PNG \"${pgn.name}\" (${pgn.canId}) for channel \"$channelName\", available channels are ${j1939Dbc!!.spnListForPgn(pgn.canId)}")
                                } else yield(signal)
                            }
                        } else null

                    } else {
                        val signal = pgn.signals.find { signal -> signal.name == it }
                        if (signal == null) {
                            errLog("SPN \"$it\" not found in PNG \"${pgn.name}\" (${pgn.canId}) for channel \"$channelName\", available channels are ${pgn.signals.joinToString { it.name }}")
                        } else yield(signal)
                    }
                } catch (e: Exception) {
                    errLog("Error getting signal \"$it\" png \"${pgn.name}\" for channel \"$channelName\", $e")
                    null
                }
            }
        }.toList()
    }

    companion object {

        val TRANSPORT_PROTOCOL_CONNECTION_MANAGEMENT = 0xEC00.toUInt()
        val TRANSPORT_PROTOCOL_DATA_TRANSFER = 0xEB00.toUInt()

        private val REGEX_PORT_NOT_FOUND = Regex("Could not find interface with name.+")

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParams: Any) =
            newInstance(
                createParams[0] as ConfigReader,
                createParams[1] as String,
                createParams[2] as String,
                createParams[3] as Logger
            )

        internal fun canIdFromBytes(bytes: ByteArray): UInt {
            val b = bytes.sliceArray(0..3)
            val canId = ((b[0].toUInt() and 0xFFu) shl 24) or
                    ((b[1].toUInt() and 0xFFu) shl 16) or
                    ((b[2].toUInt() and 0xFFu) shl 8) or
                    (b[3].toUInt() and 0xFFu).toUInt()
            return canId
        }

        private val createInstanceMutex = Mutex()


        @JvmStatic
        fun newInstance(
            configReader: ConfigReader,
            scheduleName: String,
            adapterID: String,
            logger: Logger
        ): SourceValuesReader? {


            runBlocking {
                createInstanceMutex.withLock {
                    if (adapter == null) {
                        adapter = createJ1939Adapter(adapterID, configReader, logger)
                    }
                }
            }

            val config = configReader.getConfig<J1939Configuration>()
            val schedule = config.schedules.firstOrNull { it.name == scheduleName }
            val sourcesForAdapter =
                schedule?.sources?.filter { (config.sources[it.key]?.protocolAdapterID ?: "") == adapterID }
                        ?: return null

            return if (adapter != null) InProcessSourcesReader.createInProcessSourcesReader(
                schedule = schedule,
                adapter = adapter!!,
                sources = sourcesForAdapter,
                tuningConfiguration = config.tuningConfiguration,
                metricsConfig = config.metrics,
                logger = logger
            ) else null

        }

        private var adapter: ProtocolAdapter? = null

        fun createJ1939Adapter(adapterID: String, configReader: ConfigReader, logger: Logger): ProtocolAdapter {

            val config: J1939Configuration = try {
                configReader.getConfig()
            } catch (e: Exception) {
                throw TargetException("Error loading configuration: ${e.message}")
            }
            return J1939Adapter(adapterID, config, logger)
        }

        private val ADAPTER_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
        )
    }

    internal class ProtocolDataTransfer(
        val pgnId: UInt = 0u,
        val packets: Int = 0,
        val size: Int = 0,
        var receivedPackets: Int = 0,
        val buffer: ByteArray? = null,
        var skip: Boolean = false
    ) {
        companion object {

            fun fromPayload(frame: Frame): ProtocolDataTransfer {
                val pgnBytes = frame.data.sliceArray(frame.data.size - 3..frame.data.size - 1).reversed()
                val packets = frame.data[3].toInt()
                return ProtocolDataTransfer(
                    pgnId = ((pgnBytes[0].toUInt() and 0xFFu) shl 16) or
                            ((pgnBytes[1].toUInt() and 0xFFu) shl 8) or
                            ((pgnBytes[2].toUInt() and 0xFFu) shl 0),
                    packets = packets,
                    size = minOf(frame.data[1].toInt() or (frame.data[2].toInt() shl 8), packets * 7),
                    buffer = ByteArray(packets * 7),
                    receivedPackets = 0,
                    skip = false
                )
            }
        }

        override fun toString(): String {
            return "PGN=$pgnId(0x${pgnId.asHexString().trimStart('0')}), packets=$packets, size=$size"
        }


    }

}
