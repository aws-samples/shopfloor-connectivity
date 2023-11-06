
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.s7

import com.amazonaws.sfc.data.ProtocolAdapterException
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_CONNECTIONS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_CONNECTION_ERRORS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_READ_DURATION
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_VALUES_READ
import com.amazonaws.sfc.s7.config.S7ControllerConfiguration
import com.amazonaws.sfc.s7.config.S7FieldChannelConfiguration
import com.amazonaws.sfc.s7.config.S7PlcControllerType
import com.amazonaws.sfc.util.LookupCacheHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import org.apache.plc4x.java.api.messages.PlcReadRequest
import org.apache.plc4x.java.api.messages.PlcReadResponse
import org.apache.plc4x.java.api.types.PlcResponseCode
import org.apache.plc4x.java.s7.readwrite.configuration.S7Configuration
import org.apache.plc4x.java.s7.readwrite.context.S7DriverContext
import org.apache.plc4x.java.s7.readwrite.field.S7Field
import org.apache.plc4x.java.s7.readwrite.field.S7StringField
import org.apache.plc4x.java.s7.readwrite.types.MemoryArea
import org.apache.plc4x.java.s7.readwrite.types.TransportSize
import org.apache.plc4x.java.spi.configuration.annotations.ConfigurationParameter
import org.apache.plc4x.java.spi.values.PlcBOOL
import java.math.BigInteger
import java.nio.charset.Charset
import java.time.Duration
import kotlin.time.measureTime

private typealias mapResultFunc = (String, PlcReadResponse) -> Any


