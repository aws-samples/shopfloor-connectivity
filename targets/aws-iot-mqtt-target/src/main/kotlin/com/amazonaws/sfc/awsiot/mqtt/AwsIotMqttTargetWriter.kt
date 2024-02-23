
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsiot.mqtt

import com.amazonaws.sfc.awsiot.mqtt.config.AwsIotTargetConfiguration
import com.amazonaws.sfc.awsiot.mqtt.config.AwsIotWriterConfiguration
import com.amazonaws.sfc.awsiot.mqtt.config.AwsIotWriterConfiguration.Companion.AWS_IOT_MQTT_TARGET
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
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.canNotReachAwsService
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.absoluteValue
import kotlin.random.Random


/**
 * Implements target writer for AWS IoT Core
 * @property client AwsConnectionInterface AWS IoT core interface
 * @property targetID String ID of the target
 * @property targetConfig AwsIotTargetConfiguration Configuration of the topic to publish message to
 * @see AwsIotTargetConfiguration
 * @see TargetWriter
 */
class AwsIotMqttTargetWriter(
    private val client: AwsConnectionInterface,
    private val targetID: String,
    private val configReader: ConfigReader,
    private val targetConfig: AwsIotTargetConfiguration,
    private val logger: Logger,
    resultHandler: TargetResultHandler?) : TargetWriter {

    private val className = this::class.java.simpleName

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val metricDimensions = mapOf(METRICS_DIMENSION_SOURCE to targetID,
        MetricsCollector.METRICS_DIMENSION_TYPE to className)

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
        client.close()
    }


    private val targetResults = if (resultHandler != null) TargetResultHelper(targetID, resultHandler, logger) else null
    private val config: AwsIotWriterConfiguration by lazy { configReader.getConfig() }
    private val scope = buildScope("IoT Target")

    // channel to pass data to the coroutine that publishes the data to the topic
    private val targetDataChannel = Channel<TargetData>(100)

    // Coroutine that publishes the messages to the target topic
    private val writer = scope.launch(context = Dispatchers.IO, name = "Writer") {
        val log = logger.getCtxLoggers(className, "writer")
        try {
            runWriter()
        }catch (e: CancellationException) {
            log.info("Writer stopped")
        }catch (e : Exception){
            log.error("Error in writer, $e")
        }
    }

    private val metricsCollector: MetricsCollector? by lazy {
        val metricsConfiguration = config.targets[targetID]?.metrics ?: MetricsSourceConfiguration()
        if (config.isCollectingMetrics) {
            logger.metricsCollectorMethod = collectMetricsFromLogger
            MetricsCollector(metricsConfig = config.metrics,
                metricsSourceName = targetID,
                metricsSourceType = MetricsSourceType.TARGET_WRITER,
                metricsSourceConfiguration = metricsConfiguration,
                staticDimensions = TARGET_METRIC_DIMENSIONS,
                logger = logger)
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

        log.info("AWS IoT MQTT writer for target \"$targetID\" writer publishing to topic \"${targetConfig.topicName}\" at endpoint ${targetConfig.endpoint} on target $targetID")

        // get received data to write from channel and process/transit
        for (targetData in targetDataChannel) {
            sendTargetData(targetData, log)
        }
    }

    private val transformation by lazy { if (targetConfig.template != null) OutputTransformation(targetConfig.template!!, logger) else null }

    private fun buildPayload(targetData: TargetData): String =
        if (transformation == null) targetData.toJson(config.elementNames) else transformation!!.transform(targetData, config.elementNames) ?: ""


    // Build message payload and publishes message
    private fun sendTargetData(
        targetData: TargetData, log: Logger.ContextLogger
    ) {
        val topicName = targetConfig.topicName ?: targetData.schedule
        // optionally transform data or use JSON format
        val payload = buildPayload(targetData)
        log.trace("Sending data to topic on target $targetID\"$topicName\", $payload")

        val start = DateTime.systemDateTime().toEpochMilli()

        try {
            client.publish(topicName, payload, targetConfig.qos)
            targetResults?.ack(targetData)
            val writeDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()
            createMetrics(targetID, metricDimensions, payload.length, writeDurationInMillis)

        } catch (e: Throwable) {
            val message = "Error publishing to topic \"$topicName\" for target \"$targetID\", ${e.message}"
            runBlocking { metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions) }
            log.error(message)
            if (canNotReachAwsService(e)) {
                targetResults?.nack(targetData)
            } else {
                targetResults?.error(targetData)
            }
            throw TargetException(message)
        }
    }

    private fun createMetrics(adapterID: String,
                              metricDimensions: MetricDimensions,
                              size: Int,
                              writeDurationInMillis: Double) {

        runBlocking {
            metricsCollector?.put(adapterID,
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITES, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_MESSAGES, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_DURATION, writeDurationInMillis, MetricUnits.MILLISECONDS, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SIZE, size.toDouble(), MetricUnits.BYTES, metricDimensions))
        }

    }

    companion object {

        private val className = this::class.java.simpleName

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any?) =
            newInstance(createParameters[0] as ConfigReader, createParameters[1] as String, createParameters[2] as Logger, createParameters[3] as TargetResultHandler?)

        /**
         * Creates a new writer for an AWS IoT core target
         * @param configReader ConfigReader Configuration for writer
         * @see AwsIotWriterConfiguration
         * @param targetID String ID of the target
         * @param logger Logger Logger for output
         * @return TargetWriterCreated AWS IoT Core Target writer
         */
        @JvmStatic
        fun newInstance(configReader: ConfigReader, targetID: String, logger: Logger, resultHandler: TargetResultHandler?): TargetWriter {

            // Obtain configuration
            val config: AwsIotWriterConfiguration = readConfig(configReader)

            // Obtain configuration for used target
            val mqttConfig = config.targets[targetID]
                             ?: throw TargetException("Configuration for $AWS_IOT_MQTT_TARGET type target with ID \"$targetID\" does not exist, existing targets are ${config.targets.keys}")
            return try {
                createWriter(configReader, mqttConfig, targetID, logger, resultHandler)
            } catch (e: Throwable) {
                throw TargetException("Error creating $AWS_IOT_MQTT_TARGET target for target \"$targetID\", $e")
            }
        }

        // Creates a new AwsIotMqttTargetWriter
        private fun createWriter(configReader: ConfigReader,
                                 mqttConfig: AwsIotTargetConfiguration,
                                 targetID: String,
                                 logger: Logger,
                                 resultHandler: TargetResultHandler?): AwsIotMqttTargetWriter {
            val client = buildClient(mqttConfig, targetID, logger)
            val logs = logger.getCtxLoggers(className, "createWriter")
            try {
                logs.trace("Connecting to AWS IoT core endpoint ${mqttConfig.endpoint}")
                client.connect()
                logs.trace("Connected to AWS IoT core service")
            } catch (e: Throwable) {
                val message = "Can not connect to AWS IoT core service for target \"$targetID\" at endpoint ${mqttConfig.endpoint}, $e"
                logs.error(message)
                throw TargetException(message)

            }
            return AwsIotMqttTargetWriter(
                client = client as AwsConnectionInterface,
                configReader = configReader,
                targetID = targetID,
                targetConfig = mqttConfig,
                logger = logger,
                resultHandler = resultHandler
            )
        }

        // Reads AwsIotMqttTargetWriter configuration
        private fun readConfig(configReader: ConfigReader): AwsIotWriterConfiguration {
            return try {
                configReader.getConfig()
            } catch (e: Exception) {
                throw TargetException("Could not load Aws MQTT Target configuration: $e")
            }
        }

        // Builds the connection to AWS Core
        private fun buildClient(mqttConfig: AwsIotTargetConfiguration, targetID: String, logger: Logger): AwsIotConnectionWrapper {
            try {
                val clientID = "${AWS_IOT_MQTT_TARGET}:$targetID:${Random.nextLong().absoluteValue}"
                return AwsIotConnectionWrapper.newConnection(mqttConfig, clientID)
            } catch (e: Throwable) {
                logger.getCtxErrorLog(className, "buildClient")("$e")
                throw e
            }

        }

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET)

    }


}