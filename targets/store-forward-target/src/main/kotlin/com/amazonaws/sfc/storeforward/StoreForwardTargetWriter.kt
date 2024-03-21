// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.storeforward

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
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_MESSAGE_BUFFERED_COUNT
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_MESSAGE_BUFFERED_SIZE
import com.amazonaws.sfc.storeforward.config.StoreForwardWriterConfiguration
import com.amazonaws.sfc.targets.ForwardingTargetWriter
import com.amazonaws.sfc.targets.TargetWriterFactory
import com.amazonaws.sfc.util.MemoryMonitor.Companion.getUsedMemoryMB
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


class StoreForwardTargetWriter(
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
    ), TargetWriter {

    private val className = this::class.simpleName ?: ""

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val metricDimensions = mapOf(
        MetricsCollector.METRICS_DIMENSION_SOURCE to targetID,
        MetricsCollector.METRICS_DIMENSION_TYPE to className
    )

    private val writerScope by lazy { buildScope("StoreForwardTargetWriter") }

    private val storeAndForwardWriterConfiguration by lazy {
        configReader.getConfig<StoreForwardWriterConfiguration>()
    }

    private val tuningConfiguration : TuningConfiguration
        get() =
            configReader.getConfig<ConfigWithTuningConfiguration>().tuningConfiguration


    private val forwardingTargetConfiguration by lazy {
        storeAndForwardWriterConfiguration.forwardingTargets[targetID]
            ?: throw Exception("Configuration for target \"$targetID\" does not exist, configured targets are ${storeAndForwardWriterConfiguration.forwardingTargets.keys}")
    }

    private val isAnyTargetProducingMetricsData
        get() = storeAndForwardWriterConfiguration.forwardedTargets.values.any { it.metrics.enabled }

    private val messageBuffer by lazy { runBlocking { FileSystemMessageBuffer(getTargets().keys, forwardingTargetConfiguration, logger) } }

    private val metricsCollector: MetricsCollector? by lazy {

        val forwardersMetricsConfigurations = storeAndForwardWriterConfiguration.forwardedTargets.map { it.key to it.value.metrics }
        val forwardingMetricsConfiguration =
            targetID to (storeAndForwardWriterConfiguration.forwardingTargets[targetID]?.metrics ?: MetricsSourceConfiguration())

        val metricsConfigurations = (forwardersMetricsConfigurations + forwardingMetricsConfiguration).toMap()

        if (storeAndForwardWriterConfiguration.isCollectingMetrics || isAnyTargetProducingMetricsData) {

            logger.metricsCollectorMethod = collectMetricsFromLogger
            MetricsCollector(
                metricsConfig = storeAndForwardWriterConfiguration.metrics,
                metricsSourceType = MetricsSourceType.TARGET_WRITER,
                metricsSourceConfigurations = metricsConfigurations,
                staticDimensions = TARGET_METRIC_DIMENSIONS,
                logger = logger
            )
        } else null
    }

    private val collectMetricsFromLogger: MetricsCollectorMethod? =
        if (storeAndForwardWriterConfiguration.isCollectingMetrics) {
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

    // keeps track of buffering state of buffered writers for each target
    private val targetBufferModeChanged = { _: String, mode: BufferedWriter.WriterMode ->
        val newBufferingMode = ((mode == BufferedWriter.WriterMode.BUFFERING) || isAnyTargetInBufferingState)
        if (newBufferingMode != targetBufferingState) {
            targetBufferingState = newBufferingMode
            runBlocking {
                bufferingModeChannel.send(targetBufferingState)
            }
        }
    }

    private var _targetForwardWriters: Map<String, BufferedWriter>? = null

    // Mapping of channels that serve as input for coroutines forwarding messages to the targets
    private val targetDataForwardWriters: Map<String, BufferedWriter>
        get() {
            if (_targetForwardWriters == null) {
                runBlocking {
                    _targetForwardWriters = getTargets().map { (targetID, targetWriter) ->
                        targetID to BufferedWriter(
                            targetID = targetID,
                            targetScope = targetScope,
                            writer = targetWriter,
                            buffer = messageBuffer,
                            bufferConfiguration = forwardingTargetConfiguration,
                            metricsCollectorMethod = collectMetricsFromBuffer,
                            onModeChanged = targetBufferModeChanged,
                            tuningConfiguration = tuningConfiguration,
                            logger = logger
                        )
                    }.toMap()
                }
            }
            return _targetForwardWriters ?: emptyMap()
        }


    override val metricsProvider: MetricsProvider? by lazy {
        if (metricsCollector != null) InProcessMetricsProvider(metricsCollector!!, logger) else null
    }


    override suspend fun forwardTargetData(targetData: TargetData) {

        targetDataForwardWriters.forEach {
            it.value.write(targetData)
        }
        createMetrics(targetID)
    }


    private val collectMetricsFromBuffer: MetricsCollectorMethod? =
        if (metricsCollector != null) {
            { metricsList ->
                try {
                    val dataPoints = metricsList.map { MetricsDataPoint(it.metricsName, metricDimensions, it.metricUnit, it.metricsValue) }
                    runBlocking {
                        metricsCollector?.put(targetID, dataPoints)
                    }
                } catch (e: java.lang.Exception) {
                    logger.getCtxErrorLogEx(this::class.java.simpleName, "collectMetricsFromBuffer")("Error collecting metrics from buffer", e)
                }
            }
        } else null


    private val isAnyTargetInBufferingState
        get() = targetDataForwardWriters.any { it.value.isBuffering }

    private var targetBufferingState: Boolean = targetDataForwardWriters.any { it.value.isBuffering }
    private var bufferingModeChannel = Channel<Boolean>(forwardingTargetConfiguration.subTargets?.size ?: 10)


    private val bufferMetricsCollector = if (storeAndForwardWriterConfiguration.isCollectingMetrics) {
        writerScope.launch(Dispatchers.IO) {

            fun intervalForBufferingMode(count: Long) = if (targetBufferingState || count > 0) {
                INTERVAL_BUFFER_METRICS
            } else {
                Duration.INFINITE
            }

            var lastBufferCount = 0L
            var interval = INTERVAL_BUFFER_METRICS

            var timer = timerJob(interval)
            while (isActive) {

                try {
                    select {
                        bufferingModeChannel.onReceive { isBuffering ->
                            timer.cancel()
                            if (isBuffering) {
                                lastBufferCount = createBufferMetrics()
                            }
                            interval = intervalForBufferingMode(lastBufferCount)
                            timer = timerJob(interval)

                        }
                        timer.onJoin {
                            lastBufferCount = createBufferMetrics()
                            interval = intervalForBufferingMode(lastBufferCount)
                            timer = timerJob(interval)
                        }
                    }
                } catch (e: Exception) {
                    logger.getCtxErrorLogEx(className, "bufferMetricsCollector")("Error collecting buffer metrics", e)
                }
            }
        }
    } else null


    private suspend fun createBufferMetrics(): Long {
        val bufferSize = forwardingTargetConfiguration.subTargets?.fold(0L) { acc, t -> acc + messageBuffer.size(t) } ?: 0
        val bufferCount = forwardingTargetConfiguration.subTargets?.fold(0L) { acc, t -> acc + messageBuffer.count(t) } ?: 0
        metricsCollector?.put(
            targetID,
            MetricsDataPoint(METRICS_MEMORY, metricDimensions, MetricUnits.MEGABYTES, MetricsValue(getUsedMemoryMB().toDouble())),
            MetricsDataPoint(METRICS_MESSAGE_BUFFERED_SIZE, metricDimensions, MetricUnits.COUNT, MetricsValue(bufferSize.toDouble())),
            MetricsDataPoint(METRICS_MESSAGE_BUFFERED_COUNT, metricDimensions, MetricUnits.COUNT, MetricsValue(bufferCount.toDouble()))
        )
        return bufferCount
    }

    private fun CoroutineScope.timerJob(duration: Duration) = launch( context = Dispatchers.Default, name = "Timeout Timer") {
        try {
            delay(duration)
        } catch (e: Exception) {
            // No harm done, time s just used to check for timeouts
        }
    }


    private fun createMetrics(adapterID: String) {

        runBlocking {
            metricsCollector?.put(
                adapterID,
                metricsCollector?.buildValueDataPoint(
                    adapterID,
                    METRICS_MESSAGES,
                    targetDataForwardWriters.size.toDouble(),
                    MetricUnits.COUNT,
                    metricDimensions
                )
            )
        }
    }

    override suspend fun writeMetricsData(metricsData: MetricsData) {
        metricsCollector?.put(metricsData.source, metricsData)
    }

    override suspend fun close() {
        targetDataForwardWriters.values.forEach {
            it.stop()
        }
        bufferMetricsCollector?.cancel()
        metricsCollector?.close()
    }

    override fun handleResult(targetResult: TargetResult) {
        targetDataForwardWriters[targetResult.targetID]?.handleResult(targetResult)
    }

    override val returnedData: ResulHandlerReturnedData
        get() {
            return STORE_FORWARD_RETURNED_RESULT_DATA
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
            return try {
                StoreForwardTargetWriter(targetID, configReader, logger, resultHandler)
            } catch (e: Throwable) {
                throw Exception("Error creating  and Forward target writer, ${e.message}")
            }
        }

        const val TIMEOUT_TARGET_FORWARD = 10000L

        val INTERVAL_BUFFER_METRICS = 10.toDuration(DurationUnit.SECONDS)

        val STORE_FORWARD_RETURNED_RESULT_DATA =
            ResulHandlerReturnedData(
                ack = ResulHandlerReturnedData.ResultHandlerData.SERIALS,
                nack = ResulHandlerReturnedData.ResultHandlerData.MESSAGES,
                error = ResulHandlerReturnedData.ResultHandlerData.SERIALS
            )

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
        )

    }

    override fun createTargetWriter(targetID: String, targetConfig: TargetConfiguration): TargetWriter? {


        val log = logger.getCtxLoggers("StoreForwardTargetWriter", "createTargetWriter")

        return when {

            (!targetConfig.active) -> {
                log.info("Target \"$targetID\" is not active")
                null
            }

            // create writer using client to communicate with external target IPC service
            (!targetConfig.server.isNullOrEmpty()) -> {
                log.info("Creating an IPC process writer for target \"$targetID\" on server ${targetConfig.server}")
                IpcTargetWriter.createIpcTargetWriter(configReader, targetID, targetConfig.server!!, logger, this)
            }

            else -> {

                // create in process target writer
                log.info("Creating in process target writer for target ID $targetID")
                val targetWriterFactory = TargetWriterFactory.createTargetWriterFactory(configReader, logger)
                targetWriterFactory?.createInProcessWriter(targetID, logger, this)
            }
        }
    }
}




