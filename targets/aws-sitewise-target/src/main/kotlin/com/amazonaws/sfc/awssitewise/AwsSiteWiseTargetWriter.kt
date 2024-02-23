// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awssitewise


import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseAssetConfiguration
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseTargetConfiguration
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseWriterConfiguration
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseWriterConfiguration.Companion.AWS_SITEWISE
import com.amazonaws.sfc.awssitewise.config.SiteWiseAssetPropertyConfiguration
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.DataTypes.isNumeric
import com.amazonaws.sfc.data.TargetData
import com.amazonaws.sfc.data.TargetResultBufferedHelper
import com.amazonaws.sfc.data.TargetResultHandler
import com.amazonaws.sfc.data.TargetWriter
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_MESSAGES
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITES
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_DURATION
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_ERRORS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_SIZE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_SUCCESS
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.targets.AwsServiceTargetClientHelper
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.canNotReachAwsService
import com.amazonaws.sfc.util.launch
import io.burt.jmespath.Expression
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.services.iotsitewise.IoTSiteWiseClient
import software.amazon.awssdk.services.iotsitewise.model.*
import java.time.Instant
import java.util.*


/**
 * AWS Sitewise Target writer
 * @property targetID String ID of target
 * @property configReader configReader for reading target configuration
 * @property logger Logger Logger for output
 * @see TargetWriter
 */
