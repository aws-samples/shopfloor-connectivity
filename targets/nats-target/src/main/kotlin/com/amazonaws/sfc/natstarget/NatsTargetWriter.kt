// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.natstarget


import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.crypto.SSLHelper
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.data.JsonHelper.Companion.extendedJsonException
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_BYTES_SEND
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_MESSAGES
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITES
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_DURATION
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_ERRORS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_SIZE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_SUCCESS
import com.amazonaws.sfc.natstarget.config.NatsServerConfiguration.Companion.CONFIG_CREDENTIALS_FILE
import com.amazonaws.sfc.natstarget.config.NatsServerConfiguration.Companion.CONFIG_NKEY_FILE
import com.amazonaws.sfc.natstarget.config.NatsTargetConfiguration
import com.amazonaws.sfc.natstarget.config.NatsTargetConfiguration.Companion.CONFIG_ALTERNATE_SUBJECT_NAME
import com.amazonaws.sfc.natstarget.config.NatsTargetConfiguration.Companion.CONFIG_BATCH_COUNT
import com.amazonaws.sfc.natstarget.config.NatsTargetConfiguration.Companion.CONFIG_BATCH_INTERVAL
import com.amazonaws.sfc.natstarget.config.NatsTargetConfiguration.Companion.CONFIG_BATCH_SIZE
import com.amazonaws.sfc.natstarget.config.NatsTargetConfiguration.Companion.CONFIG_SUBJECT_NAME
import com.amazonaws.sfc.natstarget.config.NatsWriterConfiguration
import com.amazonaws.sfc.natstarget.config.NatsWriterConfiguration.Companion.NATS_TARGET
import com.amazonaws.sfc.targets.TargetDataChannel
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.targets.TargetFormatter
import com.amazonaws.sfc.targets.TargetFormatterFactory
import com.amazonaws.sfc.util.*
import com.amazonaws.sfc.util.TemplateRenderer.containsPlaceHolders
import com.amazonaws.sfc.util.TemplateRenderer.getPlaceHolders
import com.google.gson.JsonSyntaxException
import io.nats.client.Connection
import io.nats.client.Nats
import io.nats.client.Options
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.measureTime


