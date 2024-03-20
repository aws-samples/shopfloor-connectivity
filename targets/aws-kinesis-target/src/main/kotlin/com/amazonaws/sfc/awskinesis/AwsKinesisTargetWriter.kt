// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awskinesis


import com.amazonaws.sfc.awskinesis.config.AwsKinesisTargetConfiguration
import com.amazonaws.sfc.awskinesis.config.AwsKinesisWriterConfiguration
import com.amazonaws.sfc.awskinesis.config.AwsKinesisWriterConfiguration.Companion.AWS_KINESIS_TARGET
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
import com.amazonaws.sfc.util.MemoryMonitor
import com.amazonaws.sfc.util.canNotReachAwsService
import com.amazonaws.sfc.util.isJobCancellationException
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.kinesis.KinesisClient
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequestEntry
import java.io.ByteArrayOutputStream
import java.util.*


/**
 * AWS Kinesis target implementation.
 * @property targetID String ID of the target
 * @property configReader configReader for reading target configuration
 * @property logger Logger Logger for output
 */
class AwsKinesisTargetWriter(
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

    private val clientHelper = AwsServiceTargetClientHelper(
        configReader.getConfig<AwsKinesisWriterConfiguration>(),
        targetID,
        KinesisClient.builder(),
        logger
    )

    private val kinesisClient: AwsKinesisClient
        get() = AwsKinesisClientWrapper(clientHelper.serviceClient as KinesisClient)

    private val targetResults = if (resultHandler != null) TargetResultHelper(targetID, resultHandler, logger) else null
    private val scope = CoroutineScope(Dispatchers.Default) + CoroutineName("Kinesis Target")

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
                } catch (e: Exception) {
                    logger.getCtxErrorLogEx(this::class.java.simpleName, "collectMetricsFromLogger")("Error collecting metrics from logger", e)
                }
            }
        } else null

    override val metricsProvider: MetricsProvider?
        get() = if (metricsCollector != null) InProcessMetricsProvider(metricsCollector!!, logger) else null

    /**
     * Writes a message to the Kinesis target.
     * @param targetData TargetData
     */
    override suspend fun writeTargetData(targetData: TargetData) {
        targetDataChannel.submit(targetData, logger.getCtxLoggers(className, "writeTargetData"))
    }


    /**
     * Closes the target.
     */
    override suspend fun close() {
        flush()
        writer.cancel()
        kinesisClient.close()
    }

    private val targetConfig: AwsKinesisTargetConfiguration
        get() {
            return clientHelper.targetConfig(
                config,
                targetID,
                AWS_KINESIS_TARGET
            )
        }

    // channel to pass messages to coroutine that sends data to the Kinesis stream
    private val targetDataChannel = TargetDataChannel.create(targetConfig, "$className:targetDataChannel")

    // buffer for batching messages
    private val buffer = newTargetDataBuffer(resultHandler)


    // coroutine writing messages to stream
    private val writer = scope.launch("Writer") {
        val log = logger.getCtxLoggers(AwsKinesisTargetWriter::class.java.simpleName, "writer")
        log.info("AWS Kinesis writer for target \"$targetID\" writing to stream \"${targetConfig.streamName}\" in region ${targetConfig.region} on target \"$targetID\"")

        var timer = timerJob()

        while (isActive) {
            try {
                select {
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
            } catch (e: Exception) {
                if (!e.isJobCancellationException)
                    log.errorEx("Error in writer", e)
            }
        }
    }


    private fun handleTargetData(targetData: TargetData) {
        val payload = buildPayload(targetData)
        // test if buffer (size) can hold message, if not first flush buffer
        if (buffer.size + payload.length > KINESIS_MAX_BATCH_MSG_SIZE) {
            flush()
        }
        buffer.add(targetData, payload)
        // flush buffer if full
        if (targetData.noBuffering || buffer.size >= targetConfig.batchSize) {
            flush()
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


    // writes al buffered messages to the stream
    private fun flush() {

        val log = logger.getCtxLoggers(className, "flush")

        // no data
        if (buffer.size == 0) {
            return
        }

        val streamName = targetConfig.streamName
        log.info("Sending ${buffer.size} record to kinesis stream \"$streamName\"")

        val start = DateTime.systemDateTime().toEpochMilli()

        try {
            val request = buildRequest(streamName)
            val resp = clientHelper.executeServiceCallWithRetries {
                try {
                    val resp = kinesisClient.putRecords(request)

                    val writeDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()
                    createMetrics(targetID, metricDimensions, writeDurationInMillis)

                    resp
                } catch (e: AwsServiceException) {

                    log.trace("Kinesis putRecords error ${e.message}")

                    // Check the exception, it will throw an AwsServiceRetryableException if the error is recoverable
                    clientHelper.processServiceException(e)

                    // Non recoverable service exceptions
                    throw e
                }
            }

            log.trace("putRecords result is status ${resp.sdkHttpResponse()?.statusCode()}, failed records ${resp.failedRecordCount()}")

            val ok = mutableListOf<TargetDataSerialMessagePair>()
            val errors = mutableListOf<TargetDataSerialMessagePair>()
            resp.records()?.forEachIndexed { i, rec ->
                if (rec.errorMessage().isNullOrBlank()) {
                    ok.add(buffer.items[i])
                } else {
                    log.error("record : ${rec.sequenceNumber()}, error : ${rec.errorMessage()}, error code : ${rec.errorCode()}")
                    errors.add(buffer.items[i])
                }
            }
            targetResults?.ackAndError(ok, errors)

        } catch (e: Exception) {
            log.errorEx("Error sending to kinesis  stream \"$streamName\" for target \"$targetID\"", e)
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

    // builds put records request for all buffered messages
    private fun buildRequest(streamName: String?) =
        PutRecordsRequest.builder()
            .streamName(streamName)
            .records(List(buffer.items.size) { i ->
                PutRecordsRequestEntry.builder()
                    .partitionKey("$i")
                    .data(
                        if (targetConfig.compressionType == CompressionType.NONE) {
                            SdkBytes.fromUtf8String(buffer.payloads[i])
                        } else {
                            val compressedData = ByteArrayOutputStream(2048)
                            Compress.compress(
                                targetConfig.compressionType, buffer.payloads[i].byteInputStream(), compressedData, entryName = UUID.randomUUID()
                                    .toString()
                            )
                            SdkBytes.fromByteArray(compressedData.toByteArray())
                        }
                    )
                    .build()
            })
            .build()

    private val config: AwsKinesisWriterConfiguration
        get() = clientHelper.writerConfig(configReader, AWS_KINESIS_TARGET)


    companion object {
        const val KINESIS_MAX_BATCH_MSG_SIZE = 1024 * 1024 * 5

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
         * Creates Kinesis target writer instance from configuration.
         * @param configReader ConfigReader Reader for target configuration
         * @param targetID String ID of the target
         * @param logger Logger Logger for output
         * @param resultHandler Callback for target result
         * @return TargetWriter Created Kinesis target writer
         */
        @JvmStatic
        fun newInstance(configReader: ConfigReader, targetID: String, logger: Logger, resultHandler: TargetResultHandler?): TargetWriter {
            return try {
                AwsKinesisTargetWriter(targetID, configReader, logger, resultHandler)
            } catch (e: Throwable) {
                throw TargetException("Error creating AWS Kinesis target writer, ${e.message}")
            }
        }

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
        )

    }
}