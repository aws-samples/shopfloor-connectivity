// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.mqtt

import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.data.JsonHelper.Companion.extendedJsonException
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
import com.amazonaws.sfc.targets.TargetDataChannel
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.*
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.measureTime

class MqttTargetWriter(
    private val targetID: String,
    private val configReader: ConfigReader,
    private val targetConfig: MqttTargetConfiguration,
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

    private val buffer = TargetDataBuffer(storeFullMessage = false)
    private val doesBatching by lazy { targetConfig.batchSize != null || targetConfig.batchCount != null || targetConfig.batchInterval != Duration.INFINITE }
    private val usesCompression = targetConfig.compressionType != CompressionType.NONE

    private var _mqttClient: MqttClient? = null
    private suspend fun getClient(context: CoroutineContext): MqttClient {

        val log = logger.getCtxLoggers(className, "getClient")

        var retries = 0
        while (_mqttClient == null && context.isActive && retries < targetConfig.connectRetries) {
            try {
                val mqttHelper = MqttHelper(targetConfig.mqttConnectionOptions, logger)
                _mqttClient = mqttHelper.buildClient("${className}_${targetID}_${getHostName()}_${UUID.randomUUID()}")
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
            targetDataChannel.close()
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
    private val config: MqttWriterConfiguration by lazy { configReader.getConfig() }
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

                        val messagePayload = buildPayload(targetData)

                        if (checkMessagePayloadSize(targetData, messagePayload.length, log)) {

                            if (exceedBufferOrMaxPayloadWhenBufferingMessage(messagePayload)) {
                                log.trace("Batch size of ${targetConfig.batchSize?.byteCountString}${if (targetConfig.maxPayloadSize != null) " or ${targetConfig.maxPayloadSize!!.byteCountString}" else ""}}reached")
                                timer = writeBufferedMessages(timer)
                            }
                            targetResults?.add(targetData)
                            buffer.add(targetData, messagePayload)

                            log.trace("Received message, buffered size is ${buffer.payloadSize.byteCountString}")

                            if (targetData.noBuffering || bufferReachedMaxSizeOrMessages(log)) {
                                timer = writeBufferedMessages(timer)
                            }
                        }
                    }

                    timer.onJoin {
                        log.trace("${targetConfig.batchInterval} batch interval reached")
                        timer = writeBufferedMessages(timer)

                    }
                }
            } catch (e: Exception) {
                if (!e.isJobCancellationException)
                    log.errorEx("Error in writer", e)
            }
        }
        writeBufferedMessages(timer).cancel()

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

    private fun bufferReachedMaxSizeOrMessages(log: Logger.ContextLogger): Boolean {
        val reachedBufferCount =  ((targetConfig.batchCount!= null ) && (buffer.size >= targetConfig.batchCount!!))
        if (((targetConfig.batchCount ?: 0)) > 1 && reachedBufferCount) log.trace("${targetConfig.batchCount} batch count reached")

        val reachedBufferSize = !reachedBufferCount &&
                targetConfig.batchSize != null &&
                (buffer.payloadSize + (2 + (buffer.size - 1)) >= targetConfig.batchSize!!)

        if (reachedBufferSize) log.trace("${targetConfig.batchSize?.byteCountString} batch size reached")

        return reachedBufferSize || reachedBufferCount
    }



    private fun checkMessagePayloadSize(targetData: TargetData, payloadSize: Int, log: Logger.ContextLogger): Boolean {
        if (usesCompression) return true
        return if (targetConfig.maxPayloadSize != null && payloadSize > targetConfig.maxPayloadSize!!) {
            log.error("Size $payloadSize bytes of message is larger max payload size  ${targetConfig.maxPayloadSize!!.byteCountString} for target")
            TargetResultHelper(targetID, resultHandler, logger).error(targetData)
            false
        } else true
    }

    private fun exceedBufferOrMaxPayloadWhenBufferingMessage(payload: String): Boolean {
        if (usesCompression) return false
        val bufferedPayloadSizeWhenAddingMessage = payload.length + (2 + (buffer.size - 1)) + buffer.payloadSize
        val bufferSizeExceededWhenAddingMessage = targetConfig.batchSize != null && bufferedPayloadSizeWhenAddingMessage > targetConfig.batchSize!!
        val maxPayloadSizeExceededWhenAddingMessage =
            targetConfig.maxPayloadSize != null && bufferedPayloadSizeWhenAddingMessage > targetConfig.maxPayloadSize!!
        val reachedMaxSizeWhenAddingToBuffer = (bufferSizeExceededWhenAddingMessage || maxPayloadSizeExceededWhenAddingMessage)
        return reachedMaxSizeWhenAddingToBuffer
    }


    private fun buildMqttMessage(): MqttMessage {
        val message = MqttMessage()
        val payload = if (doesBatching) buffer.payloads.joinToString(prefix = "[", postfix = "]", separator = ",") { it } else buffer.payloads.first()
        if (targetConfig.compressionType == CompressionType.NONE) {
            message.payload = payload.toByteArray()
        } else {
            message.payload = compressPayload(payload)

        }
        message.qos = targetConfig.qos
        return message
    }


    private fun compressPayload(content: String): ByteArray {
        val inputStream = content.byteInputStream(Charsets.UTF_8)
        val outputStream = ByteArrayOutputStream(2048)
        Compress.compress(targetConfig.compressionType, inputStream, outputStream, entryName = "${UUID.randomUUID()}")
        val log = logger.getCtxLoggers(className, "compressContent")
        val compressedData = outputStream.toByteArray()
        log.info("Used ${targetConfig.compressionType} compression to compress ${content.length.byteCountString} to ${compressedData.size.byteCountString} bytes, ${(100 - (compressedData.size.toFloat() / content.length.toFloat()) * 100).toInt()}% size reduction")
        return compressedData
    }

    private suspend fun writeBufferedMessages(timer : Job) : Job {

        if (timer.isActive) timer.cancel()

        val log = logger.getCtxLoggers(className, "writeBufferedMessages")
        if (buffer.size == 0) {
            return createTimer()
        }

        try {

            val mqttMessage = buildMqttMessage()

            val duration = measureTime {
                val client = runBlocking {
                    getClient(coroutineContext)
                }

                // message beyond max payload size
                if (targetConfig.maxPayloadSize != null && mqttMessage.payload.size > targetConfig.maxPayloadSize!!) {
                    log.error("Size of MQTT message ${mqttMessage.payload.size} bytes is beyond max payload size  ${targetConfig.maxPayloadSize!!.byteCountString} for target")
                    targetResults?.errorBuffered()
                } else {

                    withTimeout(targetConfig.publishTimeout) {
                        client.publish(targetConfig.topicName, mqttMessage)
                    }
                    targetResults?.ackBuffered()
                }
            }

            val compressedStr = if (targetConfig.compressionType != CompressionType.NONE) " compressed " else " "
            val itemStr = if (doesBatching) " containing ${buffer.size} items " else " "
            log.trace("Published MQTT${compressedStr}message with size of ${mqttMessage.payload.size.byteCountString} bytes${itemStr}in $duration")

        } catch (e: Exception) {
            runBlocking { metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions) }
            log.errorEx("Error publishing to topic \"${targetConfig.topicName}\" for target \"$targetID\", ${e.message}", e)
            if (e is TimeoutCancellationException || _mqttClient == null) {
                targetResults?.nackBuffered()
            } else {
                targetResults?.errorBuffered()
            }
        } finally {
            buffer.clear()
        }
        return createTimer()
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

    private val transformation by lazy { if (targetConfig.template != null) OutputTransformation(targetConfig.template!!, logger) else null }

    private fun buildPayload(targetData: TargetData): String =
        if (transformation == null) targetData.toJson(config.elementNames) else transformation!!.transform(targetData, config.elementNames) ?: ""


    private fun createMetrics(
        adapterID: String,
        metricDimensions: MetricDimensions,
        writeDurationInMillis: Double
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
                    METRICS_WRITES,
                    1.0,
                    MetricUnits.COUNT, metricDimensions
                ),
                metricsCollector?.buildValueDataPoint(
                    adapterID,
                    METRICS_MESSAGES,
                    buffer.size.toDouble(),
                    MetricUnits.COUNT,
                    metricDimensions
                ),
                metricsCollector?.buildValueDataPoint(
                    adapterID,
                    METRICS_WRITE_DURATION,
                    writeDurationInMillis,
                    MetricUnits.MILLISECONDS,
                    metricDimensions
                ),
                metricsCollector?.buildValueDataPoint(
                    adapterID,
                    METRICS_WRITE_SUCCESS,
                    1.0,
                    MetricUnits.COUNT,
                    metricDimensions
                ),
                metricsCollector?.buildValueDataPoint(
                    adapterID,
                    METRICS_WRITE_SIZE,
                    buffer.payloadSize.toDouble(),
                    MetricUnits.BYTES,
                    metricDimensions
                )
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