// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.mqtt

import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.*
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
import com.amazonaws.sfc.mqtt.config.MqttTargetConfiguration
import com.amazonaws.sfc.mqtt.config.MqttWriterConfiguration
import com.amazonaws.sfc.mqtt.config.MqttWriterConfiguration.Companion.MQTT_TARGET
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class MqttTargetWriter(
    private val targetID: String,
    private val configReader: ConfigReader,
    private val targetConfig: MqttTargetConfiguration,
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

    private var _mqttClient: MqttClient? = null
    private suspend fun getClient(context: CoroutineContext): MqttClient {

        val log = logger.getCtxLoggers(className, "getClient")

        var retries = 0
        while (_mqttClient == null && context.isActive &&  retries < targetConfig.connectRetries) {
            try {
                val mqttHelper = MqttHelper(targetConfig.mqttConnectionOptions, logger)
                _mqttClient = mqttHelper.buildClient("${className}_${targetID}_${getHostName()}")
            } catch (e: Exception) {
                logger.getCtxErrorLog(className, "mqttClient")("Error creating and connecting mqttClient. $e")
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
        targetDataChannel.send(targetData)
    }

    /**
     * Closes the writer
     */
    override suspend fun close() {
        targetDataChannel.close()
        writer.cancel()
        _mqttClient?.close()
    }


    private val targetResults = if (resultHandler != null) TargetResultHelper(targetID, resultHandler, logger) else null
    private val config: MqttWriterConfiguration by lazy { configReader.getConfig() }
    private val scope = buildScope("MQTT Target")

    // channel to pass data to the coroutine that publishes the data to the topic
    private val targetDataChannel = Channel<TargetData>(500)

    // Coroutine publishing the messages to the target topic
    private val writer = scope.launch(context = Dispatchers.IO, name = "Writer") {
        val log = logger.getCtxLoggers(className, "writer")
        try {
            runWriter()
        } catch (e: Exception) {
            if (e.isJobCancellationException)
                log.info("Writer stopped")
            else
                log.error("Error in writer, $e")
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
                    logger.getCtxErrorLog(this::class.java.simpleName, "collectMetricsFromLogger")("Error collecting metrics from logger, $e")
                }
            }
        } else null

    override val metricsProvider: MetricsProvider? by lazy {
        if (metricsCollector != null) InProcessMetricsProvider(metricsCollector!!, logger) else null
    }

    // Publish messages to the target topic
    private suspend fun runWriter() {
        val log = logger.getCtxLoggers(className, "runWriter")

        log.info("MQTT Writer for target \"$targetID\" writer publishing to topic \"${targetConfig.topicName}\" at endpoint ${targetConfig.endPoint} on target $targetID")

        // get received data to write from channel and process/transit
        for (targetData in targetDataChannel) {
            sendTargetData(targetData, log)
        }
    }

    private val transformation by lazy { if (targetConfig.template != null) OutputTransformation(targetConfig.template!!, logger) else null }

    private fun buildPayload(targetData: TargetData): String =
        if (transformation == null) targetData.toJson(config.elementNames) else transformation!!.transform(targetData, config.elementNames) ?: ""


    // Build message payload and publishes message
    private suspend fun sendTargetData(
        targetData: TargetData, log: Logger.ContextLogger
    ) {
        val topicName = targetConfig.topicName ?: targetData.schedule
        log.trace("Sending data to topic on target $targetID\"$topicName\", ${targetData}")


        val start = DateTime.systemDateTime().toEpochMilli()

        try {
            val client = getClient(coroutineContext)
            val message = buildMqttMessage(targetData)
            withTimeout(targetConfig.publishTimeout) {
                client.publish(topicName, message)
            }
            targetResults?.ack(targetData)
            val writeDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()
            createMetrics(targetID, metricDimensions, message.payload.size, writeDurationInMillis)

        } catch (e: Throwable) {
            val message = "Error publishing to topic \"$topicName\" for target \"$targetID\", ${e.message}"
            runBlocking { metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions) }
            log.error(message)
            if (e is TimeoutCancellationException || _mqttClient == null) {
                targetResults?.nack(targetData)
            } else {
                targetResults?.error(targetData)
            }
            throw TargetException(message)
        }
    }

    private fun buildMqttMessage(targetData: TargetData): MqttMessage {
        val message = MqttMessage()
        val payload = buildPayload(targetData)
        message.payload = payload.toByteArray()
        message.qos = targetConfig.qos
        return message
    }

    private fun createMetrics(
        adapterID: String,
        metricDimensions: MetricDimensions,
        size: Int,
        writeDurationInMillis: Double
    ) {

        runBlocking {
            metricsCollector?.put(
                adapterID,
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITES, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_MESSAGES, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(
                    adapterID,
                    METRICS_WRITE_DURATION,
                    writeDurationInMillis,
                    MetricUnits.MILLISECONDS,
                    metricDimensions
                ),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SIZE, size.toDouble(), MetricUnits.BYTES, metricDimensions)
            )
        }

    }

    companion object {

        private val className = this::class.java.simpleName

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
            val config: MqttWriterConfiguration = readConfig(configReader)

            // Obtain configuration for used target
            val mqttConfig = config.targets[targetID]
                ?: throw TargetException("Configuration for $MQTT_TARGET type target with ID \"$targetID\" does not exist, existing targets are ${config.targets.keys}")
            return try {
                MqttTargetWriter(
                    configReader = configReader,
                    targetID = targetID,
                    targetConfig = mqttConfig,
                    logger = logger,
                    resultHandler = resultHandler
                )
            } catch (e: Throwable) {
                throw TargetException("Error creating $MQTT_TARGET target for target \"$targetID\", $e")
            }
        }


        private fun readConfig(configReader: ConfigReader): MqttWriterConfiguration {
            return try {
                configReader.getConfig()
            } catch (e: Exception) {
                throw TargetException("Could not load MQTT Target configuration: $e")
            }
        }

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
        )

    }


}