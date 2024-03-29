// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.router

import com.amazonaws.sfc.channels.channelSubmitEventHandler
import com.amazonaws.sfc.channels.submit
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.ConfigWithTuningConfiguration
import com.amazonaws.sfc.config.TargetConfiguration
import com.amazonaws.sfc.config.TuningConfiguration
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.ipc.IpcTargetWriter
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_MEMORY
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_MESSAGES
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITES
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_ERRORS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_SUCCESS
import com.amazonaws.sfc.router.config.RouterResultHandlerPolicy
import com.amazonaws.sfc.router.config.RouterWriterConfiguration
import com.amazonaws.sfc.router.config.RoutesConfiguration.Companion.CONFIG_ALTERNATE_TARGET
import com.amazonaws.sfc.router.config.RoutesConfiguration.Companion.CONFIG_SUCCESS_TARGET
import com.amazonaws.sfc.targets.ForwardingTargetWriter
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.targets.TargetWriterFactory
import com.amazonaws.sfc.util.MemoryMonitor
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.isJobCancellationException
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class RouterTargetWriter(
    targetID: String,
    configReader: ConfigReader,
    private val logger: Logger,
    resultHandler: TargetResultHandler?
) :
    ForwardingTargetWriter(
        targetID = targetID,
        configReader = configReader,
        resultHandler = resultHandler,
        logger = logger
    ) {

    private val className = this::class.java.simpleName

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }


    private val metricDimensions = mapOf(
        MetricsCollector.METRICS_DIMENSION_SOURCE to targetID,
        MetricsCollector.METRICS_DIMENSION_TYPE to className
    )

    private val routerWriterConfiguration by lazy {
        configReader.getConfig<RouterWriterConfiguration>()
    }

    private val tuningConfiguration by lazy{
        configReader.getConfig<ConfigWithTuningConfiguration>().tuningConfiguration
    }

    private val routingTargetConfiguration by lazy {
        routerWriterConfiguration.routerTargets[targetID]
            ?: throw Exception("Configuration for target \"$targetID\" does not exist, configured routing targets are ${routerWriterConfiguration.routerTargets.keys}")
    }

    private val primaryTargetKeys by lazy { routingTargetConfiguration.routes.keys }

    private val primaryTargetsConfigurations by lazy {
        routerWriterConfiguration.targets.filter { t -> t.key in routingTargetConfiguration.subTargets }
    }

    private val isAnyPrimaryTargetProducingMetricsData
        get() = primaryTargetsConfigurations.values.any { it.metrics.enabled }

    private val metricsCollector: MetricsCollector? by lazy {
        createMetricsCollector(targetID)
    }

    private val forwardingTargetResultHelper =
        TargetResultHelper(
            targetID = targetID,
            targetResultHandler = resultHandler,
            logger = logger
        )


    override val metricsProvider: MetricsProvider? by lazy {
        if (metricsCollector != null) InProcessMetricsProvider(metricsCollector!!, logger) else null
    }

    private val targetForwardWriters: Map<String, TargetWriter> by lazy {
        runBlocking {
            getTargets()
        }
    }

    // Channel to pass result data from target
    private val resultChannel = Channel<TargetResult>(tuningConfiguration.targetResultsChannelSize)


    // Worker to process received results from target
    private val resultWorker = launchResultHandlerWorker(resultChannel)

    override val returnedData: ResulHandlerReturnedData? by lazy {

        val routes = routingTargetConfiguration.routes.values

        val alternateRoutes = routes.mapNotNull { it.alternateTargetID }
        val hasAlternateRoutes = alternateRoutes.any { it.isNotEmpty() }

        val successRoutes = routes.mapNotNull { it.successTargetID }
        val hasSuccessRoutes = successRoutes.any { it.isNotEmpty() }

        fun returnedData(hasSuccessRoutes: Boolean) = if (hasSuccessRoutes)
            ResulHandlerReturnedData.ResultHandlerData.MESSAGES
        else ResulHandlerReturnedData.ResultHandlerData.NONE

        if (hasAlternateRoutes || hasSuccessRoutes) {
            val ack = returnedData(hasSuccessRoutes)
            val err = returnedData(hasAlternateRoutes)
            ResulHandlerReturnedData(ack, err, err)
        } else {
            null
        }
    }


    override fun createTargetWriter(targetID: String, targetConfig: TargetConfiguration): TargetWriter? {

        val log = logger.getCtxLoggers(className, "createTargetWriter")

        // only primary targets require results
        val subTargetHandler = if (targetID in primaryTargetKeys) this else null

        return when {

            (!targetConfig.active) -> {
                log.info("Target \"$targetID\" is not active")
                null
            }

            // create writer using client to communicate with external target IPC service
            (!targetConfig.server.isNullOrEmpty()) -> {
                log.info("Creating an IPC process writer to forward data from target ${this.targetID} to target $targetID, server ${targetConfig.server}")
                IpcTargetWriter.createIpcTargetWriter(configReader, targetID, targetConfig.server!!, logger, subTargetHandler)
            }

            else -> {
                // create in process target writer
                log.info("Creating in process target writer to forward data from target ${this.targetID} to target ID $targetID")
                val targetWriterFactory = TargetWriterFactory.createTargetWriterFactory(configReader, logger)
                targetWriterFactory?.createInProcessWriter(targetID, logger, subTargetHandler)
            }
        }
    }

    private fun createMetricsCollector(targetID: String): MetricsCollector? {
        val forwardersMetricsConfigurations = primaryTargetsConfigurations.map { it.key to it.value.metrics }
        val forwardingMetricsConfiguration = targetID to (routingTargetConfiguration.metrics)

        val metricsConfigurations = (forwardersMetricsConfigurations + forwardingMetricsConfiguration).toMap()

        return if (routerWriterConfiguration.isCollectingMetrics || isAnyPrimaryTargetProducingMetricsData) {

            logger.metricsCollectorMethod = collectMetricsFromLoggerMethod
            MetricsCollector(
                metricsConfig = routerWriterConfiguration.metrics,
                metricsSourceType = MetricsSourceType.TARGET_WRITER,
                metricsSourceConfigurations = metricsConfigurations,
                staticDimensions = TARGET_METRIC_DIMENSIONS,
                logger = logger
            )
        } else null
    }

    private val collectMetricsFromLoggerMethod: MetricsCollectorMethod? =
        if (routerWriterConfiguration.isCollectingMetrics) {
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

    private suspend fun forwardTargetData(forwardTargetID: String, alternateTargetID: String?, targetData: TargetData, routeType: String = "Primary"): Boolean {

        val log = logger.getCtxLoggers(className, "forwardTargetDate")

        log.trace("Target \"${targetID}\" routing message ${targetData.serial} to $routeType route target \"$forwardTargetID\"")

        if (forwardTargetData(forwardTargetID, targetData)) return true

        if (!alternateTargetID.isNullOrEmpty()) {
            if (forwardTargetData(alternateTargetID, null, targetData, CONFIG_ALTERNATE_TARGET)) return true
        }
        return false
    }

    // this is the first forwarding step to the primary targets, with the option to forward to alternate route if this fails
    override suspend fun forwardTargetData(targetData: TargetData) {

        val scope = buildScope("ForwardData")
        val perPrimaryTargetForwardingResults = routingTargetConfiguration.routes.map { (primaryTargetID, routes) ->
            scope.async { forwardTargetData(primaryTargetID, routes.alternateTargetID, targetData) }
        }.map { it.await() }

        val totalForwardResult = when (routingTargetConfiguration.resultHandlerPolicy) {
            RouterResultHandlerPolicy.ALL_TARGETS -> perPrimaryTargetForwardingResults.all { it }
            RouterResultHandlerPolicy.ANY_TARGET -> perPrimaryTargetForwardingResults.any { it }
        }
        if (totalForwardResult) forwardingTargetResultHelper.ack(targetData) else forwardingTargetResultHelper.error(targetData)
    }

    // Start handling results from primary and secondary targets
    private fun launchResultHandlerWorker(channel : Channel<TargetResult>) = targetScope.launch("Result processor for target $targetID") {

        while (isActive) {
            try {
                val resultData = channel.receive()
                handleTargetResults(resultData)
            } catch (e: Exception) {
                logger.getCtxErrorLogEx(className, "launchResultHandlerWorker")("Error handling result", e)
            }
        }
    }

    private suspend fun handleTargetResults(resultData: TargetResult) {
        val log = logger.getCtxLoggers(className, "handleTargetResults")

        val resultTargetID = resultData.targetID

        if (resultTargetID in primaryTargetKeys) {
            log.trace("Target \"$targetID\" handling target results from target \"$resultTargetID\", ack:${resultData.ackSerials.size}, nack:${resultData.nackSerials?.size}, error:${resultData.errorSerials?.size}")

            val routesForPrimaryTarget = routingTargetConfiguration.routes[resultTargetID]

            val scope = buildScope("handleTargetResults")
            listOf(
                scope.launch {
                    try {
                        handleSuccessResults(routesForPrimaryTarget?.successTargetID, resultData)
                    } catch (e: Exception) {
                        logger.getCtxErrorLogEx(className, "handleTargetResults")("Error handling success results", e)
                    }
                },
                scope.launch {
                    try {
                        handleErrorResults(routesForPrimaryTarget?.alternateTargetID, resultData)
                    } catch (e: Exception) {
                        logger.getCtxErrorLogEx(className, "handleTargetResults")("Error handling error results", e)
                    }
                }
            ).joinAll()
        }
    }

    private suspend fun handleSuccessResults(successTargetID: String?, resultData: TargetResult) {

        if (successTargetID.isNullOrEmpty()) return
        resultData.ackMessageList?.forEach { targetData ->
            forwardTargetData(successTargetID, null, targetData, CONFIG_SUCCESS_TARGET)
        }
    }

    private suspend fun RouterTargetWriter.handleErrorResults(alternateTargetID: String?, resultData: TargetResult) {
        if (alternateTargetID.isNullOrEmpty()) return
        val failed = (resultData.nackMessageList ?: emptyList()).plus(resultData.errorMessageList ?: emptyList())
        failed.forEach { targetData ->
            forwardTargetData(alternateTargetID, null, targetData, CONFIG_ALTERNATE_TARGET)
        }
    }


    private suspend fun forwardTargetData(targetID: String, targetData: TargetData): Boolean {

        val one = MetricsValue(1)
        val metrics = mutableListOf(
            MetricsValueParam(METRICS_WRITES, one, MetricUnits.COUNT),
            MetricsValueParam(METRICS_MESSAGES, one, MetricUnits.COUNT),
            MetricsValueParam(METRICS_MEMORY, MetricsValue(MemoryMonitor.getUsedMemoryMB().toDouble()),MetricUnits.MEGABYTES)
        )

        return if (writeDataToTarget(targetID, targetData)) {
            metrics.add(MetricsValueParam(METRICS_WRITE_SUCCESS, one, MetricUnits.COUNT))
            metricsCollector?.put(targetID, metrics.map { MetricsDataPoint(it.metricsName, metricDimensions, it.metricUnit, it.metricsValue) })
            true
        } else {
            metrics.add(MetricsValueParam(METRICS_WRITE_ERRORS, one, MetricUnits.COUNT))
            metricsCollector?.put(targetID, metrics.map { MetricsDataPoint(it.metricsName, metricDimensions, it.metricUnit, it.metricsValue) })
            false
        }
    }


    // Writes data to a single target
    private suspend fun writeDataToTarget(targetID: String, targetData: TargetData): Boolean = coroutineScope {

        val log = logger.getCtxLoggers(className, "writeDataToTarget")

        log.trace("Writing data with serial ${targetData.serial} to target \"$targetID\"")

        val writer = targetForwardWriters[targetID] ?: return@coroutineScope false

        return@coroutineScope try {
            withTimeout(TIMEOUT_TARGET_FORWARD) {
                if (writer.isInitialized) {
                    writer.writeTargetData(targetData)
                    true
                } else {
                    log.trace("Can not forward to target $targetID as it has not been initialized yet")
                    false
                }
            }
        } catch (t: TimeoutCancellationException) {
            log.trace("Timeout forwarding to target \"${targetID}\"")
            false
        } catch (e: Exception) {
            if (e.isJobCancellationException)
                log.info("Forwarder stopped")
            else
                log.errorEx("Error forwarding to target \"${targetID}\"", e)
            false
        }

    }

    override fun subTargetsToSetup(targetID: String): List<String> = routingTargetConfiguration.subTargets

    override fun handleResult(targetResult: TargetResult) {
        runBlocking {
            resultChannel.submit(targetResult, tuningConfiguration.targetResultsChannelTimeout) { event ->
                channelSubmitEventHandler(
                    event = event,
                    channelName = "$className:resultChannel",
                    tuningChannelSizeName = TuningConfiguration.CONFIG_TARGET_RESULTS_CHANNEL_SIZE,
                    currentChannelSize = tuningConfiguration.targetResultsChannelSize,
                    tuningChannelTimeoutName = TuningConfiguration.CONFIG_TARGET_RESULTS_CHANNEL_TIMEOUT,
                    log = logger.getCtxLoggers(className, "handleResult")
                )
            }
        }
    }

    override suspend fun writeMetricsData(metricsData: MetricsData) {
        metricsCollector?.put(metricsData.source, metricsData)
    }

    override suspend fun close() {
        super.close()
        resultWorker.cancel()
        targetForwardWriters.values.forEach { it.close() }
    }

    companion object {
        const val TIMEOUT_TARGET_FORWARD = 10000L

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
        )


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

            return try {
                RouterTargetWriter(targetID, configReader, logger, resultHandler)
            } catch (e: Throwable) {
                throw TargetException("Error creating AWS Router target writer \"$targetID\" , ${e.message}")
            }
        }
    }


}