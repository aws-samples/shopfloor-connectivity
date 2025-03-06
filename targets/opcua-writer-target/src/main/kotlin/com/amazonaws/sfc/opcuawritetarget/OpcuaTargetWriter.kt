// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.opcuawritetarget

import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.ElementNamesConfiguration
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.data.JsonHelper.Companion.extendedJsonException
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_VALUES_WRITTEN
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITES
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_DURATION
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_ERRORS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_SUCCESS
import com.amazonaws.sfc.opcuawritetarget.OpcuaDataType.Companion.toVariant
import com.amazonaws.sfc.opcuawritetarget.config.OpcuaNodeConfiguration
import com.amazonaws.sfc.opcuawritetarget.config.OpcuaNodeConfiguration.Companion.CONFIG_DATA_TYPE
import com.amazonaws.sfc.opcuawritetarget.config.OpcuaNodeConfiguration.Companion.CONFIG_DIMENSIONS
import com.amazonaws.sfc.opcuawritetarget.config.OpcuaWriterConfiguration
import com.amazonaws.sfc.opcuawritetarget.config.OpcuaWriterConfiguration.Companion.OPCUA_WRITER_TARGET
import com.amazonaws.sfc.opcuawritetarget.config.OpcuaWriterTargetConfiguration
import com.amazonaws.sfc.targets.TargetDataChannel
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.transformations.invoke
import com.amazonaws.sfc.util.MemoryMonitor
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.isJobCancellationException
import com.amazonaws.sfc.util.launch
import com.google.gson.JsonSyntaxException
import io.burt.jmespath.Expression
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import org.eclipse.milo.opcua.stack.core.StatusCodes
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant
import java.time.Instant
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import kotlin.time.toDuration

