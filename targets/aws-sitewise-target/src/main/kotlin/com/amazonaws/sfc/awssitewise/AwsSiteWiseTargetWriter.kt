// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awssitewise


import com.amazonaws.sfc.awssitewise.config.*
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseAssetConfiguration.Companion.CONFIG_ASSET_ID
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseAssetCreationConfiguration.Companion.CONFIG_ASSET_EXTERNAL_ID
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseAssetCreationConfiguration.Companion.CONFIG_ASSET_NAME
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseWriterConfiguration.Companion.AWS_SITEWISE
import com.amazonaws.sfc.awssitewise.config.SiteWiseAssetPropertyConfiguration.Companion.CONFIG_PROPERTY_ALIAS
import com.amazonaws.sfc.awssitewise.config.SiteWiseAssetPropertyConfiguration.Companion.CONFIG_PROPERTY_EXTERNAL_ID
import com.amazonaws.sfc.awssitewise.config.SiteWiseAssetPropertyConfiguration.Companion.CONFIG_PROPERTY_ID
import com.amazonaws.sfc.awssitewise.config.SiteWiseAssetPropertyConfiguration.Companion.CONFIG_PROPERTY_NAME
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.data.DataTypes.isNumeric
import com.amazonaws.sfc.data.JsonHelper.Companion.gsonExtended
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
import com.amazonaws.sfc.system.DateTime.systemDateTime
import com.amazonaws.sfc.targets.AwsServiceTargetClientHelper
import com.amazonaws.sfc.targets.TargetDataChannel
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.*
import com.google.gson.Gson
import io.burt.jmespath.Expression
import kotlinx.coroutines.*
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
class AwsSiteWiseTargetWriter(private val targetID: String, private val configReader: ConfigReader, private val logger: Logger, resultHandler: TargetResultHandler?) : TargetWriter {