class NatsTargetWriter(
    private val targetID: String,
    private val configReader: ConfigReader,
    private val targetConfig: NatsTargetConfiguration,
    private val logger: Logger,
    private val resultHandler: TargetResultHandler?
) : TargetWriter {

    private val className = this::class.java.simpleName

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val metricDimensions = mapOf(
        METRICS_DIMENSION_SOURCE to targetID,
        MetricsCollector.METRICS_DIMENSION_TYPE to className
    )

    val targetContext = buildContext("NATS-TARGET")

    private val buffers = ConcurrentHashMap<String, TargetDataBuffer>()// TargetDataBuffer(storeFullMessage = false)
    private val timers = ConcurrentHashMap<String, Job>()
    private val timerChannel = Channel<String>(capacity = 100)

    private val doesBatching by lazy { targetConfig.batchSize != 0 || targetConfig.batchCount != 0 || targetConfig.batchInterval != Duration.INFINITE }
    private val usesCompression = targetConfig.compressionType != CompressionType.NONE

    private var _natsConnection: Connection? = null

    private val formatter: TargetFormatter? by lazy {
        TargetFormatterFactory.createTargetFormatter(configReader, targetID, targetConfig, logger)
    }

    private val customPayload = (formatter != null)


    private suspend fun getConnection(): Connection? {

        val log = logger.getCtxLoggers(className, "getClient")

        val connectionName = "SFC-NATS-$targetID-${getHostName()}-${UUID.randomUUID()}"

        var retries = 0
        val natsServerConfiguration = targetConfig.natServerConfiguration
        while (_natsConnection == null && targetContext.isActive && retries < natsServerConfiguration.connectRetries) {
            try {
                _natsConnection = Nats.connect(
                    {

                        val builder = Options.Builder()
                            .connectionName(connectionName)
                            .server(natsServerConfiguration.url)

                        if (natsServerConfiguration.username != null && natsServerConfiguration.password != null) {
                            log.info("Using username/password authentication")
                            builder.userInfo(natsServerConfiguration.username!!.toCharArray(), natsServerConfiguration.password!!.toCharArray())
                        }

                        if (natsServerConfiguration.token != null) {
                            log.info("Using token authentication")
                            builder.token(natsServerConfiguration.token!!.toCharArray())
                        }

                        if (natsServerConfiguration.nkeyFile != null) {
                            log.info("Using NKey authentication, using $CONFIG_NKEY_FILE \"${natsServerConfiguration.nkeyFile}")
                            builder.authHandler(NKeyAuthHandler(natsServerConfiguration.nkeyFile!!, logger))
                        }

                        if (natsServerConfiguration.credentialsFile != null) {
                            log.info("Using JWT authentication, using $CONFIG_CREDENTIALS_FILE \"${natsServerConfiguration.credentialsFile}")
                            builder.authHandler(Nats.credentials(natsServerConfiguration.credentialsFile!!))
                        }

                        if (natsServerConfiguration.tlsConfiguration != null) {
                            log.info("Using SSL/TLS encryption")
                            builder.sslContext(SSLHelper(natsServerConfiguration.tlsConfiguration!!, logger).sslContext)
                            builder.secure()
                        }

                        builder.build()
                    }()
                )
                log.info("Connected to NATS server at ${natsServerConfiguration.url}, connection name id $connectionName")
            } catch (e: Exception) {
                logger.getCtxErrorLog(className, "natsConnection")("Error creating NATS connection, ${e.message}")
            }
            if (_natsConnection == null) {
                log.info("Waiting ${natsServerConfiguration.waitAfterConnectError} before trying to create NATS connection")
                delay(natsServerConfiguration.waitAfterConnectError)
                retries++
            }
        }
        return _natsConnection
    }

    /**
     * Writes message to the target.
     * @param targetData TargetData
     */
    override suspend fun writeTargetData(targetData: TargetData) {
        // accept data and send to worker for further processing
        targetDataChannel.submit(targetData, logger.getCtxLoggers(className, "writeTargetData"))
    }

    /**
     * Closes the writer
     */
    override suspend fun close() {
        try {
            targetContext.cancel()
            _natsConnection?.drain(java.time.Duration.of(10, ChronoUnit.SECONDS))
            _natsConnection?.close()
        } catch (e: Exception) {
            if (!e.isJobCancellationException) {
                logger.getCtxErrorLogEx(className, "close")("Error closing NATS writer", e)
            }
        }
    }


    private val targetResults = if (resultHandler != null) TargetResultBufferedHelper(targetID, resultHandler, logger) else null
    private val config: NatsWriterConfiguration by lazy { configReader.getConfig() }
    private val scope = buildScope("NATS Target")


    // channel to pass data to the coroutine that publishes the data to the subject
    private val targetDataChannel = TargetDataChannel.create(targetConfig, "$className:targetDataChannel")

    // Coroutine publishing the messages to the target subject
    private val writer = scope.launch(context = Dispatchers.IO, name = "Writer") {
        val log = logger.getCtxLoggers(className, "writer")


        try {
            log.info("NATS Writer for target \"$targetID\" writer publishing to subjects at endpoint ${targetConfig.natServerConfiguration.url} on target $targetID")
            while (isActive) {
                try {
                    select {

                        targetDataChannel.channel.onReceive { targetData ->

                            targetResults?.add(targetData)

                            val subjectMessages = mapTargetDataToSubjects(targetData)
                            if (containsPlaceHolders(targetConfig.subjectName)) {
                                log.trace("Message ${targetData.serial} mapped to subjects ${subjectMessages.keys}")
                            }

                            subjectMessages.forEach { (subject, subjectTargetData) ->

                                val subjectBuffer = buffers.computeIfAbsent(subject) { TargetDataBuffer(storeFullMessage = customPayload) }
                                val timer = timers.computeIfAbsent(subject) { createTimer(subject) }

                                val (messagePayload: String?, payloadSize: Int) = if (customPayload) {
                                    val formattedItemSize = try {
                                        (formatter?.itemPayloadSize(targetData) ?: altSize(targetData))
                                    } catch (e: Exception) {
                                        log.errorEx("Error getting payload size using custom formatter", e)
                                        altSize(targetData)
                                    }
                                    null to formattedItemSize
                                } else {
                                    val messagePayload = buildPayload(subjectTargetData)
                                    messagePayload to messagePayload.length
                                }

                                if (checkMessagePayloadSize(targetData, payloadSize, log)) {

                                    if (exceedBufferOrMaxPayloadWhenBufferingMessage(subjectBuffer, payloadSize)) {
                                        log.trace("Batch size of ${targetConfig.batchSize.byteCountString}${if (targetConfig.maxPayloadSize != null) " or ${targetConfig.maxPayloadSize!!.byteCountString}" else ""} for subject $subject reached")
                                        timers[subject] = writeBufferedMessages(subjectBuffer, subject, timer)
                                    }

                                    if (customPayload) {
                                        subjectBuffer.add(targetData, payloadSize)
                                    } else {
                                        subjectBuffer.add(targetData, messagePayload)
                                    }

                                    log.trace("Received message, buffered size for subject \"$subject\"  is ${subjectBuffer.payloadSize.byteCountString}")

                                    if (targetData.noBuffering || !doesBatching || bufferReachedMaxSizeOrMessages(subjectBuffer, subject, log)) {
                                        timers[subject] = writeBufferedMessages(subjectBuffer, subject, timer)
                                    }
                                }
                            }
                        }

                        timerChannel.onReceive { subject ->
                            val subjectBuffer = buffers[subject]
                            log.trace("${targetConfig.batchInterval} batch interval reached for subject $subject")
                            val timer = timers[subject]
                            if (subjectBuffer != null && timer != null)
                                timers[subject] = writeBufferedMessages(subjectBuffer, subject, timer)

                        }
                    }


                } catch (e: Exception) {
                    if (!e.isJobCancellationException)
                        log.errorEx("Error in writer", e)

                    timers.keys.forEach {
                        timers[it]?.cancel()
                        timers[it] = createTimer(it)
                    }
                }
            }

            buffers.forEach { (subject, buffer) ->
                writeBufferedMessages(buffer, subject, timers[subject]!!).cancel()
            }

        } catch (e: Exception) {
            logger.getCtxErrorLogEx(className, "targetWriter")("Error in target writer", e)
        }

    }

    private fun altSize(targetData: TargetData): Int = if (targetConfig.batchCount != 0) 0 else targetData.toJson(config.elementNames, targetConfig.unquoteNumericJsonValues).length


    private fun createTimer(channel: String): Job {
        return scope.launch {
            try {
                delay(targetConfig.batchInterval)
                timerChannel.send(channel)
            } catch (e: Exception) {
                // no harm done, timer is just used to guard for timeouts
            }
        }
    }

    private fun bufferReachedMaxSizeOrMessages(buffer: TargetDataBuffer, subject: String, log: Logger.ContextLogger): Boolean {
        val reachedBufferCount = if (targetConfig.batchCount > 0) (buffer.size >= targetConfig.batchCount) else false
        if (reachedBufferCount) log.trace("${targetConfig.batchCount} batch count reached")

        val reachedBufferSize = (targetConfig.batchSize > 0) && (buffer.payloadSize + (2 * (buffer.size - 1)) >= targetConfig.batchSize)

        if (reachedBufferSize) log.trace("${targetConfig.batchSize.byteCountString} batch size for subject $subject reached")

        return reachedBufferSize || reachedBufferCount
    }


    private fun checkMessagePayloadSize(targetData: TargetData, payloadSize: Int, log: Logger.ContextLogger): Boolean {
        if (usesCompression) return true
        return if (targetConfig.maxPayloadSize != null && payloadSize > targetConfig.maxPayloadSize!!) {
            log.error("Size $payloadSize bytes of message is larger max payload size  ${targetConfig.maxPayloadSize!!.byteCountString} for target")
            TargetResultHelper(targetID, resultHandler, logger).error(targetData)
            false
        } else true
    }

    private fun exceedBufferOrMaxPayloadWhenBufferingMessage(buffer: TargetDataBuffer, payloadSize: Int): Boolean {
        if (usesCompression) return false
        val bufferedPayloadSizeWhenAddingMessage = payloadSize + (2 + (buffer.size - 1)) + buffer.payloadSize
        val bufferSizeExceededWhenAddingMessage = (targetConfig.batchSize > 0) && (bufferedPayloadSizeWhenAddingMessage > targetConfig.batchSize)
        val maxPayloadSizeExceededWhenAddingMessage =
            targetConfig.maxPayloadSize != null && bufferedPayloadSizeWhenAddingMessage > targetConfig.maxPayloadSize!!
        val reachedMaxSizeWhenAddingToBuffer = (bufferSizeExceededWhenAddingMessage || maxPayloadSizeExceededWhenAddingMessage)
        return reachedMaxSizeWhenAddingToBuffer
    }


    private fun buildNatsMessage(buffer: TargetDataBuffer): ByteArray {
        return if (customPayload) {
            try {
                val binaryPayload = formatter!!.apply(buffer.messages)
                if (targetConfig.compressionType == CompressionType.NONE) {
                    binaryPayload
                } else
                    compressPayload(ByteArrayInputStream(binaryPayload), binaryPayload.size)
            } catch (e: Exception) {
                logger.getCtxErrorLog(className, "targetWriter")("Error executing custom formatter for target \"$targetID\", $e")
                ByteArray(0)
            }
        } else {

            val stringPayload = if (doesBatching)
                if (targetConfig.arrayWhenBuffered)
                    buffer.payloads.joinToString(prefix = "[", postfix = "]", separator = ",") { it }
                else
                    buffer.payloads.joinToString(separator = "") { it }
            else
                buffer.payloads.first()

            if (targetConfig.compressionType == CompressionType.NONE) {
                stringPayload.toByteArray()
            } else {
                compressPayload(stringPayload.byteInputStream(Charsets.UTF_8), stringPayload.length)
            }
        }
    }


    private fun compressPayload(content: ByteArrayInputStream, size: Int): ByteArray {
        val outputStream = ByteArrayOutputStream(2048)
        Compress.compress(targetConfig.compressionType, content, outputStream, entryName = "${UUID.randomUUID()}")
        val log = logger.getCtxLoggers(className, "compressContent")
        val compressedData = outputStream.toByteArray()
        log.info("Used ${targetConfig.compressionType} compression to compress ${size.byteCountString} to ${compressedData.size.byteCountString} bytes, ${(100 - (compressedData.size.toFloat() / size.toFloat()) * 100).toInt()}% size reduction")
        return compressedData
    }


    private suspend fun writeBufferedMessages(buffer: TargetDataBuffer, subject: String, timer: Job): Job {

        if (timer.isActive) timer.cancel()

        val log = logger.getCtxLoggers(className, "writeBufferedMessages")
        if (buffer.size == 0) {
            return createTimer(subject)
        }

        return try {

            val natsMessage = buildNatsMessage(buffer)

            when {
                (natsMessage.isEmpty()) -> {
                    log.trace("No payload to publish to topic $subject")
                    targetResults?.errorBuffered()
                    buffer.clear()
                    createTimer(subject)
                }

                (targetConfig.maxPayloadSize != null && natsMessage.size > targetConfig.maxPayloadSize!!) -> {
                    log.error("Size of NATS message ${natsMessage.size} bytes is beyond max payload size of  ${targetConfig.maxPayloadSize!!.byteCountString} for target, reduce or set $CONFIG_BATCH_SIZE, $CONFIG_BATCH_COUNT or $CONFIG_BATCH_INTERVAL for this target")
                    targetResults?.errorBuffered()
                    metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions)
                    createTimer(subject)
                }

                else -> {

                    val duration = measureTime {
                        val connection = getConnection()

                        withTimeout(targetConfig.publishTimeout) {
                            connection?.publish(subject, natsMessage)
                        }
                        targetResults?.ackBuffered()
                    }
                    val compressedStr = if (targetConfig.compressionType != CompressionType.NONE) " compressed " else " "
                    val itemStr = if (doesBatching) " containing ${buffer.size} items " else " "
                    log.trace("Published NATS ${compressedStr}message to subject \"$subject\" with size of ${natsMessage.size.byteCountString} ${itemStr}in $duration")

                    createMetrics(targetID, metricDimensions, buffer, natsMessage.size, duration)
                    createTimer(subject)
                }
            }


        } catch (e: Exception) {
            if (!e.isJobCancellationException) {
                metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions)
                log.error("Error publishing to subject \"$subject\" for target \"$targetID\", ${e.message}, $e")
                if (e is TimeoutCancellationException || _natsConnection == null) {
                    targetResults?.nackBuffered()
                } else {
                    targetResults?.errorBuffered()
                }
            }
            createTimer(subject)
        } finally {
            buffer.clear()
        }
    }


    private val metricsCollector: MetricsCollector? by lazy {
        val metricsConfiguration = config.targets[targetID]?.metrics ?: MetricsSourceConfiguration()
        if (config.isCollectingMetrics) {
            logger.metricsCollectorMethod = collectMetricsFromLogger
            MetricsCollector(
                metricsConfig = config.metrics,
                metricsSourceName = targetID,
                metricsSourceType = MetricsSourceType.TARGET_WRITER,
                metricsSourceConfiguration = metricsConfiguration,
                staticDimensions = TARGET_METRIC_DIMENSIONS,
                logger = logger
            )
        } else null
    }

    private val collectMetricsFromLogger: MetricsCollectorMethod? =
        if (config.isCollectingMetrics) {
            { metricsList ->
                try {
                    val dataPoints = metricsList.map { MetricsDataPoint(it.metricsName, metricDimensions, it.metricUnit, it.metricsValue) }
                    runBlocking {
                        metricsCollector?.put(targetID, dataPoints)
                    }
                } catch (e: java.lang.Exception) {
                    logger.getCtxErrorLogEx(this::class.java.simpleName, "collectMetricsFromLogger")("Error collecting metrics from logger", e)
                }
            }
        } else null

    override val metricsProvider: MetricsProvider? by lazy {
        if (metricsCollector != null) InProcessMetricsProvider(metricsCollector!!, logger) else null
    }

    private val transformation by lazy { if (targetConfig.template != null) OutputTransformation(targetConfig.template!!, logger) else null }

    private fun buildPayload(targetData: TargetData): String =
        if (transformation == null) targetData.toJson(config.elementNames, targetConfig.unquoteNumericJsonValues) else transformation!!.transform(
            targetData,
            config.elementNames,
            targetConfig.templateEpochTimestamp) ?: ""


    private fun createMetrics(
        adapterID: String,
        metricDimensions: MetricDimensions,
        buffer: TargetDataBuffer,
        payloadSize: Int,
        duration: Duration
    ) {

        metricsCollector?.put(
            adapterID,
            metricsCollector?.buildValueDataPoint(
                adapterID,
                MetricsCollector.METRICS_MEMORY,
                MemoryMonitor.getUsedMemoryMB().toDouble(),
                MetricUnits.MEGABYTES
            ),
            metricsCollector?.buildValueDataPoint(
                adapterID,
                MetricsCollector.METRICS_MEMORY,
                MemoryMonitor.getUsedMemoryMB().toDouble(),
                MetricUnits.MEGABYTES
            ),
            metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITES, 1.0, MetricUnits.COUNT, metricDimensions),
            metricsCollector?.buildValueDataPoint(adapterID, METRICS_BYTES_SEND, payloadSize.toDouble(), MetricUnits.COUNT, metricDimensions),
            metricsCollector?.buildValueDataPoint(adapterID, METRICS_MESSAGES, buffer.size.toDouble(), MetricUnits.COUNT, metricDimensions),
            metricsCollector?.buildValueDataPoint(
                adapterID,
                METRICS_WRITE_DURATION,
                duration.inWholeMilliseconds.toDouble(),
                MetricUnits.MILLISECONDS,
                metricDimensions
            ),
            metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions),
            metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SIZE, buffer.payloadSize.toDouble(), MetricUnits.BYTES, metricDimensions)
        )
    }


    private fun mapTargetDataToSubjects(targetData: TargetData): Map<String, TargetData> =
        targetData.splitDataByName(targetConfig.subjectName, ::buildSubjectName)


    private fun buildSubjectName(targetData: TargetData,
                                 sourceName: String,
                                 channel: String,
                                 channelMetadata: Map<String, String>): String {

        val log = logger.getCtxLoggers(className, "buildSubjectName")

        var subjectName = if (containsPlaceHolders(targetConfig.subjectName)) {
            TemplateRenderer.render(targetConfig.subjectName, targetData.schedule, sourceName, channel, targetID, channelMetadata)
        } else targetConfig.subjectName

        if (containsPlaceHolders(subjectName)) {
            val messageStr = "Source \"$sourceName\", channel \"${channel}\""
            if (targetConfig.alternateSubjectName != null) {
                log.trace("$messageStr has unmapped placeholder(s) ${getPlaceHolders(subjectName)} in subject \"$subjectName\", using $CONFIG_SUBJECT_NAME \"${targetConfig.subjectName}\", using alternative $CONFIG_ALTERNATE_SUBJECT_NAME \"${targetConfig.alternateSubjectName}\"")
                subjectName = TemplateRenderer.render(targetConfig.alternateSubjectName!!, targetData.schedule, sourceName, channel, targetID, channelMetadata)
                if (containsPlaceHolders(subjectName)) {
                    if (targetConfig.warnAlternateSubjectName || logger.level == LogLevel.TRACE) log.warning("$messageStr has unmapped placeholder(s) ${getPlaceHolders(subjectName)} in subject \"$subjectName\", using $CONFIG_ALTERNATE_SUBJECT_NAME \"${targetConfig.alternateSubjectName}\"")
                    subjectName = ""
                }
            } else {
                if (targetConfig.warnAlternateSubjectName || logger.level == LogLevel.TRACE) log.warning("$messageStr has unmapped placeholder(s) ${getPlaceHolders(subjectName)} in subject \"$subjectName\", using $CONFIG_SUBJECT_NAME \"${targetConfig.subjectName}\"")
                subjectName = ""
            }
        }
        return subjectName
    }

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any?) =
            newInstance(
                createParameters[0] as ConfigReader,
                createParameters[1] as String,
                createParameters[2] as Logger,
                createParameters[3] as TargetResultHandler?
            )

        @JvmStatic
        fun newInstance(configReader: ConfigReader, targetID: String, logger: Logger, resultHandler: TargetResultHandler?): TargetWriter {

            // Obtain configuration
            val config: NatsWriterConfiguration = readConfig(configReader)

            // Obtain configuration for used target
            val natsConfig = config.targets[targetID]
                    ?: throw TargetException("Configuration for $NATS_TARGET type target with ID \"$targetID\" does not exist, existing targets are ${config.targets.keys}")
            return try {
                NatsTargetWriter(
                    configReader = configReader,
                    targetID = targetID,
                    targetConfig = natsConfig,
                    logger = logger,
                    resultHandler = resultHandler
                )
            } catch (e: Throwable) {
                throw TargetException("Error creating $NATS_TARGET target for target \"$targetID\", $e")
            }
        }


        private fun readConfig(configReader: ConfigReader): NatsWriterConfiguration {
            return try {
                configReader.getConfig()
            } catch (e: JsonSyntaxException) {
                throw TargetException("Could not load NATS Target configuration, JSON syntax error, ${e.extendedJsonException(configReader.jsonConfig)}")
            } catch (e: Exception) {
                throw TargetException("Could not load NATS Target configuration: $e")
            }
        }

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
        )

    }

}

