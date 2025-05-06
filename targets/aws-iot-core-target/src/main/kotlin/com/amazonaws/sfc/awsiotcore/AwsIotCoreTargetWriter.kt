// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsiotcore


import com.amazonaws.sfc.awsiotcore.config.AwsIotCoreTargetConfiguration
import com.amazonaws.sfc.awsiotcore.config.AwsIotCoreTargetConfiguration.Companion.CONFIG_ALTERNATE_TOPIC_NAME
import com.amazonaws.sfc.awsiotcore.config.AwsIotCoreTargetConfiguration.Companion.CONFIG_BATCH_COUNT
import com.amazonaws.sfc.awsiotcore.config.AwsIotCoreTargetConfiguration.Companion.CONFIG_BATCH_INTERVAL
import com.amazonaws.sfc.awsiotcore.config.AwsIotCoreTargetConfiguration.Companion.CONFIG_TOPIC_NAME
import com.amazonaws.sfc.awsiotcore.config.AwsIotCoreWriterConfiguration
import com.amazonaws.sfc.awsiotcore.config.AwsIotCoreWriterConfiguration.Companion.AWS_IOT_CORE_TARGET
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_BATCH_SIZE
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.*
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
import com.amazonaws.sfc.targets.*
import com.amazonaws.sfc.util.*
import com.amazonaws.sfc.util.TemplateRenderer.containsPlaceHolders
import com.amazonaws.sfc.util.TemplateRenderer.getPlaceHolders
import com.amazonaws.sfc.util.TemplateRenderer.render
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.iot.IotClient
import software.amazon.awssdk.services.iot.model.DescribeEndpointRequest
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClientBuilder
import software.amazon.awssdk.services.iotdataplane.model.PublishRequest
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.measureTime


/**
 * @property targetID String ID of the target
 * @property configReader configuration reader
 * @property logger to use for output
 * @see TargetWriter
 */
