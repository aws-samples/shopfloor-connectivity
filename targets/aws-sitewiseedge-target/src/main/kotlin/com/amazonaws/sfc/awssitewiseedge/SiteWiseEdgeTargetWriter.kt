// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awssitewiseedge

import com.amazonaws.sfc.awssitewiseedge.config.SiteWiseEdgeTargetConfiguration
import com.amazonaws.sfc.awssitewiseedge.config.SiteWiseEdgeWriterConfiguration
import com.amazonaws.sfc.awssitewiseedge.config.SiteWiseEdgeWriterConfiguration.Companion.AWS_SITEWISEEDGE_TARGET
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.JsonHelper.Companion.extendedJsonException
import com.amazonaws.sfc.data.TargetData
import com.amazonaws.sfc.data.TargetResultBufferedHelper
import com.amazonaws.sfc.data.TargetResultHandler
import com.amazonaws.sfc.data.TargetWriter
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
import com.amazonaws.sfc.mqtt.MqttHelper
import com.amazonaws.sfc.targets.TargetDataChannel
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.*
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import org.eclipse.paho.client.mqttv3.MqttClient
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.measureTime


class SiteWiseEdgeTargetWriter(
    private val targetID: String,
    private val configReader: ConfigReader,
    private val targetConfig: SiteWiseEdgeTargetConfiguration,
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

    private val tqvDataBuffer = TqvDataBuffer(targetID, targetConfig.topicName, logger)

    private var _mqttClient: MqttClient? = null
    private suspend fun getClient(context: CoroutineContext): MqttClient {

        val log = logger.getCtxLoggers(className, "getClient")

        var retries = 0
        while (_mqttClient == null && context.isActive && retries < targetConfig.connectRetries) {
            try {
                val mqttHelper = MqttHelper(targetConfig.mqttConnectionOptions, logger)
                _mqttClient = mqttHelper.buildClient(targetConfig.clientName)
            } catch (e: Exception) {
                logger.getCtxErrorLogEx(className, "mqttClient")("Error creating and connecting mqttClient", e)
            }
            if (_mqttClient == null) {
                log.info("Waiting ${targetConfig.waitAfterConnectError} before trying to create and connecting MQTT client")
                delay(targetConfig.waitAfterConnectError)
                retries++
            }
        }
        return _mqttClient as MqttClient
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
            writer.cancel()
            _mqttClient?.disconnect()
            _mqttClient?.close()
        } catch (e: Exception) {
            if (!e.isJobCancellationException) {
                logger.getCtxErrorLogEx(className, "close")("Error closing writer", e)
            }
        }
    }


    private val targetResults = if (resultHandler != null) TargetResultBufferedHelper(targetID, resultHandler, logger) else null
    private val config: SiteWiseEdgeWriterConfiguration by lazy { configReader.getConfig() }
    private val scope = buildScope("MQTT Target")


    // channel to pass data to the coroutine that publishes the data to the topic
    private val targetDataChannel = TargetDataChannel.create(targetConfig, "$className:targetDataChannel")

    // Coroutine publishing the messages to the target topic
    private val writer = scope.launch(context = Dispatchers.IO, name = "Writer") {
        val log = logger.getCtxLoggers(className, "writer")

        var timer = createTimer()

        log.info("MQTT Writer for target \"$targetID\" writer publishing to topic \"${targetConfig.topicName}\" at endpoint ${targetConfig.endPoint} on target $targetID")
        while (isActive) {
            try {
                select {

                    targetDataChannel.channel.onReceive { targetData ->

                        targetResults?.add(targetData)
                        tqvDataBuffer.add(targetData)

                        log.trace("Received message, buffered size is ${tqvDataBuffer.payloadSize} in ${tqvDataBuffer.size} TQV messages")

                        writeTqvMessages(targetConfig.batchCount, targetConfig.batchSize)
                    }

                    timer.onJoin {
                        log.trace("${targetConfig.batchInterval} batch interval reached")
                        if (timer.isActive) timer.cancel()
                        writeTqvMessages()
                        timer = createTimer()

                    }
                }
            } catch (e: Exception) {
                if (!e.isJobCancellationException)
                    log.errorEx("Error in writer", e)
            }
        }
        if (timer.isActive) timer.cancel()
        writeTqvMessages()
    }

    private fun createTimer(): Job {
        return scope.launch {
            try {
                delay(targetConfig.batchInterval)
            } catch (e: Exception) {
                // no harm done, timer is just used to guard for timeouts
            }
        }
    }

    private suspend fun writeTqvMessages(tqvCount: Int = 0, messageSize: Int = 0) {
        val log = logger.getCtxLoggers(className, "writeTQVs")

        val tqvMessages = tqvDataBuffer.pop(tqvCount, messageSize)
        tqvMessages.forEach { tqvMessage ->
            try {
                val mqttMessage = tqvMessage.mqttPayload()

                val duration = measureTime {
                    val client = runBlocking {
                        getClient(coroutineContext)
                    }

                    withTimeout(targetConfig.publishTimeout) {
                        client.publish(tqvMessage.topic, tqvMessage.mqttPayload())
                    }

                    targetResults?.ackBuffered()
                }
                log.trace("Published MQTT message to \"topic\" \"${targetConfig.topicName}\" with size of ${mqttMessage.payload.size.byteCountString} containing ${tqvMessage.tqvCount} TQVs in $duration")

                createMetrics(targetID, metricDimensions, mqttMessage.payload.size, duration)

            } catch (e: Exception) {
                runBlocking { metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions) }
                log.errorEx("Error publishing to topic \"${targetConfig.topicName}\" for target \"$targetID\", ${e.message}", e)
                if (e is TimeoutCancellationException || _mqttClient == null) {
                    targetResults?.nackBuffered()
                } else {
                    targetResults?.errorBuffered()
                }
            }
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
                    runBlocking {
                        val dataPoints = metricsList.map { MetricsDataPoint(it.metricsName, metricDimensions, it.metricUnit, it.metricsValue) }
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

    private fun createMetrics(
        adapterID: String,
        metricDimensions: MetricDimensions,
        payloadSize: Int,
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
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_BYTES_SEND, payloadSize.toDouble(), MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_MESSAGES, tqvDataBuffer.size.toDouble(), MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(
                    adapterID,
                    METRICS_WRITE_DURATION,
                    duration.inWholeMilliseconds.toDouble(),
                    MetricUnits.MILLISECONDS,
                    metricDimensions
                ),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SIZE, tqvDataBuffer.payloadSize.toDouble(), MetricUnits.BYTES, metricDimensions)
            )
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
            val config: SiteWiseEdgeWriterConfiguration = readConfig(configReader)

            // Obtain configuration for used target
            val mqttConfig = config.targets[targetID]
                ?: throw TargetException("Configuration for $AWS_SITEWISEEDGE_TARGET type target with ID \"$targetID\" does not exist, existing targets are ${config.targets.keys}")
            return try {
                SiteWiseEdgeTargetWriter(
                    configReader = configReader,
                    targetID = targetID,
                    targetConfig = mqttConfig,
                    logger = logger,
                    resultHandler = resultHandler
                )
            } catch (e: Throwable) {
                throw TargetException("Error creating $AWS_SITEWISEEDGE_TARGET target for target \"$targetID\", $e")
            }
        }


        private fun readConfig(configReader: ConfigReader): SiteWiseEdgeWriterConfiguration {
            return try {
                configReader.getConfig()
            } catch (e: JsonSyntaxException) {
                throw TargetException("Could not load MQTT Target configuration, JSON syntax error, ${e.extendedJsonException(configReader.jsonConfig)}")
            } catch (e: Exception) {
                throw TargetException("Could not load MQTT Target configuration: $e")
            }
        }

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
        )

    }


}