class S7Controller(private val adapterID: String,
                   private val sourceID: String,
                   private val config: S7ControllerConfiguration,
                   private val logger: Logger) {

    private val className = this::class.java.simpleName

    private val Map<String, S7FieldChannelConfiguration>.id
        get() = this.toSortedMap().map { "${it.key}:${it.value.address}" }.joinToString(separator = ",")

    // Internal class to hold the configured S7 field and optionally the fields for segmented reads for the field
    // in situations where the max pdu size of the controller is not large enough to hold the data or when the
    // data is read in raw format to handle unsupported types in PLC4J
    data class S7FieldData(val s7Field: S7Field, val s7FallbackFields: List<S7Field>?) {
        fun fieldList(channelId: String) =
            if (s7FallbackFields.isNullOrEmpty())
                listOf(channelId to s7Field)
            else
                s7FallbackFields.mapIndexed { index, s7Field ->
                    "$channelId#${index.toString().padStart(3, '0')}" to s7Field
                }
    }


    // PLC4J returns incorrect size for ULINT
    private val TransportSize.fixedSizeInBytes
        get() = when (this) {
            TransportSize.ULINT -> TransportSize.LINT.sizeInBytes
            else -> this.sizeInBytes
        }

    // Cache for S7 fields, the index is an id for the configured field, the value could be a list of fields
    // if the field needed to be split in smaller size segments to deal with max size of pdu for the controller type
    // or a fields to readf the raw data if the type is not supported in PLC4J
    private val fieldCache = LookupCacheHandler<String, S7FieldData, String>(
        supplier = { address ->
            val s7Field = S7Field.of(address)
            val fallBackFields = buildFallbackS7Fields(s7Field)
            S7FieldData(s7Field, fallBackFields)
        }
    )

    // cache optimized sets of read requests indexed by the source and channels to reuse for repetitive reads
    private val readRequestCache = LookupCacheHandler<String, List<PlcReadRequest>?, Map<String, S7FieldChannelConfiguration>>(
        supplier = { null },
        initializer = { _, _, channels -> getReadRequests(channels ?: emptyMap()) }
    )

    // lock to prevent overlapping reads for the controller
    val lock by lazy { Mutex() }

    fun close() {
        // Do not explicitly close connection as the S7 driver implementation will terminate the S7 Protocol handling
        // and will not read any data after a new adapter is used after reloading a new configuration
    }

    // from a list of PlcReadResponses find the one containing the specified name
    private fun List<PlcReadResponse>.findResponseContainingField(name: String): PlcReadResponse? =
        this.firstOrNull {
            (it.getField(name) != null)
        }

    private val connectString by lazy {

        // get all configuration fields in order to get the name used in PlC4J for that field in the configuration string
        // in order to avoid hardcoded values which may change in future versions of PLC4J
        val params =
            S7Configuration::class.java.declaredFields.filter { it.annotations.any { a -> a is ConfigurationParameter } }
                .associate {
                    it.name to (it.annotations.first { a -> a is ConfigurationParameter } as ConfigurationParameter).value
                }

        // get the name for parameter and return the configuration string for the specified value
        val fnParamStr: (String, Any) -> String = { name, value ->
            val p = params[name]
            if (p != null) {
                "$p=$value"
            } else
                ""
        }

        "s7://${config.address}?" +
        "${fnParamStr(S7Configuration::localRack.name, config.localRack)}&" +
        "${fnParamStr(S7Configuration::localSlot.name, config.localSlot)}&" +
        "${fnParamStr(S7Configuration::remoteRack.name, config.remoteRack)}&" +
        "${fnParamStr(S7Configuration::remoteSlot.name, config.remoteSlot)}&" +
        "${fnParamStr(S7Configuration::pduSize.name, config.pduSize)}&" +
        "${fnParamStr(S7Configuration::maxAmqCaller.name, config.maxAmqCaller)}&" +
        fnParamStr(S7Configuration::maxAmqCallee.name, config.maxAmqCallee) +
        if (config.controllerType == S7PlcControllerType.UNKNOWN_CONTROLLER_TYPE) ""
        else "&controller-type=${config.controllerType}"
    }

    // Driver context which holds the actual parameters returned by the S7 controller
    private var _s7DriverContext: S7DriverContext? = null

    // When creating a request the PLC4J Optimizer and S7Driver context are created
    // This class uses a custom optimizer and S7 driver context in order to intercept
    // the driver context that holds the parameter values returned by the controller
    private val s7DriverContext by lazy {
        try {
            connection.readRequestBuilder().build().execute()
        } catch (_: Exception) {
        }
        _s7DriverContext
    }

    // Max data size for the PLC type
    private val plcMaxTpuBytes by lazy {
        s7DriverContext?.pduSize?.minus(18) ?: 222
    }

    // Gets a connection using an instance of a custom S7 driver that has a method to intercept the
    // actual parameters returned by the PLC
    private val connection by lazy {

           val s7 = CustomS7Driver {
                // Interceptor stores the driver context
                _s7DriverContext = it
            }

        // get the connection using the custom driver
        s7.getConnection(connectString)
    }

    // Gets all fields that need to be read for the channels, a channel can require multiple reads requests
    // to handle the limitations of the max pdu size of the PLC or data types not supported by PLC4J
    private fun getAllS7FieldsToRead(fieldChannels: Map<String, S7FieldChannelConfiguration>): MutableMap<String, S7Field> =
        fieldChannels.flatMap { (channelId, fieldAddress) ->
            val fieldData = runBlocking { fieldCache.getItemAsync(fieldAddress.address).await() }
            fieldData?.fieldList(channelId) ?: emptyList()
        }.toMap().toMutableMap()

    // Gets the required read requests for reading the channels. To deal with the max pdu size there may be
    // multiple reads required to read the data. The code also deals with issues in the PLC4J lib regarding
    // the calculation of a request size that includes string type fields.
    private fun getReadRequests(fieldChannels: Map<String, S7FieldChannelConfiguration>): List<PlcReadRequest> {
        val readPerSingleField = config.readSingleField
        return sequence {
            val allFieldsForReadingChannels = getAllS7FieldsToRead(fieldChannels)
            // Loop through the map, adding the fields to requests that are within the limits of the PLS\'s pdu size
            // When a field is added to a request it is removed from the map. The loop exists if the map is empty.
            while (allFieldsForReadingChannels.isNotEmpty()) {
                var dataSize = 0
                val rb = connection.readRequestBuilder()
                allFieldsForReadingChannels.entries.removeIf {
                    if (it.value.dataSize() > plcMaxTpuBytes) {
                        logger.getCtxWarningLog(className, "getReadRequests")("Size of S7 field ${it.key} is larger than max pdu size of $plcMaxTpuBytes bytes for PLC")
                        return@removeIf true
                    } else {
                        if ((it.value.dataSize() + dataSize) <= plcMaxTpuBytes) {
                            // when reading one at a time set total size to max size
                            dataSize += if (readPerSingleField) plcMaxTpuBytes else it.value.dataSize()
                            rb.addItem(it.key, it.value)
                            return@removeIf true
                        }

                        return@removeIf false
                    }
                }
                if (dataSize > 0) {
                    yield(rb.build())
                    dataSize = 0
                }
            }
        }.toList()
    }


    // Reads the values for the specified fields channels
    suspend fun read(fieldChannels: Map<String, S7FieldChannelConfiguration>,
                     metricDimensions: Map<String, String>,
                     metrics: MetricsCollector?): Map<String, Any?> = coroutineScope {
        // (re)connect
        connect(metricDimensions, metrics)
        var data: Map<String, Any?>
        // result contains one or more PlcRead responses
        val duration = measureTime {
            val results = executeReadRequests(fieldChannels)
            // Use results to obtain data from results build output as a map index by the channel names
            data = processResults(fieldChannels, results)
        }
        metrics?.put(adapterID,
            MetricsDataPoint(name = METRICS_VALUES_READ, dimensions = metricDimensions, units = MetricUnits.COUNT, MetricsValue(data.size)),
            MetricsDataPoint(name = METRICS_READ_DURATION, dimensions = metricDimensions, units = MetricUnits.MILLISECONDS, MetricsValue(duration.inWholeMilliseconds.toDouble()))
        )
        return@coroutineScope data
    }

    private suspend fun executeReadRequests(fieldChannels: Map<String, S7FieldChannelConfiguration>) = coroutineScope {
        // limit for max outstanding read requests
        // build or obtain required read requests
        val readRequests = readRequestCache.getItemAsync(fieldChannels.id, fieldChannels).await() ?: emptyList()
        // Parallel async read actions
        readRequests.map { request ->
            try {
                withTimeout(config.readTimeout) {
                    withContext(Dispatchers.IO) {
                        // Execute returns a deferred result, use blocking get to make sure the call has been completed
                        // before making a new one.
                        request.execute().get()
                    }
                }
            } catch (e: TimeoutCancellationException) {
                throw ProtocolAdapterException("Timeout reading from source \"$sourceID\"")
            } catch (e: Exception) {
                val errorFields = request.fields.map { lookupConfiguredField(it as S7Field) }.toSet()
                val msg = "Error reading from source  \"$sourceID\", fields [${errorFields.joinToString { it.toString() }}, $e"
                throw ProtocolAdapterException(msg)
            }
        }
    }


    private suspend fun processResults(channels: Map<String, S7FieldChannelConfiguration>,
                                       results: List<PlcReadResponse>): Map<String, Any?> {

        // map indexed by channel
        val rawReadData = channels.map { it.key to mutableMapOf<String, Any>() }.toMap()

        // foreach channel
        channels.forEach { (channelId, fieldAddress) ->
            val field = fieldCache.getItemAsync(fieldAddress.address).await()
            // get field or fields used to read the data
            field?.fieldList(channelId)?.forEach { (fieldName, s7Field) ->
                // get the PLC read result containing the field
                val result = results.findResponseContainingField(fieldName)
                if (result != null) {
                    // test if read was successful
                    val responseCode = result.getResponseCode(fieldName)
                    if (responseCode == PlcResponseCode.OK) {
                        // get the value for the field from the result and decode it according to type of the field
                        val readValue: Any? = decodeReadResultValue(fieldName, s7Field, result)
                        // Store data for the channel
                        if (readValue != null) rawReadData[channelId]!![fieldName] = readValue
                    } else {
                        val log = logger.getCtxErrorLog(className, "processResults")
                        log("Error field ${result.getField(fieldName)} for source \"$sourceID\", channel \"$channelId\",  ${responseCode.name}")
                    }
                }
            }
        }
        // Reconstruct data for fields that required multiple reads
        return constructReadOutput(channels, rawReadData)
    }


    private fun constructReadOutput(channels: Map<String, S7FieldChannelConfiguration>, rawData: Map<String, Map<String, Any?>?>): Map<String, Any?> {
        return rawData.map { (channelID, v) ->
            channelID to when {
                // no data for this channel
                v.isNullOrEmpty() -> null
                // single field was used to read the data for the channel, use returned the value
                (v.size == 1) -> v.values.first()
                else -> {
                    // multiple fields to read data segments for a channel
                    val s7Field = channels[channelID]?.field
                    when {
                        s7Field == null -> null
                        s7Field.isStringTypeField -> joinStringResultData(v, s7Field)
                        else -> joinArrayResultData(v, s7Field)
                    }
                }
            }
        }.toMap()
    }


    private suspend fun connect(metricDimensions: Map<String, String>, metrics: MetricsCollector?) {
        if (connection.isConnected) return

        try {
            withTimeout(config.connectTimeout) {
                connection.connect()
                metrics?.put(adapterID, METRICS_CONNECTIONS, 1.0, MetricUnits.COUNT, metricDimensions)
            }
            if (!connection.metadata?.canRead()!!) {
                throw ProtocolAdapterException("Can connect but not read from PLC for source \"${sourceID}\" with connect string \"$connectString\"")
            }
        } catch (e: Exception) {
            metrics?.put(adapterID, METRICS_CONNECTION_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions)
            throw (ProtocolAdapterException("Can not connect to PLC for source \"${sourceID}\" with connect string \"$connectString\", ${e.message}"))

        }
    }

    // Builds required sets of fields to read data dealing with max pdu size for PLC or datatype
    // not handled by PLC4J. Returns null if configured field can be used
    private fun buildFallbackS7Fields(s7: S7Field): List<S7Field>? =

        when (s7.dataType) {
            TransportSize.LREAL -> listOf(asRawBytesField(s7))
            TransportSize.STRING -> stringAsRawFields(s7)
            TransportSize.WSTRING -> stringAsRawFields(s7)
            TransportSize.WCHAR -> listOf(asRawBytesField(s7))
            TransportSize.LWORD -> listOf(asRawBytesField(s7))
            TransportSize.LINT, TransportSize.ULINT -> listOf(asRawBytesField(s7))
            else -> {
                val fitsInSinglePduRequest = (s7.numberOfElements == 1) || (s7.numberOfElements * s7.dataType.fixedSizeInBytes <= plcMaxTpuBytes)
                if (fitsInSinglePduRequest) null else buildChunkedArrayFields(s7)
            }
        }

    private fun buildChunkedArrayFields(s7Field: S7Field): List<S7Field> {
        val maxItemsInTpu: Int = (plcMaxTpuBytes / s7Field.dataType.fixedSizeInBytes)

        if (maxItemsInTpu == 0) {
            throw ProtocolAdapterException("Items of type ${s7Field.dataType.shortName} is larger than PLC max TPU size")
        }

        val chunkStartAndNumberOfElements =
            IntRange(start = 0, endInclusive = s7Field.numberOfElements - 1).chunked(maxItemsInTpu) { it.first() to (it.last() - it.first()) + 1 }

        val chunks = chunkStartAndNumberOfElements.map { c ->
            val offset = s7Field.byteOffset + c.first * s7Field.dataType.fixedSizeInBytes
            val ff = "%${s7Field.memoryArea.shortName}" +
                     "${if (s7Field.memoryArea == MemoryArea.DATA_BLOCKS) s7Field.blockNumber else ""}:" +
                     "$offset:${s7Field.dataType}[${c.second}]"
            S7Field.of(ff)
        }
        return chunks

    }

    // Read data as raw bytes for types not or incorrectly supported in PLC4J
    private fun asRawBytesField(s7Field: S7Field): S7Field {
        val size = s7Field.numberOfElements * s7Field.dataType.fixedSizeInBytes
        val ff =
            "%${s7Field.memoryArea.shortName}" +
            "${if (s7Field.memoryArea == MemoryArea.DATA_BLOCKS) s7Field.blockNumber else ""}:" +
            "${s7Field.byteOffset}:SINT[${minOf(size, plcMaxTpuBytes)}]"
        return S7Field.of(ff)
    }

    // Builds fields for STRING and WSTRING (array) fields as raw string data dealing with
    // max pdu size of the PLC
    private fun stringAsRawFields(s7Field: S7Field): List<S7Field> {

        return (0 until s7Field.numberOfElements).flatMap { elementIndex ->

            val isUnicode = s7Field.dataType == TransportSize.WSTRING
            val bytesToSkip = if (isUnicode) 4 else 2

            val s7StringField = s7Field as? S7StringField? // null is no length is specified
            val lengthOfString = s7StringField?.stringLength ?: DEFAULT_STRING_LENGTH
            val stringBytesLength = if (isUnicode) lengthOfString * 2 else lengthOfString

            val stringSections = splitMemoryRangeInToSegments(
                s7Field.byteOffset + (elementIndex * (stringBytesLength + bytesToSkip)) + bytesToSkip,
                stringBytesLength
            )

            stringSections.map { (sectionOffset, sectionLength) ->
                val rawDataFieldStr = "%${s7Field.memoryArea.shortName}" +
                                      "${if (s7Field.memoryArea == MemoryArea.DATA_BLOCKS) s7Field.blockNumber else ""}:" +
                                      "$sectionOffset:USINT[${sectionLength}]"
                S7Field.of(rawDataFieldStr)
            }

        }
    }


    // returns data bytes for a field
    private fun S7Field.dataSize(): Int =

        when (this.dataType) {

            TransportSize.STRING -> {
                val s = this as? S7StringField?
                s?.stringLength ?: DEFAULT_STRING_LENGTH
            }

            TransportSize.WSTRING -> {
                val s = this as? S7StringField?
                (s?.stringLength?.times(2)) ?: DEFAULT_STRING_LENGTH
            }

            else ->
                this.dataType.fixedSizeInBytes * this.numberOfElements
        }


    // Returns the segments (start and length) for a reading a block of bytes taking into account
    // the max pdu size of the PLC
    private fun splitMemoryRangeInToSegments(offset: Int, len: Int): List<Pair<Int, Int>> {
        val maxLength = plcMaxTpuBytes
        var l = len
        var o = offset
        val ll = sequence {
            while (l > maxLength) {
                yield(o to maxLength)
                l -= maxLength
                o += maxLength
            }
            yield(o to l)
        }.toList()
        return ll

    }

    // Decodes data read from PLC for types that need special handling
    private fun decodeReadResultValue(fieldName: String, s7Field: S7Field, result: PlcReadResponse): Any? {

        val f: S7Field? = lookupConfiguredField(s7Field)

        val o: Any? = when (f?.dataType) {

            // Byte is an array of 8 booleans
            TransportSize.BYTE, TransportSize.WORD, TransportSize.DWORD ->
                mapResultValue(fieldName, result) { fld, resp ->
                    resp.getAllObjects(fld).map { b ->
                        when (b) {
                            is Iterable<*> -> {
                                (b).map { bb ->
                                    if (bb is PlcBOOL) bb.boolean else b
                                }
                            }

                            is PlcBOOL -> b.boolean
                            else -> b
                        }

                    }
                }

            // Unsigned Byte, 8 bits
            TransportSize.USINT ->
                mapResultValue(fieldName, result) { fld, resp ->
                    resp.getAllBytes(fld).map { s -> s.toUByte() }
                }

            // Unsigned short, 16 bits
            TransportSize.UINT ->
                mapResultValue(fieldName, result) { fld, resp ->
                    resp.getAllShorts(fld).map { s -> s.toUShort() }
                }

            // Unsigned Int, 32 bits
            TransportSize.UDINT ->
                mapResultValue(fieldName, result) { fld, resp ->
                    resp.getAllIntegers(fld).map { s -> s.toUInt() }
                }

            // Unsigned Long, 64 bits
            TransportSize.LINT ->
                mapResultValue(fieldName, result) { fld, resp ->
                    val bytes = resp.getAllBytes(fld).toByteArray()
                    bytesToLong(bytes)
                }

            // Unsigned Long, 64 bits
            TransportSize.ULINT ->
                mapResultValue(fieldName, result) { fld, resp ->
                    val bytes = resp.getAllBytes(fld).toByteArray()
                    bytesToLong(bytes).map { it.toULong() }
                }

            // Double
            TransportSize.LREAL ->
                mapResultValue(fieldName, result) { fld, resp ->
                    val bytes = resp.getAllBytes(fld).toByteArray()
                    bytesToLReal(bytes)
                }

            // Double
            TransportSize.LWORD ->
                mapResultValue(fieldName, result) { fld, resp ->
                    val bytes = resp.getAllBytes(fld).toByteArray()
                    bytesToLWord(bytes)
                }

            // String
            TransportSize.STRING -> {
                mapResultValue(fieldName, result) { fld, resp ->
                    val bytes = resp.getAllBytes(fld).toByteArray()
                    bytesToString(bytes, Charsets.UTF_8)
                }

            }

            // WString as UTF_16 unicode string
            TransportSize.WSTRING ->
                mapResultValue(fieldName, result) { fld, resp ->
                    val bytes = resp.getAllBytes(fld).toByteArray()
                    bytesToString(bytes, Charsets.UTF_16)
                }

            // Char as a single char string
            TransportSize.CHAR ->
                mapResultValue(fieldName, result) { fld, resp ->
                    resp.getAllStrings(fld).map { s ->
                        s.first()
                    }
                }

            // WChar as a single character unicode string
            TransportSize.WCHAR ->
                mapResultValue(fieldName, result) { fld, resp ->
                    val bytes = resp.getAllBytes(fld).toByteArray()
                    bytes.asList().chunked(2).map {
                        val s = bytesToString(it.toByteArray(), Charsets.UTF_16)
                        s.substring(0, minOf(1, s.length))
                    }
                }


            // Time as Duration string in ISO8601 format
            TransportSize.TIME ->
                mapResultValue(fieldName, result) { fld, resp ->
                    resp.getAllObjects(fld).map { t ->
                        (t as Duration).toString()
                    }
                }

            // LTime as Duration string in ISO8601 format
            TransportSize.LTIME ->
                mapResultValue(fieldName, result) { fld, resp ->
                    resp.getAllObjects(fld).map { t ->
                        (t as Duration).toString()
                    }
                }

            // Date as ISO formatted string, yyyy-mm-dd
            TransportSize.DATE ->
                mapResultValue(fieldName, result) { fld, resp ->
                    resp.getAllObjects(fld).map { d ->
                        d.toString()
                    }
                }

            // Time of day as ISO formatted string, hh:mm:ss
            TransportSize.TIME_OF_DAY ->
                mapResultValue(fieldName, result) { fld, resp ->
                    resp.getAllObjects(fld).map { t ->
                        t.toString()
                    }
                }

            // Date and time as ISO formatted string
            TransportSize.DATE_AND_TIME ->
                mapResultValue(fieldName, result) { fld, resp ->
                    resp.getAllObjects(fld).map { t ->
                        t.toString()
                    }
                }


            // Data types that don't need special handling
            else ->
                mapResultValue(fieldName, result) { fld, resp ->
                    resp.getAllObjects(fld)
                }
        }
        return o
    }

    private fun lookupConfiguredField(s7Field: S7Field): S7Field? {
        var f: S7Field? = null
        fieldCache.keys.takeWhile { f == null }.forEach { name ->
            runBlocking {
                val fd = fieldCache.getItemAsync(name).await()!!
                if ((fd.s7Field == s7Field) || ((fd.s7FallbackFields ?: emptyList()).contains(s7Field))) {
                    f = fd.s7Field
                }
            }
        }
        return f
    }

    // Combines segments of (array of) STRING and WSTRING values
    private fun joinStringResultData(fieldStringData: Map<String, Any?>, fld: S7Field): Any {

        val data = fieldStringData.keys.asSequence()
            .sorted().map { fieldStringData[it] }.filterNotNull() // Values sorted by segmented field names
            .chunked(fieldStringData.size / fld.numberOfElements) // chunked by number of fields used to get the original field (could be an array)
            .map { chunk ->
                if (chunk.size > 1) chunk.joinToString(separator = "") { it.toString() } else chunk.first()
            }.toList()
        return if (data.size == 1) data.first() else data
    }


    private fun joinArrayResultData(fieldData: Map<String, Any?>, fld: S7Field): Any {

        val data = fieldData.keys
            .sorted().mapNotNull { fieldData[it] }
            .flatMap { it as Iterable<*> }

        return when (data.size) {
            1 -> data.first()!!
            fld.numberOfElements -> data
            else -> {
                data.chunked(data.size / fld.numberOfElements)
            }
        }

    }

    companion object {

        const val DEFAULT_STRING_LENGTH = 254

        // Helper to return data either as single valle of an array
        private fun mapResultValue(
            channelID: String, response: PlcReadResponse, block: mapResultFunc
        ): Any? {
            val data = block(channelID, response)
            return if (data is Iterable<*>) {
                if ((data as List<*>).size == 1) data.first() else data
            } else {
                data
            }
        }

        // Builds a STRING or unicode WSTRING value from raw bytes
        private fun bytesToString(bytes: ByteArray, charset: Charset): String {
            val str = String(bytes, charset)
            val n = str.indexOf(Char(0))
            return if (n == -1) str else str.substring(0, n)
        }

        // Builds (array of) LREAL from raw bytes
        private fun bytesToLReal(bytes: ByteArray): Any =
            // build bit string for each value using 8 bytes for each
            bytes.asList().chunked(8).map { c ->
                val sb = StringBuffer()
                c.forEach { i ->
                    var binaryString = Integer.toBinaryString(i.toInt())
                    val len = binaryString.length
                    binaryString =
                        when {
                            // bit string for each byte must be 8 chars
                            len < 8 -> binaryString.padStart(8, '0')
                            len > 8 -> binaryString.substring(binaryString.length - 8)
                            else -> binaryString
                        }
                    sb.append(binaryString)
                }
                // convert bit string of 64 bite to double value
                val asLong = sb.toString().toLong(2)
                Double.fromBits(asLong)
            }

        // Build (array) of LWORD from raw bytes
        private fun bytesToLWord(bytes: ByteArray): Any {
            val booleanArrays = bytes.asList().chunked(8).map { b ->
                b.map { byte ->
                    var mask = 0x80
                    sequence {
                        while (mask != 0) {
                            yield((byte.toInt() and mask) != 0)
                            mask = mask shr 1
                        }
                    }.toList()
                }.flatten()
            }
            return if (booleanArrays.size == 1) booleanArrays.first() else booleanArrays
        }

        // Build (array) of LINT from raw bytes
        private fun bytesToLong(bytes: ByteArray) =
            bytes.asList().chunked(8).map { b ->
                BigInteger(b.toByteArray()).toLong()
            }

        private val S7Field.isStringTypeField: Boolean
            get() = this.dataType == TransportSize.WSTRING || this.dataType == TransportSize.STRING

    }

}