class AwsIotCoreTargetWriter(
    private val targetID: String,
    private val configReader: ConfigReader,
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

    private val scope = CoroutineScope(Dispatchers.Default) + CoroutineName("IoT Core Target")

    private val targetConfig: AwsIotCoreTargetConfiguration by lazy {
        clientHelper.targetConfig(config, targetID, AWS_IOT_CORE_TARGET)
    }
    private val buffers = ConcurrentHashMap<String, TargetDataBuffer>()// TargetDataBuffer(storeFullMessage = false)
    private val timers = ConcurrentHashMap<String, Job>()
    private val timerChannel = Channel<String>(capacity = 100)

    private val doesBatching by lazy { targetConfig.batchSize > 0 || targetConfig.batchCount > 0 || targetConfig.batchInterval != Duration.INFINITE }
    private val usesCompression = targetConfig.compressionType != CompressionType.NONE

    private val dataClientBuilder: IotDataPlaneClientBuilder = IotDataPlaneClient.builder().endpointOverride(URI.create("https://$endpoint"))

    private val clientHelper =
        AwsServiceTargetClientHelper(
            configReader.getConfig<AwsIotCoreWriterConfiguration>(),
            targetID,
            dataClientBuilder,
            logger
        )

    private val formatter: TargetFormatter? by lazy {
        TargetFormatterFactory.createTargetFormatter(configReader, targetID, targetConfig, logger)
    }

    private val customPayload = (formatter != null)


    // Channel to pass data to be sent to target to worker that does the actual sending
    private val targetDataChannel = TargetDataChannel.create(targetConfig, "$className:targetDataChannel")
    private val targetResults = if (resultHandler != null) TargetResultBufferedHelper(targetID, resultHandler, logger) else null

    private val iotDataPlaneClient: AwsIoTCoreDataPlaneClient
        get() {
            return AwsIotCoreClientWrapper(clientHelper.serviceClient as IotDataPlaneClient)
        }

    private val endpoint: String?
        get() {
            val iotControlHelper = AwsServiceTargetClientHelper(
                configReader.getConfig<AwsIotCoreWriterConfiguration>(),
                targetID,
                IotClient.builder(),
                logger
            )
            val iotControlClient = iotControlHelper.serviceClient as IotClient
            val endpoint = iotControlClient.describeEndpoint { r: DescribeEndpointRequest.Builder -> r.endpointType("iot:Data-ATS") }?.endpointAddress()
            iotControlClient.close()
            return endpoint
        }


    private val config: AwsIotCoreWriterConfiguration
        get() {
            return clientHelper.writerConfig(configReader, AWS_IOT_CORE_TARGET)
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

    /**
     *
     * @param targetData TargetData data to be published to the target.
     */
    override suspend fun writeTargetData(targetData: TargetData) {
        // Accept data and send for worker for processing and  sending to IoT topic target
        targetDataChannel.submit(targetData, logger.getCtxLoggers(className, "writeTargetData"))
    }

    /**
     * Closes the target
     * @see TargetWriter
     */
    override suspend fun close() {
        writer.cancel()
    }

    // Worker coroutine that handles the publishing of the data to the topic
    private val writer = scope.launch(context = Dispatchers.IO, name = "Writer") {
        try {
            val log = logger.getCtxLoggers(className, "writer")


            log.info("AWS IoT Core writer for target \"$targetID\" publishing to topics in region ${targetConfig.region}")

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
                                        (formatter?.itemPayloadSize(targetData) ?: altSize(targetData))
                                    } catch (e: Exception) {
                                        log.errorEx("Error getting payload size using custom formatter", e)
                                        altSize(targetData)
                                    }
                                    null to formattedItemSize
                                } else {
                                    val messagePayload = buildPayload(topicTargetData)
                                    messagePayload to messagePayload.length
                                }

                                if (checkMessagePayloadSize(targetData, payloadSize, log)) {
                                    if (exceedBufferOrMaxPayloadWhenBufferingMessage(topicBuffer, payloadSize)) {
                                        log.trace("Batch size of ${targetConfig.batchSize.byteCountString} or AWS IoT Core max payload ise of ${AWS_IOT_CORE_MAX_PAYLOAD_SIZE.byteCountString} reached")
                                        timers[topic] = writeBufferedMessages(topicBuffer, topic, timer)
                                    }
                                }

                                if (customPayload) {
                                    topicBuffer.add(targetData, payloadSize)
                                } else {
                                    topicBuffer.add(targetData, messagePayload)
                                }


                                if (targetData.noBuffering || !doesBatching || bufferReachedMaxSizeOrMessages(topicBuffer, topic, log)) {
                                    timers[topic] = writeBufferedMessages(topicBuffer, topic, timer)
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
                    if (e.isServiceNotReachable) {
                        targetResults?.nackBuffered()
                    } else {
                        targetResults?.errorBuffered()
                    }
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


    private fun checkMessagePayloadSize(targetData: TargetData, payloadSize: Int, log: Logger.ContextLogger): Boolean {
        if (usesCompression) return true
        return if (payloadSize > AWS_IOT_CORE_MAX_PAYLOAD_SIZE) {
            log.error("Size $payloadSize bytes of message is larger max payload size ${AWS_IOT_CORE_MAX_PAYLOAD_SIZE.byteCountString} for AWS IoT Core")
            TargetResultHelper(targetID, resultHandler, logger).error(targetData)
            false
        } else true
    }

    private fun exceedBufferOrMaxPayloadWhenBufferingMessage(buffer: TargetDataBuffer, payloadSize: Int): Boolean {
        val bufferedPayloadSizeWhenAddingMessage = payloadSize + (2 + (buffer.size - 1)) + buffer.payloadSize
        val bufferSizeExceededWhenAddingMessage = (targetConfig.batchSize > 0) && (bufferedPayloadSizeWhenAddingMessage > targetConfig.batchSize)
        val maxPayloadSizeExceededWhenAddingMessage = bufferedPayloadSizeWhenAddingMessage > AWS_IOT_CORE_MAX_PAYLOAD_SIZE
        val reachedMaxSizeWhenAddingToBuffer = (bufferSizeExceededWhenAddingMessage || maxPayloadSizeExceededWhenAddingMessage)
        return reachedMaxSizeWhenAddingToBuffer
    }

    private fun bufferReachedMaxSizeOrMessages(buffer: TargetDataBuffer, topic: String, log: Logger.ContextLogger): Boolean {

        val reachedBufferCount = if (targetConfig.batchCount > 0) buffer.size >= targetConfig.batchCount else false
        if (reachedBufferCount) log.trace("${targetConfig.batchCount} batch count for topic $topic reached")

        val reachedBufferSize = if (targetConfig.batchSize != 0) (buffer.payloadSize + (2 + (buffer.size - 1)) >= targetConfig.batchSize) else false
        if (reachedBufferSize) log.trace("${targetConfig.batchSize.byteCountString} batch size for topic $topic reached")

        return reachedBufferSize || reachedBufferCount
    }

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

    private val transformation by lazy { if (targetConfig.template != null) OutputTransformation(targetConfig.template!!, logger) else null }

    private fun buildPayload(targetData: TargetData): String =
        if (transformation == null) targetData.toJson(config.elementNames, targetConfig.unquoteNumericJsonValues) else transformation!!.transform(
            targetData,
            config.elementNames,
            targetConfig.templateEpochTimestamp) ?: ""

    private fun writeBufferedMessages(buffer: TargetDataBuffer, topic: String, timer: Job): Job {
        if (timer.isActive) timer.cancel()

        val log = logger.getCtxLoggers(className, "writeBufferedMessages")
        if (buffer.size == 0) {
            return createTimer(topic)
        }

        val request = buildRequest(buffer, topic)
        val payloadSize = request.payload().asByteArray().size

        return try {

            when {
                (payloadSize == 0) -> {
                    log.trace("No payload to publish to topic $topic")
                    targetResults?.errorBuffered()
                    buffer.clear()
                    createTimer(topic)
                }

                (payloadSize > AWS_IOT_CORE_MAX_PAYLOAD_SIZE) -> {
                    targetResults?.errorBuffered()
                    log.error("Size of MQTT payload for topic $topic is $payloadSize bytes, max payload size of AWS IoT core is $AWS_IOT_CORE_MAX_PAYLOAD_SIZE bytes, reduce or set $CONFIG_BATCH_SIZE, $CONFIG_BATCH_COUNT, $CONFIG_BATCH_INTERVAL for this target")
                    runBlocking { metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions) }
                    createTimer(topic)
                }

                else -> {
                    val duration = measureTime {
                        clientHelper.executeServiceCallWithRetries {
                            try {
                                log.trace("Publishing to topic $topic")
                                val resp = iotDataPlaneClient.publish(request)
                                log.trace("AWS IoT Core publish response ${resp.sdkHttpResponse().statusCode()}")
                                targetResults?.ackBuffered()
                            } catch (e: AwsServiceException) {
                                log.trace("AWS IoT Core publish error ${e.message}")
                                // Check the exception, it will throw an AwsServiceRetryableException if the error is recoverable
                                clientHelper.processServiceException(e)
                                // Non recoverable service exceptions
                                throw e
                            }
                        }
                    }

                    val compressedStr = if (targetConfig.compressionType != CompressionType.NONE) " compressed " else " "
                    val itemStr = if (doesBatching) " containing ${buffer.size} items " else " "
                    log.trace("Published MQTT${compressedStr}message to topic \"$topic\" with size of ${payloadSize.byteCountString} ${itemStr}in $duration")

                    createMetrics(targetID, metricDimensions, buffer.size, payloadSize, duration)
                    createTimer(topic)
                }
            }

        } catch (e: Exception) {
            val throttled = (e is SdkClientException && e.suppressedExceptions.any { (it.message ?: "").lowercase().contains("throttled") })
            val message = "Error writing to topic \"${targetConfig.topicNameTemplate}\", ${if (throttled) "throttled" else "${e.message}"}"
            log.error(message)
            runBlocking { metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions) }
            throw (e)
        } finally {
            buffer.clear()
        }
    }

    private fun buildRequest(buffer: TargetDataBuffer, topic: String): PublishRequest {

        val builder = PublishRequest.builder()
        builder.topic(topic)

        val payload: SdkBytes? = if (customPayload) {
            try {
                val binaryPayload = formatter!!.apply(buffer.messages)
                if (targetConfig.compressionType == CompressionType.NONE) {
                    SdkBytes.fromByteArray(binaryPayload)
                } else
                    SdkBytes.fromByteArray(compressPayload(ByteArrayInputStream(binaryPayload), binaryPayload.size))
            } catch (e: Exception) {
                logger.getCtxErrorLog(className, "targetWriter")("Error executing custom formatter for target \"$targetID\", $e")
                SdkBytes.fromByteArray(ByteArray(0))
            }

        } else {

            val stringPayload = if (doesBatching)
                if (targetConfig.arrayWhenBuffered)
                    buffer.payloads.joinToString(prefix = "[", postfix = "]", separator = ",") { it }
                else
                    buffer.payloads.joinToString(separator = "") { it }
            else
                buffer.payloads.first()

            if (targetConfig.compressionType == CompressionType.NONE)
                SdkBytes.fromUtf8String(stringPayload)
            else
                SdkBytes.fromByteArray(compressPayload(stringPayload.byteInputStream(Charsets.UTF_8), stringPayload.length))

        }

        builder.payload(payload)
        builder.retain(targetConfig.retain)
        return builder.build()
    }

    private fun compressPayload(content: ByteArrayInputStream, size: Int): ByteArray {
        val outputStream = ByteArrayOutputStream(2048)
        Compress.compress(targetConfig.compressionType, content, outputStream, entryName = "${UUID.randomUUID()}")
        val log = logger.getCtxLoggers(className, "compressContent")
        val compressedData = outputStream.toByteArray()
        log.info("Used ${targetConfig.compressionType} compression to compress ${size.byteCountString} to ${compressedData.size.byteCountString} bytes, ${(100 - (compressedData.size.toFloat() / size.toFloat()) * 100).toInt()}% size reduction")
        return compressedData
    }


    private fun createMetrics(
        adapterID: String, metricDimensions: MetricDimensions, messages: Int, size: Int, duration: Duration
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
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITES, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_MESSAGES, messages.toDouble(), MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(
                    adapterID,
                    METRICS_WRITE_DURATION,
                    duration.inWholeMilliseconds.toDouble(),
                    MetricUnits.MILLISECONDS,
                    metricDimensions
                ),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SIZE, size.toDouble(), MetricUnits.BYTES, metricDimensions)
            )
        }

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
                log.trace("$messageStr has unmapped placeholder(s) ${getPlaceHolders(topicName)} in topic name \"$topicName\", using $CONFIG_TOPIC_NAME \"${targetConfig.topicNameTemplate}\", using alternative $CONFIG_ALTERNATE_TOPIC_NAME \"${targetConfig.alternateTopicName}\"")
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

        /**
         * Creates a new instance of an AWS IoT target writer.
         * @param configReader ConfigReader Reader for reading the target configuration
         * @param targetID String ID of the target

         * @return TargetWriter Created AWS IoT target writer
         * @see TargetWriter,
         * @see AwsIotCoreTargetConfiguration
         * @see AwsIotCoreTargetWriter
         * @throws Exception
         */
        @JvmStatic
        fun newInstance(configReader: ConfigReader, targetID: String, logger: Logger, resultHandler: TargetResultHandler?): TargetWriter {

            return try {
                AwsIotCoreTargetWriter(targetID, configReader, logger, resultHandler)
            } catch (e: Throwable) {
                throw TargetException("Error creating AWS IoT core target writer, ${e.message}")
            }
        }

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
        )

        private const val AWS_IOT_CORE_MAX_PAYLOAD_SIZE = 128 * 1024
    }

}