class AwsSiteWiseTargetWriter(
    private val targetID: String,
    private val configReader: ConfigReader,
    private val logger: Logger,
    resultHandler: TargetResultHandler?
) : TargetWriter {

    private val className = this::class.java.simpleName

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }


    private val metricDimensions = mapOf(
        METRICS_DIMENSION_SOURCE to targetID,
        MetricsCollector.METRICS_DIMENSION_TYPE to className
    )

    private val scope = buildScope("Sitewise Target")

    private val clientHelper =
        AwsServiceTargetClientHelper(
            configReader.getConfig<AwsSiteWiseWriterConfiguration>(),
            targetID,
            IoTSiteWiseClient.builder(),
            logger
        )

    private val targetResults = if (resultHandler != null) TargetResultBufferedHelper(targetID, resultHandler, logger) else null
    private val sitewiseClient: AwsSiteWiseClient
        get() = AwsSiteWiseClientWrapper(clientHelper.serviceClient as IoTSiteWiseClient)

    // Configured field names
    private val elementNames = config.elementNames

    // channel for passing messages to coroutine that sends messages to SiteWise queue
    private val targetDataChannel = Channel<TargetData>(100)

    // Buffer to collect batch of property values
    private val propertyValuesBuffer = mutableMapOf<String, MutableMap<SiteWiseAssetPropertyConfiguration, MutableList<AssetPropertyValue>>>()
    private var batchCount = 0

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
                    logger.getCtxErrorLog(this::class.java.simpleName, "collectMetricsFromLogger")("Error collecting metrics from logger, $e")
                }
            }
        } else null

    override val metricsProvider: MetricsProvider? by lazy {
        if (metricsCollector != null) InProcessMetricsProvider(metricsCollector!!, logger) else null
    }


    // coroutine that writes messages to queue
    private val writer = scope.launch("Writer") {

        val log = logger.getCtxLoggers(AwsSiteWiseTargetWriter::class.java.simpleName, "writer")
        log.info("AWS SiteWise writer for target \"$targetID\" sending to ${targetConfig.assets.size} assets in region ${targetConfig.region}")

        var timer = timerJob()

        while (isActive) {
            try {
                select {

                    targetDataChannel.onReceive { targetData ->
                        timer.cancel()
                        handleTargetData(targetData)
                    }

                    timer.onJoin {
                        log.trace("${(targetConfig.interval.inWholeMilliseconds)} milliseconds buffer interval reached, flushing buffer")
                        flush()
                    }
                }

                timer = timerJob()
            }catch (e: CancellationException) {
                log.info("Writer stopped")
            }catch (e : Exception){
                log.error("Error in writer, $e")
            }
        }
    }

    private fun handleTargetData(targetData: TargetData) {

        val log = logger.getCtxLoggers(className, "handleTargetData")

        if (logger.level == LogLevel.TRACE) {
            val json = targetData.toJson(elementNames)
            log.trace("Writer received data \"$json\"")
        }

        targetResults?.add(targetData)
        // Remapping keys of the data to escape unsupported characters for jmespath queries
        val data = targetData.toMap(config.elementNames, jmesPathCompatibleKeys = true)
        // For every configured asset
        targetConfig.assets.forEach { asset ->
            // For every configures property of the asset
            asset.properties.forEach { property ->
                // Extract the value and timestamp from the data
                val valueAndTimeStamp = getPropertyValueAndTimeStamp(data, asset, property, log)
                if (valueAndTimeStamp != null) {
                    // Create and buffer asset property value
                    try {
                        val assetValue = buildAssetValue(property.dataType, valueAndTimeStamp.first, valueAndTimeStamp.second)
                        storeValueAndTimestampInBuffer(asset.assetID, property, assetValue)
                    } catch (e: Exception) {
                        log.error("Error building property value for property $property from value ${valueAndTimeStamp.first} (${valueAndTimeStamp.first::class.java.simpleName}), ${e.message}")
                    }
                }
            }
        }
        // Keeps track of reads in batch, flush if configured value is reached
        batchCount += 1
        if (targetData.noBuffering || batchCount >= targetConfig.batchSize) {
            flush()
            batchCount = 0
        }
    }


    private fun CoroutineScope.timerJob(): Job {
        return launch("Timeout timer") {

            return@launch try {
                delay(targetConfig.interval)
            } catch (e: Exception) {
                // no harm done, timer is just used to guard for timeouts
            }
        }
    }


    /**
     * Writes message to SiteWise target
     * @param targetData TargetData
     */
    override suspend fun writeTargetData(targetData: TargetData) {
        targetDataChannel.send(targetData)
    }

    /**
     * Closes the writer
     */
    override suspend fun close() {
        flush()
        writer.cancel()
        sitewiseClient.close()
    }

    /**
     * Builds asset property value
     * @param dataType SiteWiseDataType type of the data
     * @param value Any The data value
     * @param timestamp Instant The timestamp
     * @return AssetPropertyValue Created AssetPropertyValue instance
     */
    private fun buildAssetValue(dataType: SiteWiseDataType, value: Any, timestamp: Instant): AssetPropertyValue {

        // Use configured datatype, if unspecified determine type of data from the value
        val usedDataType = if (dataType != SiteWiseDataType.UNSPECIFIED) dataType else siteWiseDataType(value)

        // Build the AssetPropertyValue
        return AssetPropertyValue.builder()
            .timestamp(
                TimeInNanos.builder()
                    .timeInSeconds(timestamp.epochSecond)
                    .offsetInNanos(timestamp.nano)
                    .build()
            )
            .value(buildValue(usedDataType, value))
            .build()
    }

    /**
     * Stores a AssetPropertyValue in the buffer
     * @param assetID String Asset ID
     * @param property SiteWiseAssetPropertyConfiguration Asset property configuration
     * @param propValue AssetPropertyValue The AssetPropertyValue to store
     */
    private fun storeValueAndTimestampInBuffer(
        assetID: String, property: SiteWiseAssetPropertyConfiguration, propValue: AssetPropertyValue
    ) {
        // Get entry for asset
        var assetEntry = propertyValuesBuffer[assetID]
        if (assetEntry == null) {
            propertyValuesBuffer[assetID] = mutableMapOf()
            assetEntry = propertyValuesBuffer[assetID]
        }

        // Get entry in asset for property
        var propertyEntry = assetEntry!![property]
        if (propertyEntry == null) {
            assetEntry[property] = mutableListOf()
            propertyEntry = assetEntry[property]
        }

        // Add to list of stored values
        propertyEntry!!.add(propValue)
    }


    /**
     * Extracts property value and timestamp from the received data
     * @param data Mapping<String, Mapping<String, Any?>> Data received by target writer
     * @param asset AwsSitewiseAssetConfig Configuration of the asset
     * @param prop SiteWiseAssetPropertyConfiguration Configuration of the property
     * @param log ContextLogger Logger for output
     * @return Pair<Any, Instant>? Pair containing value and timestamp if found, else null
     */
    private fun getPropertyValueAndTimeStamp(
        data: Map<String, Any>,
        asset: AwsSiteWiseAssetConfiguration,
        prop: SiteWiseAssetPropertyConfiguration,
        log: Logger.ContextLogger
    ): Pair<Any, Instant>? {

        // internal method for searching value and timestamp for an asset property
        fun searchData(query: Expression<Any>?): Any? =
            try {
                query?.search(data)
            } catch (e: NullPointerException) {
                null
            } catch (e: Exception) {
                log.error("Error querying data for target \"$targetID\", asset ${asset.assetID}, property $prop")
                null
            }

        // internal method for fetching a specified value from input value this is a map
        fun valueFromMap(v: Any?, key: String): Any? = if ((v is Map<*, *>) && (v.containsKey(key))) v[key] else null

        // Search property data
        val dataSearchResult = searchData(prop.dataPath)
        // If the value is a map the value can be in the value field
        val propertyValue: Any? = (valueFromMap(dataSearchResult, elementNames.value)) ?: dataSearchResult

        // No data for this property
        if (propertyValue == null) {
            log.warning("No value found for target \"$targetID\", asset \"${asset.assetID}\", property \"$prop\" for dataPath \"${prop.dataPathStr}\"")
            return null
        }

        log.trace("Value $propertyValue (${propertyValue::class.java.simpleName}) found for target \"$targetID\" , asset \"${asset.assetID}\", property \"$prop\" using path ${prop.dataPathStr}")

        val timestampPath = prop.timestampPath
        // Test if there is an explicit timestamp query, in that case use it to query for the timestamp
        val timestampValue: Instant? = if (timestampPath != null) {
            val timestampSearchResult = searchData(timestampPath) as? Instant?
            (valueFromMap(timestampSearchResult, elementNames.timestamp) as? Instant?) ?: timestampSearchResult
        } else {
            // else test if it was included as a timestamp field with the value
            (valueFromMap(dataSearchResult, elementNames.timestamp) as? Instant?)
        }
        if (timestampValue == null) {
            log.trace("No timestamp found for target \"$targetID\", asset ${asset.assetID}, property $prop\" for timestampPath \"${prop.timestampPathStr}\"")
            return null
        }

        log.trace("Timestamp $timestampValue found for target \"$targetID\", asset \"${asset.assetID}\", property \"$prop\"")

        return propertyValue to timestampValue

    }


    /**
     * Writes all buffered values to SiteWise in optimized bach calls
     */
    private fun flush() {

        val log = logger.getCtxLoggers(className, "flush")

        // No buffered messages, nothing to do
        if (propertyValuesBuffer.isEmpty()) {
            return
        }


        // Stream of requests containing max 10 entries with max 10 values per asset property
        buildBatchRequests().forEach { request ->
            try {

                val start = DateTime.systemDateTime().toEpochMilli()
                val resp = clientHelper.executeServiceCallWithRetries {
                    try {
                        log.info(
                            "Writing batch of ${request.entries().sumOf { it.propertyValues().count() }} values to ${
                                request.entries().count()
                            } properties for ${request.entries().groupBy { it.assetId() }.count()} assets(s)"
                        )
                        val r = sitewiseClient.batchPutAssetPropertyValue(request)
                        val writeDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()
                        createMetrics(targetID, metricDimensions, request, writeDurationInMillis)

                        r

                    } catch (e: AwsServiceException) {
                        log.error("SiteWise batchPutAssetPropertyValue error ${e.message}")
                        runBlocking { metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions) }

                        // Check the exception, it will throw an AwsServiceRetryableException if the error is recoverable
                        clientHelper.processServiceException(e)

                        // Non recoverable service exceptions
                        throw e
                    }
                }

                log.trace("BatchPutAssetPropertyValue result is ${resp.sdkHttpResponse()?.statusCode()}")
                targetResults?.ackBuffered()

                resp.errorEntries().forEach { errorEntry ->
                    logErrorEntry(request, errorEntry, log.error)

                }
            } catch (e: Exception) {
                log.error("Error sending to SiteWise \"$targetID\", ${e.message}")
                if (canNotReachAwsService(e)) {
                    targetResults?.nackBuffered()
                } else {
                    targetResults?.errorBuffered()
                }
            }
        }
        propertyValuesBuffer.clear()
    }

    private fun createMetrics(
        adapterID: String,
        metricDimensions: MetricDimensions,
        request: BatchPutAssetPropertyValueRequest,
        writeDurationInMillis: Double
    ) {

        runBlocking {
            metricsCollector?.put(
                adapterID,
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITES, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_MESSAGES, request.entries().size.toDouble(), MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_DURATION, writeDurationInMillis, MetricUnits.MILLISECONDS, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SIZE, request.toString().length.toDouble(), MetricUnits.BYTES, metricDimensions)
            )
        }
    }

    /**
     * Logs BatchPutAssetPropertyValueRequest entry with error
     * @param request BatchPutAssetPropertyValueRequest The request of the failed entry
     * @param errorEntry BatchPutAssetPropertyErrorEntry The error entry
     * @param error Function1<String, Unit> Writes to error log
     */
    private fun logErrorEntry(request: BatchPutAssetPropertyValueRequest, errorEntry: BatchPutAssetPropertyErrorEntry, error: (String) -> Unit) {
        val entry = request.entries().find { it.entryId() == errorEntry.entryId() }.toString()
        val errors = errorEntry.errors().joinToString(separator = ", ") { "${it.errorMessage()}, ${it.errorCodeAsString()}" }
        error("$entry: $errors")
    }

    /**
     * Builds an optimized stream of BatchPutAssetPropertyValueRequest requests from buffered values. Each request has max 10 entries,
     * each entry has max 10 values
     * @return Sequence<BatchPutAssetPropertyValueRequest>
     */
    private fun buildBatchRequests() = sequence {
        // Sequence of PutAssetPropertyValueEntries
        sequence {
            propertyValuesBuffer.forEach { (assetID, properties) ->
                properties.forEach { (prop, propertyValues) ->
                    propertyValues.chunked(10) // Max 10 values per entry
                        .map { values ->

                            // Build the entry
                            val builder = PutAssetPropertyValueEntry.builder()
                                .assetId(assetID)
                                .entryId(UUID.randomUUID().toString())
                                .propertyValues(values)

                            // Use either property ID or alias
                            if (!prop.propertyID.isNullOrEmpty()) {
                                builder.propertyId(prop.propertyID)
                            } else if (!prop.propertyAlias.isNullOrEmpty()) {
                                builder.propertyAlias(prop.propertyAlias)
                            }

                            yield(builder.build())
                        }
                }
            }
        }.chunked(10)
            .forEach { entries ->   // Max 10 entries per request
                yield(BatchPutAssetPropertyValueRequest.builder().entries(entries).build())
            }
    }

    /**
     * Builds SiteWise Variant for value
     * @param dataType SiteWiseDataType The datatype
     * @param data Any The value
     * @return Variant
     */
    private fun buildValue(dataType: SiteWiseDataType, data: Any): Variant {
        val variantBuilder = Variant.builder()
        when (dataType) {
            SiteWiseDataType.DOUBLE -> variantBuilder.doubleValue(toSiteWiseDouble(data))
            SiteWiseDataType.INTEGER -> variantBuilder.integerValue(toSiteWiseInt(data))
            SiteWiseDataType.STRING -> variantBuilder.stringValue(data.toString())
            SiteWiseDataType.BOOLEAN -> variantBuilder.booleanValue(toSiteWiseBoolean(data))
            else -> variantBuilder.stringValue(data.toString())
        }
        return variantBuilder.build()
    }

    /**
     * Gets the config for SiteWise Target writer
     */
    private val config: AwsSiteWiseWriterConfiguration
        get() {
            return clientHelper.writerConfig(configReader, AWS_SITEWISE)
        }

    /**
     * Gets the config for the siteWise Target
     */

    private var _targetConfig: AwsSiteWiseTargetConfiguration? = null
    private val targetConfig: AwsSiteWiseTargetConfiguration
        get() {
            if (_targetConfig == null) {
                _targetConfig = clientHelper.targetConfig(config, targetID, AWS_SITEWISE)
            }
            return _targetConfig as AwsSiteWiseTargetConfiguration
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

        /**
         * Creates new instance of AWS SiteWise target from configuration.
         * @param configReader ConfigReader Reader for reading configuration for target instance
         * @see AwsSiteWiseWriterConfiguration
         * @param targetID String ID of the target
         * @param logger Logger Logger for output
         * @return TargetWriter Created target
         * @throws Exception
         */
        @JvmStatic
        fun newInstance(configReader: ConfigReader, targetID: String, logger: Logger, resultHandler: TargetResultHandler?): TargetWriter {
            return try {
                AwsSiteWiseTargetWriter(targetID, configReader, logger, resultHandler)
            } catch (e: Throwable) {
                throw TargetException("Error creating AWS SiteWise target writer, ${e.message}")
            }
        }

        /**
         * Determined the SiteWise DataType from a value
         * @param value Any The data value
         * @return SiteWiseDataType
         */
        private fun siteWiseDataType(value: Any): SiteWiseDataType =
            when (value) {
                is String -> SiteWiseDataType.STRING
                is Boolean -> SiteWiseDataType.BOOLEAN
                is Byte -> SiteWiseDataType.INTEGER
                is Short -> SiteWiseDataType.INTEGER
                is Int -> SiteWiseDataType.INTEGER
                is Long -> SiteWiseDataType.INTEGER
                is UByte -> SiteWiseDataType.INTEGER
                is UShort -> SiteWiseDataType.INTEGER
                is UInt -> SiteWiseDataType.INTEGER
                is ULong -> SiteWiseDataType.INTEGER
                is Double -> SiteWiseDataType.DOUBLE
                is Float -> SiteWiseDataType.DOUBLE
                else -> SiteWiseDataType.STRING
            }


        /**
         * Converts a value to boolean
         * @param data Any Data value
         * @return Boolean
         */
        private fun toSiteWiseBoolean(data: Any) = when (data) {
            is String -> when (data.lowercase()) {
                "true", "1", "1.0" -> true
                "false", "0", "0.0" -> false
                else -> throw TargetException("Can not convert string $data to boolean")
            }

            is Boolean -> data
            else -> if (isNumeric(data::class)) {
                data != 0
            } else {
                throw TargetException("Can not convert $data (${data::class.java.simpleName}) to boolean")
            }
        }

        /**
         * Converts a value to double
         * @param data Any Data value
         * @return Boolean
         */
        private fun toSiteWiseDouble(data: Any) = when (data) {
            is String -> try {
                data.toDouble()
            } catch (e: NumberFormatException) {
                throw TargetException("Can not convert string \"$data\" to Double")
            }

            is Boolean -> if (data) 1.0 else 0.0
            is Byte -> data.toDouble()
            is Short -> data.toDouble()
            is Int -> data.toDouble()
            is Long -> data.toDouble()
            is UByte -> data.toDouble()
            is UShort -> data.toDouble()
            is UInt -> data.toDouble()
            is ULong -> data.toDouble()
            is Double -> data
            is Float -> data.toDouble()
            else -> throw TargetException("Can not convert $data (${data::class.java.simpleName}) to Double")
        }

        /**
         * Converts a value to integer
         * @param data Any Data value
         * @return Boolean
         */
        private fun toSiteWiseInt(data: Any) = when (data) {
            is String -> try {
                data.toInt()
            } catch (e: NumberFormatException) {
                throw TargetException("Can not convert string \"$data\" to Integer")
            }

            is Boolean -> if (data) 1 else 0
            is Byte -> data.toInt()
            is Short -> data.toInt()
            is Int -> data
            is Long -> data.toInt()
            is UByte -> data.toInt()
            is UShort -> data.toInt()
            is UInt -> data.toInt()
            is ULong -> data.toInt()
            is Double -> data.toInt()
            is Float -> data.toInt()
            else -> throw TargetException("Can not convert $data (${data::class.java.simpleName}) to Integer")
        }

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
        )

    }
}