    private val className = this::class.java.simpleName

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }


    private val metricDimensions = mapOf(
        METRICS_DIMENSION_SOURCE to targetID, MetricsCollector.METRICS_DIMENSION_TYPE to className)

    private val scope = buildScope("Sitewise Target")

    private val clientHelper = AwsServiceTargetClientHelper(
        configReader.getConfig<AwsSiteWiseWriterConfiguration>(), targetID, IoTSiteWiseClient.builder(), logger)

    private val assetHelper by lazy {

        val assetCreationConfiguration = targetConfig.assetCreationConfiguration

        val anyAssetsByName = targetConfig.assets.map { it.assetName }.any { it != null }

        val anyPropertiesByName = targetConfig.assets.flatMap { asset -> asset.properties.map { prop -> prop.propertyName } }.any { it != null }

        val anyAssetsByExternalIdentity = targetConfig.assets.map { it.assetExternalID }.any { it != null }

        val anyAssetPropertiesByExternalIdentity = targetConfig.assets.flatMap { asset -> asset.properties.map { prop -> prop.propertyExternalID } }.any { it != null }

        val anyAssetsOrPropertiesNeedLookup = anyAssetsByName || anyPropertiesByName || anyAssetsByExternalIdentity || anyAssetPropertiesByExternalIdentity

        if (assetCreationConfiguration != null || anyAssetsOrPropertiesNeedLookup) {
            SiteWiseAssetHelper(
                sitewiseClient,
                targetID, assetCreationConfiguration ?: AwsSiteWiseAssetCreationConfiguration(),
                assetCreationConfiguration != null, logger)
        } else null
    }

    private val targetResults = if (resultHandler != null) TargetResultBufferedHelper(targetID, resultHandler, logger) else null
    private val sitewiseClient: AwsSiteWiseClient
        get() = AwsSiteWiseClientWrapper(clientHelper.serviceClient as IoTSiteWiseClient)

    // Configured field names
    private val elementNames = config.elementNames


    private var _targetConfig: AwsSiteWiseTargetConfiguration? = null
    private val targetConfig: AwsSiteWiseTargetConfiguration
        get() {
            if (_targetConfig == null) {
                _targetConfig = clientHelper.targetConfig(config, targetID, AWS_SITEWISE)
            }
            return _targetConfig as AwsSiteWiseTargetConfiguration
        }


    // channel for passing messages to coroutine that sends messages to SiteWise queue
    private val targetDataChannel = TargetDataChannel.create(targetConfig, "$className:targetDataChannel")

    // Buffer to collect batch of property values
    private val propertyValuesBuffer = mutableMapOf<String?, MutableMap<String, MutableList<AssetPropertyValue>>>()
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
                logger = logger)
        } else null
    }
    private val collectMetricsFromLogger: MetricsCollectorMethod? = if (config.isCollectingMetrics) {
        { metricsList ->
            try {
                val dataPoints = metricsList.map {
                    MetricsDataPoint(
                        it.metricsName, metricDimensions, it.metricUnit, it.metricsValue)
                }
                runBlocking {
                    metricsCollector?.put(targetID, dataPoints)
                }
            } catch (e: java.lang.Exception) {
                logger.getCtxErrorLogEx(
                    this::class.java.simpleName, "collectMetricsFromLogger")("Error collecting metrics from logger", e)
            }
        }
    } else null

    override val metricsProvider: MetricsProvider? by lazy {
        if (metricsCollector != null) InProcessMetricsProvider(metricsCollector!!, logger) else null
    }


    // coroutine that writes messages to queue
    private val writer = scope.launch("Writer") {

        val log = logger.getCtxLoggers(AwsSiteWiseTargetWriter::class.java.simpleName, "writer")
        log.info("AWS SiteWise writer for target \"$targetID\" in region ${targetConfig.region}")

        var timer = timerJob()

        while (isActive) {
            try {
                select {

                    targetDataChannel.onReceive { targetData ->
                        if (handleTargetData(targetData)) {
                            timer.cancel()
                            timer = timerJob()
                        }
                    }

                    timer.onJoin {
                        log.trace("${(targetConfig.interval.inWholeMilliseconds)} milliseconds buffer interval reached, flushing buffer")
                        flush()
                        timer = timerJob()
                    }
                }


            } catch (e: Exception) {
                if (!e.isJobCancellationException) log.errorEx("Error in writer", e)
            }
        }
    }

    private suspend fun handleTargetData(targetData: TargetData): Boolean {

        val log = logger.getCtxLoggers(className, "handleTargetData")

        if (logger.level == LogLevel.TRACE) {
            val json = targetData.toJson(elementNames, targetConfig.unquoteNumericJsonValues)
            log.trace("Writer received data \"$json\"")
        }

        targetResults?.add(targetData)

        handleTargetDataForCreatedAssets(targetData)
        handleDataForConfiguredAssets(targetData)

        // Keeps track of reads in batch, flush if configured value is reached
        batchCount += 1
        if (targetData.noBuffering || batchCount >= targetConfig.batchSize) {
            flush()
            batchCount = 0
            return true
        }
        return false
    }

    private fun handleDataForConfiguredAssets(targetData: TargetData) {

        // Remapping keys of the data to escape unsupported characters for jmespath queries
        val data = targetData.toMap(config.elementNames, jmesPathCompatibleKeys = true) // For every configured asset
        targetConfig.assets.forEach { asset ->
            val assetID = getAssetId(asset, logger)
            asset.properties.forEach { property ->

                val propertyIdOrAlias = getPropertyIdOrAlias(property, assetID)

                if (propertyIdOrAlias != null) {
                    buildAndStorePropertyValue(asset, property, assetID, propertyIdOrAlias, data)
                }
            }

        }
    }

    private fun buildAndStorePropertyValue(assetConfig: AwsSiteWiseAssetConfiguration,
                                           propertyConfig: SiteWiseAssetPropertyConfiguration,
                                           assetID: String?,
                                           propertyIdOrAlias: String,
                                           data: Map<String, Any>) {

        val log = logger.getCtxLoggers(className, "buildAnsStorePropertyValue")

        val valueAndTimeStamp = getPropertyValueAndTimeStamp(data, assetConfig, propertyConfig, log)
        if (valueAndTimeStamp != null) {
            try {

                val assetValue = buildAssetValue(propertyConfig.dataType, valueAndTimeStamp.first, valueAndTimeStamp.second)

                val byAlias: Boolean = propertyConfig.propertyAlias != null

                storeValueAndTimestampInBuffer(
                    // when using an alias the property id is not needed
                    (if (byAlias) null else assetID),
                    // when using an alias the property id is the alias name
                    propertyIdOrAlias,
                    assetValue)

            } catch (e: Exception) {
                val valueStr = "${valueAndTimeStamp.first} (${valueAndTimeStamp.first::class.java.simpleName}"
                log.errorEx("Error building or storing property value for property ${propertyConfig.asString} from value $valueStr)", e)
            }
        }
    }

    private fun getAssetId(asset: AwsSiteWiseAssetConfiguration, logger: Logger): String? =
        when {
            (!asset.assetID.isNullOrEmpty()) -> asset.assetID

            (!asset.assetName.isNullOrEmpty()) -> {
                val assetID = assetHelper?.assetDetailsByName?.get(asset.assetName)?.assetId()
                if (assetID == null) {
                    logger.getCtxWarningLog("Asset with $CONFIG_ASSET_NAME \"${asset.assetName}\" could not be found")
                }
                assetID
            }

            (!asset.assetExternalID.isNullOrEmpty()) -> {
                val assetID = assetHelper?.assetDetailsByExternalID?.get(asset.assetExternalID)?.assetId()
                if (assetID == null) {
                    logger.getCtxWarningLog(className, "getAssetId")("Asset with $CONFIG_ASSET_EXTERNAL_ID \"${asset.assetExternalID}\" could not be found")
                }
                assetID
            }

            (!asset.assetExternalID.isNullOrEmpty()) -> {
                val assetID = assetHelper?.assetDetailsByExternalID?.get(asset.assetExternalID)?.assetId()
                if (assetID == null) {
                    logger.getCtxWarningLog(className, "getAssetId")("Asset with $CONFIG_ASSET_EXTERNAL_ID \"${asset.assetExternalID}\" could not be found")
                }
                assetID
            }

            else -> {
                if (asset.properties.any { it.propertyAlias.isNullOrEmpty() }) {
                    logger.getCtxWarningLog(className, "getAssetId")("No $CONFIG_ASSET_ID, $CONFIG_ASSET_NAME or $CONFIG_ASSET_EXTERNAL_ID  specified for asset, ${Gson().toJson(asset)}")
                }
                null
            }
        }


    private suspend fun handleTargetDataForCreatedAssets(targetData: TargetData) {

        val log = logger.getCtxLoggers(className, "handleTargetDataForCreatedAssets")

        if (assetHelper != null && assetHelper!!.createAssets) {
            targetData.sources.forEach { (sourceName, sourceData) ->
                try {
                    val (asset, channelToAssetPropertyMap) = assetHelper!!.assetAndPropertiesForSource(
                        sourceName, targetData)
                    sourceData.channels.filter { it.value.value != null }.forEach { (channelName, channelData) ->

                        val channelPropertyName = targetConfig.assetCreationConfiguration!!.renderAssetPropertyName(targetID, sourceName, channelName, targetData)

                        val assetProperty: AssetProperty? = channelToAssetPropertyMap[channelName]
                        if (assetProperty != null) {
                            val dataType = SiteWiseDataType.from(assetProperty.dataType())
                            val assetValue = buildAssetValue(
                                dataType, channelData.value!!, assetHelper?.getPropertyTimestamp(targetData, sourceData, channelData) ?: systemDateTime())
                            storeValueAndTimestampInBuffer(
                                asset, assetProperty.id(), assetValue)
                        } else {
                            log.warning("No property for channel \"$channelName\" with name \"$channelPropertyName\" in asset \"$asset\"")
                        }
                    }
                } catch (e: Exception) {
                    println(e)
                    log.error("Error getting asset for source $sourceName, $e")
                }
            }
        }
    }


    private fun CoroutineScope.timerJob(): Job {
        return launch("Timeout timer") {

            return@launch try {
                delay(targetConfig.interval)
            } catch (e: Exception) { // no harm done, timer is just used to guard for timeouts
            }
        }
    }


    /**
     * Writes message to SiteWise target
     * @param targetData TargetData
     */
    override suspend fun writeTargetData(targetData: TargetData) {
        targetDataChannel.submit(targetData, logger.getCtxLoggers(className, "writeTargetData"))
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
        val usedDataType = if (dataType != SiteWiseDataType.UNSPECIFIED) dataType else SiteWiseDataType.fromValue(value)

        // Build the AssetPropertyValue
        return AssetPropertyValue.builder().timestamp(
            TimeInNanos.builder().timeInSeconds(timestamp.epochSecond).offsetInNanos(timestamp.nano).build()).value(buildValue(usedDataType, value)).build()
    }

    /**
     * Stores a AssetPropertyValue in the buffer
     * @param assetID String Asset ID
     * @param propertyID SiteWiseAssetPropertyConfiguration Asset property configuration
     * @param propValue AssetPropertyValue The AssetPropertyValue to store
     */
    private fun storeValueAndTimestampInBuffer(assetID: String?, propertyID: String, propValue: AssetPropertyValue) {

        // Get entry for asset
        var assetEntry = propertyValuesBuffer[assetID]
        if (assetEntry == null) {
            propertyValuesBuffer[assetID] = mutableMapOf()
            assetEntry = propertyValuesBuffer[assetID]
        }

        // Get entry in asset for property
        var propertyEntry = assetEntry!![propertyID]
        if (propertyEntry == null) {
            assetEntry[propertyID] = mutableListOf()
            propertyEntry = assetEntry[propertyID]
        }

        // Add to list of stored values
        val entryWithSameTimestamp = propertyEntry!!.find { it.timestamp() == propValue.timestamp() }
        if (entryWithSameTimestamp != null) {
            propertyEntry.remove(entryWithSameTimestamp)
        }
        propertyEntry.add(propValue)
    }

    fun searchData(query: Expression<Any>?, data: Map<String, Any>, prop: SiteWiseAssetPropertyConfiguration): Any? = try {
        query?.search(data)
    } catch (e: NullPointerException) {
        null
    } catch (e: Exception) {
        val log = logger.getCtxErrorLogEx(className, "searchData")
        log("Error querying data for target \"$targetID\", property $prop", e)
        null
    }

    /**
     * Extracts property value and timestamp from the received data
     * @param data Mapping<String, Mapping<String, Any?>> Data received by target writer
     * @param asset AwsSitewiseAssetConfig Configuration of the asset
     * @param prop SiteWiseAssetPropertyConfiguration Configuration of the property
     * @param log ContextLogger Logger for output
     * @return Pair<Any, Instant>? Pair containing value and timestamp if found, else null
     */
    private fun getPropertyValueAndTimeStamp(data: Map<String, Any>, asset: AwsSiteWiseAssetConfiguration, prop: SiteWiseAssetPropertyConfiguration, log: Logger.ContextLogger): Pair<Any, Instant>? {

        // internal method for fetching a specified value from input value this is a map
        fun valueFromMap(v: Any?, key: String): Any? = if ((v is Map<*, *>) && (v.containsKey(key))) v[key] else null

        // Search property data
        val dataSearchResult = searchData(prop.dataPath, data, prop) // If the value is a map the value can be in the value field
        val propertyValue: Any? = (valueFromMap(dataSearchResult, elementNames.value)) ?: dataSearchResult

        // No data for this property
        if (propertyValue == null) {
            if ((prop.warnIfNotPresent && logger.level != LogLevel.TRACE) || (logger.level == LogLevel.TRACE)) {
                log.warning("No value found for target \"$targetID\", asset \"${asset.asString}\", property \"${prop.asString}\" for dataPath \"${prop.dataPathStr}\"")
            }
            return null
        }

        log.trace("Value $propertyValue (${propertyValue::class.java.simpleName}) found for target \"$targetID\" , asset \"${asset.asString}\", property \"${prop.asString}\" using path ${prop.dataPathStr}")

        val timestampPath = prop.timestampPath

        // Test if there is an explicit timestamp query, in that case use it to query for the timestamp
        val timestampValue: Instant? = if (timestampPath != null) {
            val timestampSearchResult = searchData(timestampPath, data, prop) as? Instant?
            (valueFromMap(timestampSearchResult, elementNames.timestamp) as? Instant?) ?: timestampSearchResult
        } else {
            getImplicitTimeStamp(prop, data)
        }
        if (timestampValue == null) {
            log.trace("No timestamp found for target \"$targetID\", asset ${asset.asString}, property ${prop.asString}\" for timestampPath \"${prop.timestampPathStr}\"")
            return null
        }

        log.trace("Timestamp $timestampValue found for target \"$targetID\", asset \"${asset.asString}\", property \"${prop.asString}\"")

        return propertyValue to timestampValue

    }

    private fun getImplicitTimeStamp(prop: SiteWiseAssetPropertyConfiguration, data: Map<String, Any>): Instant? {

        return getImplicitValueTimestamp(prop, data) ?: getImplicitSourceTimestamp(prop, data) ?: data[elementNames.timestamp] as? Instant?
    }

    private fun getImplicitValueTimestamp(prop: SiteWiseAssetPropertyConfiguration, data: Map<String, Any>): Instant? {

        val log = logger.getCtxLoggers(className, "getImplicitValueTimestamp")

        val valueTimeStampPathStr = if (prop.dataPathStr?.endsWith(elementNames.value) == true) {
            val pathElements = prop.dataPathStr!!.split(".").toMutableList()
            pathElements[pathElements.lastIndex] = elementNames.timestamp
            pathElements.joinToString(separator = ".")
        } else null

        val implicitTimestampPath = if (valueTimeStampPathStr != null) SiteWiseAssetPropertyConfiguration.getExpression(valueTimeStampPathStr) else null

        val valueTimeStamp = if (implicitTimestampPath != null) searchData(implicitTimestampPath, data, prop) as? Instant? else null
        if (valueTimeStamp != null) {
            log.trace("Found timestamp $valueTimeStamp for value using implicit path \"$valueTimeStampPathStr\"")
        }
        return valueTimeStamp
    }

    private fun getImplicitSourceTimestamp(prop: SiteWiseAssetPropertyConfiguration, data: Map<String, Any>): Instant? {

        val log = logger.getCtxLoggers(className, "getImplicitSourceTimestamp")

        var pathElements = prop.dataPathStr!!.split(".")
        val implicitTimestampPathStr = if (pathElements.indexOf(elementNames.timestamp) == 2) {
            pathElements = pathElements.subList(0, 3)
            pathElements.joinToString(separator = ".")
        } else null
        val implicitSourceTimestampPath = if (implicitTimestampPathStr != null) SiteWiseAssetPropertyConfiguration.getExpression(
            implicitTimestampPathStr) else null

        val sourceTimeStamp = if (implicitSourceTimestampPath != null) searchData(
            implicitSourceTimestampPath, data, prop) as? Instant? else null
        if (sourceTimeStamp != null) {
            log.trace("Found timestamp $sourceTimeStamp for source using implicit path \"$implicitSourceTimestampPath\"")
        }

        return sourceTimeStamp
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
        buildBatchPutAssetRequestsForBufferedValues().forEach { request ->
            try {

                val start = systemDateTime().toEpochMilli()
                val resp = clientHelper.executeServiceCallWithRetries {
                    try {
                        log.info("Writing batch of ${request.entries().sumOf { it.propertyValues().count() }} values to " +
                                "${request.entries().count()} properties for " +
                                "${request.entries().groupBy { it.assetId() }.filter { it.key != null }.count()} configured assets(s)")
                        val r = sitewiseClient.batchPutAssetPropertyValue(request)
                        val writeDurationInMillis = (systemDateTime().toEpochMilli() - start).toDouble()
                        createMetrics(targetID, metricDimensions, request, writeDurationInMillis)
                        r
                    } catch (e: AwsServiceException) {
                        log.errorEx("SiteWise batchPutAssetPropertyValue error", e)
                        runBlocking {
                            metricsCollector?.put(
                                targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions)
                        }

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
            } catch (e: IllegalStateException) {
                log.info("SiteWise target \"$targetID\" adapter closing down \"$targetID\"")
            } catch (e: Exception) {
                log.errorEx("Error sending to SiteWise \"$targetID\"", e)
                if (canNotReachAwsService(e)) {
                    targetResults?.nackBuffered()
                } else {
                    targetResults?.errorBuffered()
                }
            }
        }
        propertyValuesBuffer.clear()
    }

    private fun createMetrics(adapterID: String, metricDimensions: MetricDimensions, request: BatchPutAssetPropertyValueRequest, writeDurationInMillis: Double) {

        runBlocking {
            metricsCollector?.put(
                adapterID, metricsCollector?.buildValueDataPoint(
                    adapterID, MetricsCollector.METRICS_MEMORY, MemoryMonitor.getUsedMemoryMB().toDouble(), MetricUnits.MEGABYTES), metricsCollector?.buildValueDataPoint(
                    adapterID, METRICS_WRITES, 1.0, MetricUnits.COUNT, metricDimensions), metricsCollector?.buildValueDataPoint(
                    adapterID, METRICS_MESSAGES, request.entries().size.toDouble(), MetricUnits.COUNT, metricDimensions), metricsCollector?.buildValueDataPoint(
                    adapterID, METRICS_WRITE_DURATION, writeDurationInMillis, MetricUnits.MILLISECONDS, metricDimensions), metricsCollector?.buildValueDataPoint(
                    adapterID, METRICS_WRITE_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions), metricsCollector?.buildValueDataPoint(
                    adapterID, METRICS_WRITE_SIZE, request.toString().length.toDouble(), MetricUnits.BYTES, metricDimensions))
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
    private fun buildBatchPutAssetRequestsForBufferedValues(): List<BatchPutAssetPropertyValueRequest> {


        val log = logger.getCtxLoggers(className, "buildBatchRequests")


        return propertyValuesBuffer.map { (assetID, properties) ->

            properties.map { (propertyIdOrAlias, propertyValues) ->

                // get values for this asset property in sets of 10
                propertyValues.chunked(10).map { listOf10Values ->
                    val builder = PutAssetPropertyValueEntry.builder()
                        .entryId(UUID.randomUUID().toString())
                        .propertyValues(listOf10Values)

                    // assetID is null when an alias was used in the property configuration, in which case only this alias must be specified
                    if (assetID == null) {
                        builder.propertyAlias(propertyIdOrAlias)
                    } else {
                        // no alias, requires assetID and the propertyID (which can be looked up from its name or externalID in the property config
                        builder.assetId(assetID)
                        builder.propertyId(propertyIdOrAlias)
                    }

                    builder.build()
                }
            }.flatten() // flatten list for asset
        }.flatten() // flatten list for all
            // get 10 entries per request
            .chunked(10).map { i ->
                val batchPutAssetPropertyValueRequest = BatchPutAssetPropertyValueRequest.builder().entries(i).build()
                if (logger.level == LogLevel.TRACE) {
                    log.trace("Created BatchPutAssetPropertyValueRequest request with ${batchPutAssetPropertyValueRequest.entries().size} entries for ${
                        batchPutAssetPropertyValueRequest.entries().sumOf { it.propertyValues().size }
                    } values")
                }
                batchPutAssetPropertyValueRequest
            }
    }


    private fun getPropertyIdOrAlias(propertyConfig: SiteWiseAssetPropertyConfiguration, assetID: String?): String? {

        if (!propertyConfig.propertyAlias.isNullOrEmpty()) return propertyConfig.propertyAlias

        val log = logger.getCtxLoggers(className, "getPropertyIdOrAlias")
        if (assetID == null) {
            log.warning(
                "Sitewise target \"$targetID\",  property ${propertyConfig.asString}\" can not be found as asset can not be identified, which is required for a property with a " +
                        "$CONFIG_PROPERTY_ID, $CONFIG_PROPERTY_NAME or $CONFIG_PROPERTY_EXTERNAL_ID , " +
                        "configure a $CONFIG_ASSET_ID, $CONFIG_ASSET_NAME or $CONFIG_PROPERTY_EXTERNAL_ID for this asset or use a $CONFIG_PROPERTY_ALIAS for this property.")
            return null
        }

        return when {

            (!propertyConfig.propertyID.isNullOrEmpty()) -> propertyConfig.propertyID

            (!propertyConfig.propertyName.isNullOrEmpty()) -> {
                val propertyID = getPropertyIdByName(assetID, propertyConfig)
                if (propertyID == null) {
                    val availableNames = assetHelper?.assetDetailsById?.get(assetID)?.assetProperties()?.joinToString(prefix = "[", postfix = "]", separator = ", ") { it.name() }
                    log.warning("Sitewise target \"$targetID\", asset ID \"$assetID\", property with " + "$CONFIG_PROPERTY_NAME \"${propertyConfig.propertyName}\" not found, available properties names in this asset are $availableNames")
                }
                propertyID
            }

            (!propertyConfig.propertyExternalID.isNullOrEmpty()) -> {
                val propertyID = getPropertyIdByExternalID(assetID, propertyConfig)
                if (propertyID == null) {
                    val availableExternalIDs = assetHelper?.assetDetailsById?.get(assetID)?.assetProperties()?.joinToString(prefix = "[", postfix = "]", separator = ", ") { it.externalId() }
                    log.warning("Sitewise target \"$targetID\", asset ID \"$assetID\", property with " + "$CONFIG_PROPERTY_EXTERNAL_ID \"${propertyConfig.propertyName}\" not found, available properties names in this asset are $availableExternalIDs")
                }
                propertyID
            }

            else -> {
                log.warning("Sitewise target \"$targetID\", asset ID \"$assetID\", property ${Gson().toJson(propertyConfig)} no $CONFIG_PROPERTY_ID, $CONFIG_PROPERTY_NAME, $CONFIG_PROPERTY_EXTERNAL_ID, $CONFIG_PROPERTY_ALIAS specified")
                null
            }
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
            SiteWiseDataType.STRING -> {
                val s = toSiteWiseString(data)
                variantBuilder.stringValue(s)
            }

            SiteWiseDataType.BOOLEAN -> variantBuilder.booleanValue(toSiteWiseBoolean(data))
            else -> variantBuilder.stringValue(data.toString())
        }
        return variantBuilder.build()
    }

    private fun toSiteWiseString(data: Any): String = when (data) {
        is Map<*, *> -> gsonExtended().toJson(data)
        is ArrayList<*> -> data.joinToString(prefix = "[", postfix = "]", separator = ",") { toSiteWiseString(it) }
        else -> data.toString()
    }

    /**
     * Gets the config for SiteWise Target writer
     */
    private val config: AwsSiteWiseWriterConfiguration
        get() {
            return clientHelper.writerConfig(configReader, AWS_SITEWISE)
        }

    private fun getPropertyIdByName(assetID: String, propertyConfig: SiteWiseAssetPropertyConfiguration): String? {
        val log = logger.getCtxLoggers(className, "getPropertyIdByName")

        val assetProperties = assetHelper?.assetDetailsById?.get(assetID)?.assetProperties() ?: emptyList()
        val propertyID = assetProperties.find { p -> p.name() == propertyConfig.propertyName }?.id()
        if (propertyID == null) {

            val availablePropertyNames = assetProperties.filter { p -> !p.name().isNullOrEmpty() }.map { p -> p.name() }
            log.warning("Sitewise target \"$targetID\", asset ID \"$assetID\", property with $CONFIG_PROPERTY_NAME \"${propertyConfig.propertyName}\" not found, available  names for this asset are $availablePropertyNames")
        }
        return propertyID
    }

    private fun getPropertyIdByExternalID(assetID: String, propertyConfig: SiteWiseAssetPropertyConfiguration): String? {
        val log = logger.getCtxLoggers(className, "getPropertyIdByExternalID")

        val assetProperties = assetHelper?.assetDetailsById?.get(assetID)?.assetProperties() ?: emptyList()
        val propertyID = assetProperties.find { p -> p.externalId() == propertyConfig.propertyExternalID }?.id()
        if (propertyID == null) {
            val availablePropertyNames = assetProperties.filter { p -> !p.externalId().isNullOrEmpty() }.map { p -> p.externalId() }
            log.warning("Sitewise target \"$targetID\", asset ID \"$assetID\", property with  $CONFIG_PROPERTY_EXTERNAL_ID \"${propertyConfig.propertyExternalID}\" not found, available external identifiers for this asset are $availablePropertyNames")
        }
        return propertyID
    }

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any?) = newInstance(
            createParameters[0] as ConfigReader, createParameters[1] as String, createParameters[2] as Logger, createParameters[3] as TargetResultHandler?)

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
            } catch (e: ConfigurationException) {
                throw e
            } catch (e: Throwable) {
                throw TargetException("Error creating AWS SiteWise target writer, ${e.message}")
            }
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
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET)

    }
}













