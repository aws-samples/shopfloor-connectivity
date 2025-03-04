// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.opcuatarget

import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.ElementNamesConfiguration
import com.amazonaws.sfc.data.JsonHelper.Companion.extendedJsonException
import com.amazonaws.sfc.data.TargetData
import com.amazonaws.sfc.data.TargetResultHandler
import com.amazonaws.sfc.data.TargetResultHelper
import com.amazonaws.sfc.data.TargetWriter
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITES
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_DURATION
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_ERRORS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_SUCCESS
import com.amazonaws.sfc.opcuatarget.config.OpcuaTargetConfiguration
import com.amazonaws.sfc.opcuatarget.config.OpcuaWriterConfiguration
import com.amazonaws.sfc.opcuatarget.config.OpcuaWriterConfiguration.Companion.OPCUA_TARGET
import com.amazonaws.sfc.targets.TargetDataChannel
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.MemoryMonitor
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.isJobCancellationException
import com.amazonaws.sfc.util.launch
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilter
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilterContext
import org.eclipse.milo.opcua.stack.core.AttributeId
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import kotlin.time.toDuration

class OpcuaTargetWriter(
    private val targetID: String,
    configReader: ConfigReader,
    private val targetConfig: OpcuaTargetConfiguration,
    private val logger: Logger,
    private val resultHandler: TargetResultHandler?
) : TargetWriter, AttributeFilter {

    private val className = this::class.java.simpleName

    private val modelUpdateChannel = Channel<Unit>()
    private val targetWriterScope = buildScope(className, dispatcher = Dispatchers.IO)

    private val opcuaConfigReader = OpcuaTargetConfigReader(configReader)

    private var readCount: AtomicInteger = AtomicInteger(0)
    private var writeCount: AtomicInteger = AtomicInteger(0)

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val metricDimensions = mapOf(
        METRICS_DIMENSION_SOURCE to targetID,
        MetricsCollector.METRICS_DIMENSION_TYPE to className
    )

    private val opcuaTargetServer: OpcuaTargetServer by lazy {
        OpcuaTargetServer(targetConfig, config.transformations, this, config.elementNames, logger).initialize()
    }

    override suspend fun writeTargetData(targetData: TargetData) {
        targetDataChannel.submit(targetData, logger.getCtxLoggers(className, "writeTargetData"))
    }


    private val targetResults = if (resultHandler != null) TargetResultHelper(targetID, resultHandler, logger) else null
    private val config: OpcuaWriterConfiguration by lazy { opcuaConfigReader.getConfig() }

    private val targetDataChannel = TargetDataChannel.create(targetConfig, "$className:targetDataChannel")

    private val modelChangedEventTask = targetWriterScope.launch(context = Dispatchers.IO, name = "ModelChanged") { modelChangedEvent() }

    private val monitorTask = targetWriterScope.launch(context = Dispatchers.IO, name = "Monitor") { monitor() }

    private val writerTask = targetWriterScope.launch(context = Dispatchers.IO, name = "Writer") { writer() }


    private suspend fun CoroutineScope.monitor() {

        while (isActive) {

           val interval = 60.toDuration(DurationUnit.SECONDS)
            delay(interval)
            logger.getCtxInfoLog(className, "monitor")("OPCUA server: ${readCount.get()} reads and  ${writeCount.get()} writes over the last $interval")


            if (metricsCollector != null) {
                metricsCollector?.put(
                    targetID,
                    metricsCollector?.buildValueDataPoint(
                        targetID,
                        MetricsCollector.METRICS_VALUES_READ,
                        readCount.get().toDouble(),
                        MetricUnits.COUNT
                    ))
            }
            readCount.set(0)
            writeCount.set(0)
        }
    }

    private suspend fun CoroutineScope.writer() {
        val log = logger.getCtxLoggers(className, "writer")

        opcuaTargetServer.startup()

        if (opcuaTargetServer.opcuaServer == null) {
            throw TargetException("Unable to start OPC UA server")
        }

        if (!checkServerBoundEndpoints()) throw TargetException("No endpoints bound for OPC UA server")
        displayServerBoundEndpoints()

        while (isActive) {
            try {
                select {

                    targetDataChannel.channel.onReceive { targetData ->
                        log.trace("Received target data ${targetData.toJson(ElementNamesConfiguration(), true, pretty = false)}")
                        try {
                            val duration = measureTime {
                                opcuaTargetServer.writeTargetData(targetData)
                            }
                            createMetrics(targetID, metricDimensions, duration)
                            targetResults?.ack(targetData)

                        } catch (e: Exception) {
                            runBlocking { metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions) }
                            if (logger.level == LogLevel.TRACE){
                                log.traceEx("Error writing target data", e)
                            } else {
                                log.error("Error writing target data, $e")
                            }
                            targetResults?.nack(targetData)
                        }

                    }


                }
            } catch (e: Exception) {
                if (!e.isJobCancellationException)
                    log.errorEx("Error in writer", e)
            }
        }

        opcuaTargetServer.shutdown()
    }

    private suspend fun CoroutineScope.modelChangedEvent() {
        while (isActive) {
            modelUpdateChannel.receive()
            opcuaTargetServer.raiseDataModelChangedEvent()
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

    private fun displayServerBoundEndpoints() {
        val log = logger.getCtxLoggers(className, "displayBoundEndpoints")
        log.info("OPCUA server endpoints:")
        opcuaTargetServer.opcuaServer!!.stackServer.boundEndpoints.forEach {
            log.info("URL: ${it.endpointUrl}, Security mode: ${it.securityMode}, Security policy: ${it.securityPolicy}")
        }
    }

    private fun checkServerBoundEndpoints(): Boolean {
        val log = logger.getCtxLoggers(className, "checkBoundEndpoints")
        if (opcuaTargetServer.opcuaServer!!.stackServer.boundEndpoints.isEmpty()) {
            return false
        }
        if (opcuaTargetServer.opcuaServer!!.stackServer.boundEndpoints.count() != opcuaTargetServer.opcuaServer!!.stackServer.config.endpoints.count()) {
            opcuaTargetServer.opcuaServer!!.stackServer.config.endpoints.forEach { e ->
                if (opcuaTargetServer.opcuaServer!!.stackServer.boundEndpoints.find { it == e } == null) {
                    log.warning("Endpoint ${e.endpointUrl}, Security mode: ${e.securityMode}, Security policy: ${e.securityPolicy} is not bound")
                }
            }
        }
        return true
    }


    private fun createMetrics(
        adapterID: String,
        metricDimensions: MetricDimensions,
        duration: Duration
    ) {

        runBlocking {
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
                metricsCollector?.buildValueDataPoint(
                    adapterID,
                    METRICS_WRITE_DURATION,
                    duration.inWholeMilliseconds.toDouble(),
                    MetricUnits.MILLISECONDS,
                    metricDimensions
                ),
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
            modelChangedEventTask.cancel()
            targetWriterScope.cancel()
            opcuaTargetServer.shutdown()
        } catch (e: Exception) {
            if (!e.isJobCancellationException) {
                logger.getCtxErrorLogEx(className, "close")("Error closing ${className}r", e)
            }
        }
    }

    override fun getAttribute(ctx: AttributeFilterContext.GetAttributeContext, attributeId: AttributeId): Any? {

        val value = ctx.getAttribute(attributeId)
        // only log external reads
        if (value is DataValue && ctx.session.isPresent) {

            readCount.addAndGet(1)

            if (logger.level == LogLevel.TRACE && ctx.session.isPresent) {
                val session = ctx.session.get()
                logger.getCtxTraceLog(className, "getAttribute")(
                    "Reading value ${value.valueStr}${value.valueTypeStr} from node ${ctx.node.nodeId.toParseableString()}, " +
                            "client session:${session.sessionName}, " +
                            "address:${session.clientAddress}, " +
                            "endpoint:${session.endpoint.endpointUrl}, " +
                            "application:${session.clientDescription.applicationName.text}, " +
                            "identity policy:${session.identityToken!!.policyId}")
            }
        }
        return value
    }

    override fun setAttribute(ctx: AttributeFilterContext.SetAttributeContext, attributeId: AttributeId, value: Any?) {
        if (value is DataValue) {

            writeCount.addAndGet(1)

            if (logger.level == LogLevel.TRACE && value.value != null) {
                logger.getCtxTraceLog(
                    className,
                    "getAttribute")("Writing value ${value.valueStr}${value.valueTypeStr} to node ${ctx.node.nodeId.toParseableString()}")
            }
            ctx.setAttribute(attributeId, value)
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
            val opcuaTargetConfigReader = OpcuaTargetConfigReader(configReader)
            val config: OpcuaWriterConfiguration = readConfig(opcuaTargetConfigReader)

            // Obtain configuration for used target
            val opcuaTargetConfig = config.targets[targetID]
                    ?: throw TargetException("Configuration for $OPCUA_TARGET type target with ID \"$targetID\" does not exist, existing targets are ${config.targets.keys}")
            return try {

                OpcuaTargetWriter(
                    configReader = opcuaTargetConfigReader,
                    targetID = targetID,
                    targetConfig = opcuaTargetConfig,
                    logger = logger,
                    resultHandler = resultHandler
                )
            } catch (e: Throwable) {
                throw TargetException("Error creating $OPCUA_TARGET target for target \"$targetID\", $e")
            }
        }


        private fun readConfig(configReader: OpcuaTargetConfigReader): OpcuaWriterConfiguration {
            return try {
                configReader.getConfig()
            } catch (e: JsonSyntaxException) {
                throw TargetException("Could not load OPCUA Target configuration, JSON syntax error, ${e.extendedJsonException(configReader.jsonConfig)}")
            } catch (e: Exception) {
                throw TargetException("Could not load OPCUA Target configuration: $e")
            }
        }

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
        )

    }

    private val DataValue.valueStr
    get() =  when {
        this.value.value == null -> "null"
        (this.value.value is Array<*>) -> (this.value.value as Array<*>).joinToString(prefix = "[", postfix = "]", separator = ",") { it.toString() }
        else -> this.value.value.toString()
    }

    private val DataValue.valueTypeStr
        get() = when {
            this.value.value == null -> ""
            this.value.value is Array<*> &&
                    (this.value.value as Array<*>).isNotEmpty()  &&
                    ((this.value.value as Array<*>).first() != null) -> ":[${(this.value.value as Array<*>).first()!!::class.java.simpleName?:""}]"
            this.value.value is List<*> &&
                    (this.value.value as List<*>).isNotEmpty()  &&
                    ((this.value.value as List<*>).first() != null) -> ":[${(this.value.value as List<*>).first()!!::class.java.simpleName}]"
            else -> ":${this.value.value::class.java.simpleName}"
        }
}