// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.mqtt

import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.data.JsonHelper.Companion.extendedJsonException
import com.amazonaws.sfc.log.LogLevel
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
import com.amazonaws.sfc.mqtt.config.MqttTargetConfiguration
import com.amazonaws.sfc.mqtt.config.MqttTargetConfiguration.Companion.CONFIG_ALTERNATE_TOPIC_NAME
import com.amazonaws.sfc.mqtt.config.MqttTargetConfiguration.Companion.CONFIG_BATCH_COUNT
import com.amazonaws.sfc.mqtt.config.MqttTargetConfiguration.Companion.CONFIG_BATCH_INTERVAL
import com.amazonaws.sfc.mqtt.config.MqttTargetConfiguration.Companion.CONFIG_BATCH_SIZE
import com.amazonaws.sfc.mqtt.config.MqttTargetConfiguration.Companion.CONFIG_TOPIC_NAME
import com.amazonaws.sfc.mqtt.config.MqttWriterConfiguration
import com.amazonaws.sfc.mqtt.config.MqttWriterConfiguration.Companion.MQTT_TARGET
import com.amazonaws.sfc.targets.TargetDataChannel
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.targets.TargetFormatter
import com.amazonaws.sfc.targets.TargetFormatterFactory
import com.amazonaws.sfc.util.*
import com.amazonaws.sfc.util.TemplateRenderer.containsPlaceHolders
import com.amazonaws.sfc.util.TemplateRenderer.getPlaceHolders
import com.amazonaws.sfc.util.TemplateRenderer.render
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap
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

    private val buffers = ConcurrentHashMap<String, TargetDataBuffer>()// TargetDataBuffer(storeFullMessage = false)
    private val timers = ConcurrentHashMap<String, Job>()
    private val timerChannel = Channel<String>(capacity = 100)

    private val doesBatching by lazy { targetConfig.batchSize != 0 || targetConfig.batchCount != 0 || targetConfig.batchInterval != Duration.INFINITE }
    private val usesCompression = targetConfig.compressionType != CompressionType.NONE

    private val formatter: TargetFormatter? by lazy {
        TargetFormatterFactory.createTargetFormatter(configReader, targetID,targetConfig, logger)
    }

    private val customPayload = (formatter != null)

    private var _mqttClient: MqttClient? = null
    private suspend fun getClient(context: CoroutineContext): MqttClient {

        val log = logger.getCtxLoggers(className, "getClient")

        var retries = 0
        while (_mqttClient == null && context.isActive && retries < targetConfig.connectRetries) {
            try {
                val mqttHelper = MqttHelper(targetConfig.mqttConnectionOptions, logger)
                val clientId = if (targetConfig.clientId.isEmpty())
                    "${className}_${targetID}_${getHostName()}_${UUID.randomUUID()}"
                else
                    targetConfig.clientId
                _mqttClient = mqttHelper.buildClient(clientId)
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
    private val config: MqttWriterConfiguration by lazy { configReader.getConfig() }
    private val scope = buildScope("MQTT Target")


    // channel to pass data to the coroutine that publishes the data to the topic
    private val targetDataChannel = TargetDataChannel.create(targetConfig, "$className:targetDataChannel")

    // Coroutine publishing the messages to the target topic
    private val writer = scope.launch(context = Dispatchers.IO, name = "Writer") {
        val log = logger.getCtxLoggers(className, "writer")


        try {
            log.info("MQTT Writer for target \"$targetID\" writer publishing to topics at endpoint ${targetConfig.endPoint} on target $targetID")
            while (isActive) {
                try {
                    select {

                        targetDataChannel.channel.onReceive { targetData ->

                            targetResults?.add(targetData)

                            val topicMessages = mapTargetDataToTopics(targetData)
                            if (topicMessages.size > 1) {
                                log.trace("Message ${targetData.serial} mapped to topics ${topicMessages.keys}")
                            }

                            topicMessages.forEach { (topic, topicTargetData) ->

                                val topicBuffer = buffers.computeIfAbsent(topic) { TargetDataBuffer(storeFullMessage = customPayload) }
                                val timer = timers.computeIfAbsent(topic) { createTimer(topic) }

                                val (messagePayload: String?, payloadSize: Int) = if (customPayload) {
                                    val formattedItemSize = try {
                                        (formatter?.itemPayloadSize(targetData) ?: altSize(topicTargetData))
                                    } catch (e: Exception) {
                                        log.errorEx("Error getting payload size using custom formatter", e)
                                        altSize(topicTargetData)
                                    }
                                    null to formattedItemSize
                                } else {
                                    val messagePayload = buildPayload(topicTargetData)
                                    messagePayload to messagePayload.length
                                }

                                if (checkMessagePayloadSize(targetData, payloadSize, log)) {

                                    if (exceedBufferOrMaxPayloadWhenBufferingMessage(topicBuffer, payloadSize)) {
                                        log.trace("Batch size of ${targetConfig.batchSize.byteCountString}${if (targetConfig.maxPayloadSize != null) " or ${targetConfig.maxPayloadSize!!.byteCountString}" else ""} for topic $topic reached")
                                        timers[topic] = writeBufferedMessages(topicBuffer, topic, timer)
                                    }

                                    if (customPayload) {
                                        topicBuffer.add(targetData, payloadSize)
                                    } else {
                                        topicBuffer.add(targetData, messagePayload)
                                    }


                                    log.trace("Received message, buffered items for topic \"$topic\" is ${topicBuffer.size}${if (!customPayload) " with a total payload size of ${topicBuffer.payloadSize.byteCountString}" else ""}")
                                    if (targetData.noBuffering || !doesBatching || bufferReachedMaxSizeOrMessages(topicBuffer, topic, log)) {
                                        timers[topic] = writeBufferedMessages(topicBuffer, topic, timer)
                                    }
                                }
                            }
                        }

                        timerChannel.onReceive { topic ->
                            val topicBuffer = buffers[topic]
                            log.trace("${targetConfig.batchInterval} batch interval reached for topic $topic")
                            val timer = timers[topic]
                            if (topicBuffer != null && timer != null)
                                timers[topic] = writeBufferedMessages(topicBuffer, topic, timer)

                        }
                    }


                } catch (e: Exception) {
                    if (!e.isJobCancellationException)
                        log.errorEx("Error in writer", e)

                    timers.keys.forEach {
                        timers[it]?.cancel()
                        timers[it] = createTimer(it)
                    }
                }
            }

            buffers.forEach { (topic, buffer) ->
                writeBufferedMessages(buffer, topic, timers[topic]!!).cancel()
            }

        } catch (e: Exception) {
            logger.getCtxErrorLogEx(className, "targetWriter")("Error in target writer", e)
        }

    }

    private fun altSize(targetData: TargetData): Int = if (targetConfig.batchCount != 0) 0 else targetData.toJson(config.elementNames, targetConfig.unquoteNumericJsonValues).length


    private fun createTimer(channel: String): Job {
        return scope.launch {
            try {
                delay(targetConfig.batchInterval)
                timerChannel.send(channel)
            } catch (_: Exception) {
                // no harm done, timer is just used to guard for timeouts
            }
        }
    }

    private fun bufferReachedMaxSizeOrMessages(buffer: TargetDataBuffer, topic: String, log: Logger.ContextLogger): Boolean {
        val reachedBufferCount = if (targetConfig.batchCount > 0) (buffer.size >= targetConfig.batchCount) else false
        if (reachedBufferCount) log.trace("${targetConfig.batchCount} batch count reached")

        val reachedBufferSize = (targetConfig.batchSize > 0) && (buffer.payloadSize + (2 + (buffer.size - 1)) >= targetConfig.batchSize)

        if (reachedBufferSize) log.trace("${targetConfig.batchSize.byteCountString} batch size for topic $topic reached")

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

    private fun exceedBufferOrMaxPayloadWhenBufferingMessage(buffer: TargetDataBuffer, payloadSize: Int): Boolean {
        if (usesCompression) return false
        val bufferedPayloadSizeWhenAddingMessage = payloadSize + (2 + (buffer.size - 1)) + buffer.payloadSize
        val bufferSizeExceededWhenAddingMessage = (targetConfig.batchSize > 0) && (bufferedPayloadSizeWhenAddingMessage > targetConfig.batchSize)
        val maxPayloadSizeExceededWhenAddingMessage =
            targetConfig.maxPayloadSize != null && bufferedPayloadSizeWhenAddingMessage > targetConfig.maxPayloadSize!!
        val reachedMaxSizeWhenAddingToBuffer = (bufferSizeExceededWhenAddingMessage || maxPayloadSizeExceededWhenAddingMessage)
        return reachedMaxSizeWhenAddingToBuffer
    }


    private fun buildMqttMessage(buffer: TargetDataBuffer): MqttMessage {
        val message = MqttMessage()

        val payLoad: ByteArray = buildPayload(buffer)

        message.payload = payLoad
        message.isRetained = targetConfig.retain
        message.qos = targetConfig.qos
        return message
    }

    private fun buildPayload(buffer: TargetDataBuffer): ByteArray = if (customPayload) {
        try {
            val binaryPayload = formatter!!.apply(buffer.messages)
            if (targetConfig.compressionType == CompressionType.NONE) {
                binaryPayload
            } else
                compressPayload(ByteArrayInputStream(binaryPayload), binaryPayload.size)
        } catch (e: Exception) {
            logger.getCtxErrorLog(className, "targetWriter")("Error executing custom formatter for target \"$targetID\", $e")
            ByteArray(0)
        }
    } else {
        val stringPayload = if (doesBatching)
            if (targetConfig.arrayWhenBuffered)
                buffer.payloads.joinToString(prefix = "[", postfix = "]", separator = ",") { it }
            else
                buffer.payloads.joinToString(separator = "") { it }
        else
            buffer.payloads.first()

        if (targetConfig.compressionType == CompressionType.NONE) {
            stringPayload.toByteArray()
        } else {
            compressPayload(stringPayload.byteInputStream(Charsets.UTF_8), stringPayload.length)
        }

    }


    private fun compressPayload(content: ByteArrayInputStream, size: Int): ByteArray {
        val outputStream = ByteArrayOutputStream(2048)
        Compress.compress(targetConfig.compressionType, content, outputStream, entryName = "${UUID.randomUUID()}")
        val log = logger.getCtxLoggers(className, "compressContent")
        val compressedData = outputStream.toByteArray()
        log.info("Used ${targetConfig.compressionType} compression to compress ${size.byteCountString} to ${compressedData.size.byteCountString} bytes, ${(100 - (compressedData.size.toFloat() / size.toFloat()) * 100).toInt()}% size reduction")
        return compressedData
    }

    private suspend fun writeBufferedMessages(buffer: TargetDataBuffer, topic: String, timer: Job): Job {

        if (timer.isActive) timer.cancel()

        val log = logger.getCtxLoggers(className, "writeBufferedMessages")
        if (buffer.size == 0) {
            return createTimer(topic)
        }

        return try {

            val mqttMessage = buildMqttMessage(buffer)

            return when {

                (mqttMessage.payload.size == 0) -> {
                    log.trace("No payload to publish to topic $topic")
                    targetResults?.ackBuffered()
                    buffer.clear()
                    createTimer(topic)
                }

                (targetConfig.maxPayloadSize != null && mqttMessage.payload.size > targetConfig.maxPayloadSize!!) -> {
                    log.error("Size of MQTT message ${mqttMessage.payload.size} bytes is beyond max payload size of  ${targetConfig.maxPayloadSize!!.byteCountString} for target, reduce or set $CONFIG_BATCH_SIZE, $CONFIG_BATCH_COUNT or $CONFIG_BATCH_INTERVAL for this target")
                    targetResults?.errorBuffered()
                    metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions)
                    createTimer(topic)
                }

                else -> {
                    val duration = measureTime {
                        val client = runBlocking {
                            getClient(coroutineContext)
                        }

                        withTimeout(targetConfig.publishTimeout) {
                            client.publish(topic, mqttMessage)
                        }

                        targetResults?.ackBuffered()
                    }
                    val compressedStr = if (targetConfig.compressionType != CompressionType.NONE) " compressed " else " "
                    val itemStr = if (doesBatching) " containing ${buffer.size} items " else " "
                    log.trace("Published MQTT${compressedStr}message to topic\"$topic\" with size of ${mqttMessage.payload.size.byteCountString} ${itemStr}in $duration")

                    createMetrics(targetID, metricDimensions, buffer, mqttMessage.payload.size, duration)
                    createTimer(topic)
                }
            }

        } catch (e: Exception) {
            if (!e.isJobCancellationException) {
                metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions)
                log.errorEx("Error publishing to topic \"topic\" for target \"$targetID\", ${e.message}", e)
                if (e is TimeoutCancellationException || _mqttClient == null) {
                    targetResults?.nackBuffered()
                } else {
                    targetResults?.errorBuffered()
                }
            }
            createTimer(topic)
        } finally {
            buffer.clear()
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

    private val transformation by lazy { if (targetConfig.template != null) OutputTransformation(targetConfig.template!!, logger) else null }

    private fun buildPayload(targetData: TargetData): String =
        if (transformation == null) targetData.toJson(config.elementNames, targetConfig.unquoteNumericJsonValues) else transformation!!.transform(
            targetData,
            config.elementNames,
            targetConfig.templateEpochTimestamp) ?: ""


    private fun createMetrics(
        adapterID: String,
        metricDimensions: MetricDimensions,
        buffer: TargetDataBuffer,
        payloadSize: Int,
        duration: Duration
    ) {

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
            metricsCollector?.buildValueDataPoint(adapterID, METRICS_MESSAGES, buffer.size.toDouble(), MetricUnits.COUNT, metricDimensions),
            metricsCollector?.buildValueDataPoint(
                adapterID,
                METRICS_WRITE_DURATION,
                duration.inWholeMilliseconds.toDouble(),
                MetricUnits.MILLISECONDS,
                metricDimensions
            ),
            metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions),
            metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SIZE, buffer.payloadSize.toDouble(), MetricUnits.BYTES, metricDimensions)
        )

    }


    private fun mapTargetDataToTopics(targetData: TargetData): Map<String, TargetData> =
        targetData.splitDataByName(targetConfig.topicNameTemplate, ::buildTopicName)

    private fun buildTopicName(targetData: TargetData,
                               sourceName: String,
                               channel: String,
                               channelMetadata: Map<String, String>): String {

        val log = logger.getCtxLoggers(className, "buildTopicName")

        var topicName = if (containsPlaceHolders(targetConfig.topicNameTemplate)) {
            render(targetConfig.topicNameTemplate, targetData.schedule, sourceName, channel, targetID, channelMetadata)
        } else targetConfig.topicNameTemplate

        if (containsPlaceHolders(topicName)) {
            val messageStr = "Source \"$sourceName\", channel \"${channel}\""
            if (targetConfig.alternateTopicName != null) {
                log.trace("$messageStr has unmapped placeholder(s) ${getPlaceHolders(topicName)} in topic name \"$topicName\", using $CONFIG_TOPIC_NAME \"${targetConfig.topicNameTemplate}\", now using alternative $CONFIG_ALTERNATE_TOPIC_NAME \"${targetConfig.alternateTopicName}\"")
                topicName = render(targetConfig.alternateTopicName!!, targetData.schedule, sourceName, channel, targetID, channelMetadata)
                if (containsPlaceHolders(topicName)) {
                    if (targetConfig.warnAlternateTopicName || logger.level == LogLevel.TRACE) log.warning("$messageStr has unmapped placeholder(s) ${getPlaceHolders(topicName)} in topic name \"$topicName\", using $CONFIG_ALTERNATE_TOPIC_NAME \"${targetConfig.alternateTopicName}\"")
                    topicName = ""
                }
            } else {
                if (targetConfig.warnAlternateTopicName || logger.level == LogLevel.TRACE) log.warning("$messageStr has unmapped placeholder(s) ${getPlaceHolders(topicName)} in topic name \"$topicName\", using $CONFIG_TOPIC_NAME \"${targetConfig.topicNameTemplate}\"")
                topicName = ""
            }
        }
        return topicName
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