class OpcuaTargetWriter(
    private val targetID: String,
    configReader: ConfigReader,
    private val targetConfig: OpcuaWriterTargetConfiguration,
    private val logger: Logger,
    private val resultHandler: TargetResultHandler?
) : TargetWriter {

    private val className = this::class.java.simpleName
    private val config: OpcuaWriterConfiguration by lazy { configReader.getConfig() }

    private val targetWriterScope = buildScope(className, dispatcher = Dispatchers.IO)
    private var writeCount: AtomicInteger = AtomicInteger(0)
    private var valuesCount: AtomicInteger = AtomicInteger(0)

    override val metricsProvider: MetricsProvider? by lazy {
        if (metricsCollector != null) InProcessMetricsProvider(metricsCollector!!, logger) else null
    }

    val opcuaWriter by lazy { OpcuaWriter(targetID, targetConfig, logger, metricsCollector, metricDimensions) }


    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val metricDimensions = mapOf(
        METRICS_DIMENSION_SOURCE to targetID,
        MetricsCollector.METRICS_DIMENSION_TYPE to className
    )

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


    override suspend fun writeTargetData(targetData: TargetData) {
        targetDataChannel.submit(targetData, logger.getCtxLoggers(className, "writeTargetData"))
    }

    private val targetResults = if (resultHandler != null) TargetResultHelper(targetID, resultHandler, logger) else null


    private val targetDataChannel = TargetDataChannel.create(targetConfig, "$className:targetDataChannel")

    private val monitorTask = targetWriterScope.launch(context = Dispatchers.IO, name = "Monitor") { monitor() }
    private val writerTask = targetWriterScope.launch(context = Dispatchers.IO, name = "Writer") { writer() }

    // Configured field names
    private val elementNames = config.elementNames

    private suspend fun CoroutineScope.writer() {
        val log = logger.getCtxLoggers(className, "writer")

        while (isActive) {
            try {
                select {

                    targetDataChannel.channel.onReceive { targetData ->
                        log.trace("Received target data ${targetData.toJson(ElementNamesConfiguration(), true, pretty = false)}")

                        val client = opcuaWriter.getClient()
                        if (client == null) {
                            targetResults?.error(targetData)
                            return@onReceive
                        }

                        val data = targetData.toMap(config.elementNames, jmesPathCompatibleKeys = true)
                        var duration: Duration
                        var writtenValues = 0
                        var writes = 0

                        duration = measureTime {

                            // Create list of nodes and values to write
                            val nodesAndValuesToWrite = sequence {
                                targetConfig.nodes.forEach { node ->
                                    val valueAndTimestamp = getNodeValueAndTimeStamp(data, node, log)
                                    if (valueAndTimestamp?.first != null) {
                                        val value = if (!node.transformationID.isNullOrEmpty()) {
                                            applyTransformation(valueAndTimestamp.first, node.nodeId?.toParseableString() ?: "", node.transformationID!!)
                                        } else valueAndTimestamp.first
                                        if (value != null) {
                                            val dataValue = buildValue(node, value, valueAndTimestamp.second)
                                            yield(node.nodeId to dataValue)
                                        }
                                    }
                                }
                            }.toList()

                            // Write data to the nodes
                            val results = nodesAndValuesToWrite.chunked(targetConfig.writeBatchSize).map { batch ->
                                val nodeIds = batch.map { it.first }
                                val values = batch.map { it.second }
                                var status = client.writeValues(nodeIds, values)
                                writes += 1
                                writeCount.addAndGet(1)
                                Triple(nodeIds, values, status)
                            }

                            // Process write results
                            results.forEach { result ->
                                try {
                                    result.third?.get()?.forEachIndexed { i, statusCode ->

                                        val value = result.second[i].value.value
                                        val dataTypeStr = OpcuaDataType.fromIdentifier((result.second[i].value.dataType).get())
                                        val valueWithType = "${if ((value is Array<*>)) value.toList().joinToString(prefix = "[", postfix = "]") else value}:$dataTypeStr"

                                        if (statusCode == StatusCode.GOOD) {
                                            if (logger.level == LogLevel.TRACE) log.trace("Written value  $valueWithType to node ${result.first[i]?.toParseableString()}")
                                            writtenValues += 1
                                            valuesCount.addAndGet(1)
                                        } else {
                                            val errorDescription = " ${StatusCodes.lookup(statusCode.value).get().joinToString()}"
                                            val statusCodeHex = "0x${statusCode.value.toString(16)}"
                                            log.error("Error writing value $valueWithType to node ${result.first[i]?.toParseableString()}, status code is $statusCodeHex :$errorDescription ")
                                            targetResults?.error(targetData)
                                        }
                                    }

                                    // ack the result as the actual writes were successful despite some nodes that may not have been written
                                    targetResults?.ack(targetData)

                                } catch (e: Exception) {
                                    if (e is ExecutionException && e.message?.contains("UaSerializationException") != false)
                                        log.error("Error writing data to server because of datatype error, set nodes $CONFIG_DATA_TYPE, and $CONFIG_DIMENSIONS for array data, to exactly match the type and dimensions of the data, $e")
                                    else
                                        log.error("Error writing value to server, $e")
                                    targetResults?.error(targetData)
                                    runBlocking { metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions) }
                                }
                            }
                        }
                        createMetrics(targetID, metricDimensions, writes, writtenValues, duration)
                    }
                }
            // outer level catch to keep writing task
            } catch (e: Exception) {
                if (!e.isJobCancellationException)
                    log.errorEx("Error writing values", e)
            }
        }
    }

    private suspend fun CoroutineScope.monitor() {

        val interval = 60.toDuration(DurationUnit.SECONDS)
        while (isActive) {
            try {
                delay(interval)
                logger.getCtxInfoLog(className, "monitor")("${writeCount.get()} writes to sever ${targetConfig.endPoint} for ${valuesCount.get()} values over the last $interval")
                writeCount.set(0)
                valuesCount.set(0)

            } catch (e: Exception) {
                if (!e.isJobCancellationException) {
                    logger.getCtxErrorLogEx(className, "monitor")("Error in monitor", e)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildValue(node: OpcuaNodeConfiguration, value: Any, sourceTimeStamp: Instant): DataValue {
        val variant = when {
            value is Map<*, *> -> Variant(JsonHelper.gsonExtended().toJson(value))
            value is List<*> && value.isNotEmpty() -> {
                if (value.first() is ChannelOutputData) {
                    (value as List<ChannelOutputData>).map { it.value }.toVariant(node.dataType?.identifier, node.dimensions, logger)
                } else {
                    value.toVariant(node.dataType?.identifier, node.dimensions, logger)
                }
            }

            else -> value.toVariant(dimensions = node.dimensions.map { it.toInt() }, dataTypeIdentifier = node.dataType?.identifier, logger = logger)
        }
        return DataValue(variant, StatusCode.GOOD, DateTime(sourceTimeStamp), DateTime.now())
    }


    fun searchData(query: Expression<Any>?, data: Map<String, Any>, prop: OpcuaNodeConfiguration): Any? = try {
        query?.search(data)
    } catch (_: NullPointerException) {
        null
    } catch (e: Exception) {
        val log = logger.getCtxErrorLogEx(className, "searchData")
        log("Error querying data for target \"$targetID\", property $prop", e)
        null
    }


    private fun getNodeValueAndTimeStamp(data: Map<String, Any>, node: OpcuaNodeConfiguration, log: Logger.ContextLogger): Pair<Any, Instant>? {

        // internal method for fetching a specified value from input value this is a map
        fun valueFromMap(v: Any?, key: String): Any? = if ((v is Map<*, *>) && (v.containsKey(key))) v[key] else null

        // Search node data
        val dataSearchResult = searchData(node.dataPath, data, node) // If the value is a map the value can be in the value field
        val nodeValue: Any? = (valueFromMap(dataSearchResult, elementNames.value)) ?: dataSearchResult

        // No data for this node
        if (nodeValue == null) {
            if ((node.warnIfNotPresent && logger.level != LogLevel.TRACE) || (logger.level == LogLevel.TRACE)) {
                log.warning("No value found for target \"$targetID\", node \"${node.nodeId}\",  for dataPath \"${node.dataPathStr}\"")
            }
            return null
        }

        log.trace("Value $nodeValue (${nodeValue::class.java.simpleName}) found for target \"$targetID\", , property \"${node.nodeId}\" using path ${node.dataPathStr}")

        val timestampPath = node.timestampPath

        // Test if there is an explicit timestamp query, in that case use it to query for the timestamp
        val timestampValue: Instant? = if (timestampPath != null) {
            val timestampSearchResult = searchData(timestampPath, data, node) as? Instant?
            (valueFromMap(timestampSearchResult, elementNames.timestamp) as? Instant?) ?: timestampSearchResult
        } else {
            getImplicitTimeStamp(node, data)
        }
        if (timestampValue == null) {
            log.trace("No timestamp found for target \"$targetID\", node ${node.nodeId}\" for timestampPath \"${node.timestampPathStr}\"")
            return null
        }

        log.trace("Timestamp $timestampValue found for target \"$targetID\", node \"${node.nodeId}\"")

        return nodeValue to timestampValue

    }

    private fun getImplicitTimeStamp(prop: OpcuaNodeConfiguration, data: Map<String, Any>): Instant? {

        return getImplicitValueTimestamp(prop, data) ?: getImplicitSourceTimestamp(prop, data) ?: data[elementNames.timestamp] as? Instant?
    }

    private fun getImplicitValueTimestamp(node: OpcuaNodeConfiguration, data: Map<String, Any>): Instant? {

        val log = logger.getCtxLoggers(className, "getImplicitValueTimestamp")

        val valueTimeStampPathStr = if (node.dataPathStr?.endsWith(elementNames.value) == true) {
            val pathElements = node.dataPathStr!!.split(".").toMutableList()
            pathElements[pathElements.lastIndex] = elementNames.timestamp
            pathElements.joinToString(separator = ".")
        } else null

        val implicitTimestampPath = if (valueTimeStampPathStr != null) OpcuaNodeConfiguration.getExpression(valueTimeStampPathStr) else null

        val valueTimeStamp = if (implicitTimestampPath != null) searchData(implicitTimestampPath, data, node) as? Instant? else null
        if (valueTimeStamp != null) {
            log.trace("Found timestamp $valueTimeStamp for value using implicit path \"$valueTimeStampPathStr\"")
        }
        return valueTimeStamp
    }

    private fun getImplicitSourceTimestamp(prop: OpcuaNodeConfiguration, data: Map<String, Any>): Instant? {

        val log = logger.getCtxLoggers(className, "getImplicitSourceTimestamp")

        var pathElements = prop.dataPathStr!!.split(".")
        val implicitTimestampPathStr = if (pathElements.indexOf(elementNames.timestamp) == 2) {
            pathElements = pathElements.subList(0, 3)
            pathElements.joinToString(separator = ".")
        } else null
        val implicitSourceTimestampPath = if (implicitTimestampPathStr != null) OpcuaNodeConfiguration.getExpression(
            implicitTimestampPathStr) else null

        val sourceTimeStamp = if (implicitSourceTimestampPath != null) searchData(
            implicitSourceTimestampPath, data, prop) as? Instant? else null
        if (sourceTimeStamp != null) {
            log.trace("Found timestamp $sourceTimeStamp for source using implicit path \"$implicitSourceTimestampPath\"")
        }

        return sourceTimeStamp
    }

    private fun applyTransformation(value: Any, name: String, transformationID: String): Any? {
        val log = logger.getCtxLoggers(className, "applyTransformation")

        val transformation = config.transformations[transformationID] ?: return null
        return try {
            log.trace("Applying transformation \"$transformationID\" on value ${value}:${value::class.java.simpleName} to \"$name\"")
            val transformedValue = transformation.invoke(value, name, true, logger)
            log.trace("Result of transformation \"$transformationID\" is ${transformedValue}${if (transformedValue != null) ":${transformedValue::class.java.simpleName}" else ""}")
            transformedValue
        } catch (e: Exception) {
            logger.getCtxErrorLog(className, "applyTransformation")("Error applying transformation $transformationID to name \"$name\", $e")
            null
        }
    }


    private fun createMetrics(
        adapterID: String,
        metricDimensions: MetricDimensions,
        writes: Int,
        values: Int,
        duration: Duration
    ) {

        runBlocking {
            metricsCollector?.put(
                adapterID,
                metricsCollector?.buildValueDataPoint(
                    adapterID, MetricsCollector.METRICS_MEMORY,
                    MemoryMonitor.getUsedMemoryMB().toDouble(),
                    MetricUnits.MEGABYTES
                ),
                metricsCollector?.buildValueDataPoint(
                    adapterID,
                    MetricsCollector.METRICS_MEMORY,
                    MemoryMonitor.getUsedMemoryMB().toDouble(),
                    MetricUnits.MEGABYTES
                ),
                metricsCollector?.buildValueDataPoint(
                    adapterID,
                    METRICS_WRITES,
                    writes.toDouble(),
                    MetricUnits.COUNT,
                    metricDimensions),

                metricsCollector?.buildValueDataPoint(
                    adapterID,
                    METRICS_WRITE_DURATION,
                    duration.inWholeMilliseconds.toDouble(),
                    MetricUnits.MILLISECONDS,
                    metricDimensions
                ),
                metricsCollector?.buildValueDataPoint(
                    adapterID,
                    METRICS_WRITES, 1.0,
                    MetricUnits.COUNT,
                    metricDimensions),

                metricsCollector?.buildValueDataPoint(
                    adapterID,
                    METRICS_VALUES_WRITTEN, values.toDouble(),
                    MetricUnits.COUNT,
                    metricDimensions),

                metricsCollector?.buildValueDataPoint(
                    adapterID,
                    METRICS_WRITE_SUCCESS, 1.0,
                    MetricUnits.COUNT,
                    metricDimensions),
            )
        }

    }

    override suspend fun close() {
        try {
            monitorTask.cancel()
            writerTask.cancel()
            targetWriterScope.cancel()
        } catch (e: Exception) {
            if (!e.isJobCancellationException) {
                logger.getCtxErrorLogEx(className, "close")("Error closing ${className}r", e)
            }
        }
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
            //  val opcuaTargetConfigReader = OpcuaTargetConfigReader(configReader)
            val config: OpcuaWriterConfiguration = readConfig(configReader)

            // Obtain configuration for used target
            val opcuaTargetConfig = config.targets[targetID]
                    ?: throw TargetException("Configuration for $OPCUA_WRITER_TARGET type target with ID \"$targetID\" does not exist, existing targets are ${config.targets.keys}")
            return try {

                OpcuaTargetWriter(
                    configReader = configReader,
                    targetID = targetID,
                    targetConfig = opcuaTargetConfig,
                    logger = logger,
                    resultHandler = resultHandler
                )
            } catch (e: Throwable) {
                throw TargetException("Error creating $OPCUA_WRITER_TARGET target for target \"$targetID\", $e")
            }
        }

        private fun readConfig(configReader: ConfigReader): OpcuaWriterConfiguration {
            return try {
                configReader.getConfig()
            } catch (e: JsonSyntaxException) {
                throw TargetException("Could not load OPCUA Write Target configuration, JSON syntax error, ${e.extendedJsonException(configReader.jsonConfig)}")
            } catch (e: Exception) {
                throw TargetException("Could not load OPCUA Write Target configuration: $e")
            }
        }

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
        )

    }
}