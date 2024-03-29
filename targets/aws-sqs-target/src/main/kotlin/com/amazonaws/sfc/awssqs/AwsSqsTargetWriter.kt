// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awssqs


import com.amazonaws.sfc.awssqs.config.AwsSqsTargetConfiguration
import com.amazonaws.sfc.awssqs.config.AwsSqsWriterConfiguration
import com.amazonaws.sfc.awssqs.config.AwsSqsWriterConfiguration.Companion.AWS_SQS
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.data.TargetDataBuffer.Companion.newTargetDataBuffer
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
import com.amazonaws.sfc.targets.AwsServiceTargetClientHelper
import com.amazonaws.sfc.targets.TargetDataChannel
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse
import java.util.*

/**
 * AWS SQS Target writer
 * @property targetID String ID of target
 * @property configReader configReader for reading target configuration
 * @property logger Logger Logger for output
 * @see TargetWriter
 */
class AwsSqsTargetWriter(
    private val targetID: String,
    private val configReader: ConfigReader,
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

    private val clientHelper =
        AwsServiceTargetClientHelper(
            configReader.getConfig<AwsSqsWriterConfiguration>(),
            targetID,
            SqsClient.builder(),
            logger
        )

    private val targetResults = if (resultHandler != null) TargetResultHelper(targetID, resultHandler, logger) else null

    private val sqsClient: AwsSqsClient
        get() = AwsSqsClientWrapper(clientHelper.serviceClient as SqsClient)

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
        if (metricsCollector != null)
            InProcessMetricsProvider(metricsCollector!!, logger) else null
    }

    private val scope = buildScope("Sqs Target")

    override suspend fun writeTargetData(targetData: TargetData) {
        targetDataChannel.submit(targetData, logger.getCtxLoggers(className, "writeTargetData"))
    }

    override suspend fun close() {
        flush()
        writer.cancel()
        sqsClient.close()
    }

    private var _targetConfig: AwsSqsTargetConfiguration? = null
    private val targetConfig: AwsSqsTargetConfiguration
        get() {
            if (_targetConfig == null) {
                _targetConfig = clientHelper.targetConfig(config, targetID, AWS_SQS)
            }
            return _targetConfig as AwsSqsTargetConfiguration
        }

    // channel for passing messages to coroutine that sends messages to SQS queue
    private val targetDataChannel = TargetDataChannel.create(targetConfig, "$className:targetDataChannel")


    private val buffer by lazy {
        newTargetDataBuffer(resultHandler)
    }


    // coroutine that writes messages to queue
    private val writer = scope.launch(context = Dispatchers.IO, name = "Writer") {
        val log = logger.getCtxLoggers(AwsSqsTargetWriter::class.java.simpleName, "writer")

        log.info("AWS SQS writer for target \"$targetID\" sending to queue \"${targetConfig.queueUrl}\" in region ${targetConfig.region}")

        var timer = timerJob()

        while (isActive) {
            try {
                select {

                    targetDataChannel.onReceive { targetData ->
                        timer.cancel()
                        // flush buffer is the serial is already in the buffer
                        handleTargetData(targetData)
                    }
                    timer.onJoin {
                        log.trace("${(targetConfig.interval.inWholeMilliseconds)} milliseconds buffer interval reached, flushing buffer")
                        flush()
                    }
                }
                timer = timerJob()

            } catch (e: Exception) {
                if (!e.isJobCancellationException)
                    log.errorEx("Error in writer", e)
            }
        }
    }

    private fun handleTargetData(targetData: TargetData) {
        if (buffer.serials.contains(targetData.serial)) {
            flush()
            return
        }

        val payload = buildPayload(targetData)

        if (payload.length > SQS_MAX_BATCH_MSG_SIZE) {
            logger.getCtxErrorLog(className, "sendTargetData")("Message exceeds max SQS message size")
            targetResults?.error(targetData)
            return
        }

        // check for max message size
        if (payload.length + buffer.payloadSize > SQS_MAX_BATCH_MSG_SIZE) {
            flush()
        }

        buffer.add(targetData, payload)
        if (targetData.noBuffering || buffer.size >= targetConfig.batchSize) {
            flush()
        }
    }

    private fun CoroutineScope.timerJob(): Job {
        return launch(context = Dispatchers.Default, name = "Timeout timer") {

            return@launch try {
                delay(targetConfig.interval)
            } catch (e: Exception) {
                // no harm done, timer is just used to guard for timeouts
            }
        }
    }

    private val transformation by lazy { if (targetConfig.template != null) OutputTransformation(targetConfig.template!!, logger) else null }

    private fun buildPayload(targetData: TargetData): String =
        if (transformation == null) targetData.toJson(config.elementNames) else transformation!!.transform(targetData, config.elementNames) ?: ""


    // writes all buffered messages to SQS queue
    private fun flush() {

        val log = logger.getCtxLoggers(className, "flush")

        // No buffered messages, nothing to do
        if (buffer.size == 0) {
            return
        }

        val queueUrl = targetConfig.queueUrl
        log.trace("Sending ${buffer.payloads.size} data items to queue \"$queueUrl\", payload size is ${buffer.payloads.sumOf { it.length }.byteCountString}")

        val start = DateTime.systemDateTime().toEpochMilli()


        try {
            val request = buildSqsSendRequest(queueUrl)

            val resp = clientHelper.executeServiceCallWithRetries {
                try {
                    val rsp = sqsClient.sendMessageBatch(request)
                    val writeDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()
                    createMetrics(targetID, metricDimensions, writeDurationInMillis)
                    rsp

                } catch (e: AwsServiceException) {

                    log.trace("SQS sendMessageBatch error ${e.message}")

                    // Check the exception, it will throw an AwsServiceRetryableException if the error is recoverable
                    clientHelper.processServiceException(e)

                    // Non recoverable service exceptions
                    throw e
                }
            }

            log.trace("SendMessageBatch result is ${resp.sdkHttpResponse()?.statusCode()}")

            reportResults(resp, log.error)

        } catch (e: Exception) {
            log.errorEx("Error sending to queue \"$queueUrl\" for target \"$targetID\"", e)
            runBlocking { metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions) }
            if (canNotReachAwsService(e)) {
                targetResults?.nackList(buffer.items)
            } else {
                targetResults?.errorList(buffer.items)
            }
        } finally {
            buffer.clear()
        }
    }


    private fun createMetrics(
        adapterID: String,
        metricDimensions: MetricDimensions,
        writeDurationInMillis: Double
    ) {

        runBlocking {
            metricsCollector?.put(
                adapterID,
                metricsCollector?.buildValueDataPoint(adapterID, MetricsCollector.METRICS_MEMORY, MemoryMonitor.getUsedMemoryMB().toDouble(),MetricUnits.MEGABYTES ),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITES, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_MESSAGES, buffer.size.toDouble(), MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_DURATION, writeDurationInMillis, MetricUnits.MILLISECONDS, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SIZE, buffer.payloadSize.toDouble(), MetricUnits.BYTES, metricDimensions)
            )
        }

    }

    private fun reportResults(resp: SendMessageBatchResponse?, errorLog: (String) -> Unit) {

        if (targetResults == null) return

        // get list of delivered messages
        val ackSerials = resp?.successful()?.map { it.id() } ?: emptyList()
        val ackItems = buffer.items.filter { ackSerials.contains(it.serial) }
        if (ackItems.size == buffer.size) {
            // all OK, just ack
            targetResults.ack(ackItems)
        } else {
            // get rejected messages
            val errorSerials = resp?.failed()?.map {
                errorLog("Error sending target data with serial ${it.id()}, ${it.message()}")
                it.id()
            } ?: emptyList()
            val errorItems = buffer.items.filter { errorSerials.contains(it.serial) }

            // report delivered and rejected messages
            targetResults.ackAndError(ackItems.toList(), errorItems.toList())
        }
    }


    // build SQS send request
    private fun buildSqsSendRequest(queueUrl: String?) =
        SendMessageBatchRequest.builder().queueUrl(queueUrl)
            .entries(buffer.serials.mapIndexed { index, serial ->
                SendMessageBatchRequestEntry.builder()
                    .id(serial)
                    .messageBody(buildMessageBody(index))
                    .build()

            })
            .build()

    private fun buildMessageBody(index: Int): String {
        val message = buffer.payloads[index]
        return if (targetConfig.compressionType == CompressionType.NONE) message
        else {

            val log = logger.getCtxInfoLog(className, "buildPayload")
            val compressedPayload = Compress.compressedDataPayload(targetConfig.compressionType, message, UUID.randomUUID().toString())

            val reduction = (100 - (compressedPayload.length.toFloat() / message.length.toFloat()) * 100).toInt()
            log("Used ${targetConfig.compressionType} compression to compress ${message.length.byteCountString} to ${compressedPayload.length.byteCountString}, $reduction% size reduction")
            compressedPayload
        }
    }


    private val config: AwsSqsWriterConfiguration
        get() {
            return clientHelper.writerConfig(configReader, AWS_SQS)
        }

    companion object {
        const val SQS_MAX_BATCH_MSG_SIZE = 1024 * 256

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
                AwsSqsTargetWriter(targetID, configReader, logger, resultHandler)
            } catch (e: Throwable) {
                throw TargetException("Error creating AWS SQS target writer, ${e.message}")
            }
        }

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
        )
    }
}











