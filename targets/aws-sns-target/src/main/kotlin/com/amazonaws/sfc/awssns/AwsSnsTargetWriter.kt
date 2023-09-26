/*
 Copyright (c) 2021. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.awssns


import com.amazonaws.sfc.awssns.config.AwsSnsTargetConfiguration
import com.amazonaws.sfc.awssns.config.AwsSnsWriterConfiguration
import com.amazonaws.sfc.awssns.config.AwsSnsWriterConfiguration.Companion.AWS_SNS
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
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.byteCountString
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishBatchRequest
import software.amazon.awssdk.services.sns.model.PublishBatchRequestEntry
import java.util.*

/**
 * AWS SNS Target writer
 * @property targetID String ID of target
 * @property configReader configReader for reading target configuration
 * @property logger Logger Logger for output
 * @see TargetWriter
 */
class AwsSnsTargetWriter(
    private val targetID: String,
    private val configReader: ConfigReader,
    private val logger: Logger,
    resultHandler: TargetResultHandler?) : TargetWriter {

    private val className = this::class.java.simpleName

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val metricDimensions = mapOf(METRICS_DIMENSION_SOURCE to targetID,
        MetricsCollector.METRICS_DIMENSION_TYPE to className)

    private val clientHelper =
        AwsServiceTargetClientHelper(
            configReader.getConfig<AwsSnsWriterConfiguration>(),
            targetID,
            SnsClient.builder(),
            logger)

    private val snsClient: AwsSnsClient
        get() = AwsSnsClientWrapper(clientHelper.serviceClient as SnsClient)

    private val targetResults = if (resultHandler != null) TargetResultHelper(targetID, resultHandler, logger) else null
    private val scope = buildScope("Sns Target")

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

    /**
     * Writes message to SNS target.
     * @param targetData TargetData
     */
    override suspend fun writeTargetData(targetData: TargetData) {
        targetDataChannel.send(targetData)
    }

    override suspend fun close() {
        flush()
        writer.cancel()
        snsClient.close()
    }

    // channel for passing messages to coroutine that sends messages to SNS queue
    private val targetDataChannel = Channel<TargetData>(100)

    // buffer for message batches
    private val buffer = newTargetDataBuffer(resultHandler)

    // coroutine that writes messages to queue
    private val writer = scope.launch("Writer") {
        val log = logger.getCtxLoggers(AwsSnsTargetWriter::class.java.simpleName, "writer")

        log.info("AWS SNS writer for target \"$targetID\" sending to topic \"${targetConfig.topicArn}\" in region ${targetConfig.region}")

        var timer = timerJob()

        while (isActive) {

            select<Unit> {
                targetDataChannel.onReceive { targetData ->

                    timer.cancel()
                    handleTargetData(targetData)
                }

                timer.onJoin {
                    log.trace("${(targetConfig.interval.inWholeMilliseconds)} milliseconds buffer interval reached, flushing buffer")
                    flush()
                }
            }

            timer = timerJob()
        }
    }

    private fun handleTargetData(targetData: TargetData) {
        val payload = buildPayload(targetData)

        if (payload.length > SNS_MAX_BATCH_MSG_SIZE) {
            logger.getCtxErrorLog(className, "handleTargetData")("Message with serial ${targetData.serial} exceeds max SNS message size")
            targetResults?.error(targetData)
        } else {
            // check for max message size
            if (payload.length + buffer.payloadSize > SNS_MAX_BATCH_MSG_SIZE) {
                flush()
            }
            buffer.add(targetData, payload)

            // flush if buffer is full
            if (targetData.noBuffering || buffer.size >= targetConfig.batchSize) {
                flush()
            }
        }
    }


    private fun CoroutineScope.timerJob(): Job {
        return launch("Timeout timer") {

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


    // writes all buffered messages to SNS queue
    private fun flush() {

        val log = logger.getCtxLoggers(className, "flush")

        // No buffered messages, nothing to do
        if (buffer.size == 0) {
            return
        }

        val topicArn = targetConfig.topicArn
        log.trace("Sending \"$topicArn\"\n${buffer.payloads.size} items to $topicArn")

        val start = DateTime.systemDateTime().toEpochMilli()

        try {
            val request = buildSnsPublishBatchRequest(topicArn)

            val resp = clientHelper.executeServiceCallWithRetries {
                try {
                    val r = snsClient.publishBatch(request)
                    val writeDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()
                    createMetrics(targetID, metricDimensions, writeDurationInMillis)
                    r
                } catch (e: AwsServiceException) {

                    log.trace("SNS publishBatch error ${e.message}")

                    // Check the exception, it will throw an AwsServiceRetryableException if the error is recoverable
                    clientHelper.processServiceException(e)

                    // Non recoverable service exceptions
                    throw e
                }
            }

            if (resp.hasFailed() && resp.failed().size > 0) {
                val errLog = logger.getCtxErrorLog(className, "flush")
                val okSerials = resp.successful().map { it.id() }
                val ok = buffer.items.filter { okSerials.contains(it.serial) }
                val errorsSerials = resp.failed().map {
                    errLog("Error sending message ${it.id()} of batch, ${it.code()}")
                    it.id()
                }
                val errors = buffer.items.filter { errorsSerials.contains(it.serial) }
                targetResults?.ackAndError(ok, errors)
            }

            log.trace("PublishBatch result is ${resp.sdkHttpResponse()?.statusCode()}")


        } catch (e: Exception) {
            log.error("Error sending to topic \"$topicArn\" for target \"$targetID\", ${e.message}")
            runBlocking { metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions) }
        } finally {
            buffer.clear()
        }
    }

    private fun createMetrics(adapterID: String,
                              metricDimensions: MetricDimensions,
                              writeDurationInMillis: Double) {

        runBlocking {
            metricsCollector?.put(adapterID,
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITES, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_MESSAGES, buffer.size.toDouble(), MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_DURATION, writeDurationInMillis, MetricUnits.MILLISECONDS, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SIZE, buffer.payloadSize.toDouble(), MetricUnits.BYTES, metricDimensions))
        }
    }


    // build SNS batch publish request
    private fun buildSnsPublishBatchRequest(topicArn: String?) =
        PublishBatchRequest.builder()
            .topicArn(topicArn)
            .publishBatchRequestEntries(buffer.serials.mapIndexed { index, serial ->
                buildPublishBatchRequestEntry(index, serial)
            })
            .build()

    // builds a batch send request for all buffered messages
    private fun buildPublishBatchRequestEntry(index: Int, serial: String?): PublishBatchRequestEntry {

        val message = buffer.payloads[index]

        val builder = PublishBatchRequestEntry.builder()
            .message(
                if (targetConfig.compressionType == CompressionType.NONE)
                    message
                else {
                    val log = logger.getCtxInfoLog(className, "buildPublishBatchRequestEntry")

                    val compressedPayload = Compress.compressedDataPayload(targetConfig.compressionType, message, UUID.randomUUID().toString())
                    val reduction = (100 - (compressedPayload.length.toFloat() / message.length.toFloat()) * 100).toInt()
                    log("Used ${targetConfig.compressionType} compression to compress ${message.length.byteCountString} to ${compressedPayload.length.byteCountString}, $reduction% size reduction")
                    compressedPayload
                }

            )
            .id(index.toString())


        if (targetConfig.serialAsMessageDeduplicationId)
            builder.messageDeduplicationId(serial)


        if (targetConfig.messageGroupId != null) {
            builder.messageGroupId(targetConfig.messageGroupId)
        }

        if (targetConfig.subject != null) {
            builder.subject(targetConfig.subject)
        }

        return builder.build()

    }

    private val config: AwsSnsWriterConfiguration
        get() {
            return clientHelper.writerConfig(configReader, AWS_SNS)
        }

    private var _targetConfig: AwsSnsTargetConfiguration? = null
    private val targetConfig: AwsSnsTargetConfiguration
        get() {
            if (_targetConfig == null) {
                _targetConfig = clientHelper.targetConfig(config, targetID, AWS_SNS)
            }
            return _targetConfig as AwsSnsTargetConfiguration
        }


    companion object {
        const val SNS_MAX_BATCH_MSG_SIZE = 1024 * 256

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any?) =
            newInstance(createParameters[0] as ConfigReader, createParameters[1] as String, createParameters[2] as Logger, createParameters[3] as TargetResultHandler?)

        /**
         * Creates new instance of AWS SNS target from configuration.
         * @param configReader ConfigReader Reader for reading configuration for target instance
         * @see AwsSnsWriterConfiguration
         * @param targetID String ID of the target
         * @param logger Logger Logger for output
         * @return TargetWriter Created target
         * @throws Exception
         */
        @JvmStatic
        fun newInstance(configReader: ConfigReader, targetID: String, logger: Logger, resultHandler: TargetResultHandler?): TargetWriter {
            return try {
                AwsSnsTargetWriter(targetID, configReader, logger, resultHandler)
            } catch (e: Throwable) {
                throw TargetException("Error creating AWS SNS target writer, ${e.message}")
            }
        }

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET)


    }
}











