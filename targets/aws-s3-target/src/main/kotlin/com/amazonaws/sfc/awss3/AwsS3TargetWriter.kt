
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awss3


import com.amazonaws.sfc.awss3.config.AwsS3TargetConfiguration
import com.amazonaws.sfc.awss3.config.AwsS3WriterConfiguration
import com.amazonaws.sfc.awss3.config.AwsS3WriterConfiguration.Companion.AWS_S3
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
import com.amazonaws.sfc.system.DateTime.systemCalendarUTC
import com.amazonaws.sfc.targets.AwsServiceTargetClientHelper
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.byteCountString
import com.amazonaws.sfc.util.canNotReachAwsService
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.ByteArrayOutputStream
import java.util.*


// AWS S3 target
class AwsS3TargetWriter(
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
        AwsServiceTargetClientHelper(configReader.getConfig<AwsS3WriterConfiguration>(),
            targetID,
            S3Client.builder(),
            logger)

    private val s3Client: AwsS3Client
        get() = AwsS3ClientWrapper(clientHelper.serviceClient as S3Client)

    private val targetDataChannel = Channel<TargetData>(100)

    private val scope = buildScope("S3 Target")
    private val targetResults = if (resultHandler != null) TargetResultBufferedHelper(targetID, resultHandler, logger) else null
    private val buffer = TargetDataBuffer(storeFullMessage = false)

    private val config: AwsS3WriterConfiguration
        get() {
            return clientHelper.writerConfig(configReader, AWS_S3)
        }

    private val targetConfig: AwsS3TargetConfiguration by lazy {
        clientHelper.targetConfig(config, targetID, AWS_S3)
    }

    private val transformation by lazy {
        if (targetConfig.template != null) OutputTransformation(targetConfig.template!!, logger) else null
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

    private fun buildPayload(targetData: TargetData): String =
        if (transformation == null) targetData.toJson(config.elementNames) else transformation!!.transform(targetData, config.elementNames) ?: ""

    private val writer = scope.launch("Writer") {

        var timer = timerJob()

        val loggers = logger.getCtxLoggers(AwsS3TargetWriter::class.java.simpleName, "writer")
        loggers.info("AWS S3 writer for target \"$targetID\" writing to S3 bucket \"${targetConfig.bucketName}\"  in region ${targetConfig.region}")

        while (isActive) {
            select<Unit> {
                targetDataChannel.onReceive { targetData ->
                    val payload = buildPayload(targetData)

                    targetResults?.add(targetData)
                    buffer.add(targetData, payload)

                    loggers.trace("Received message, buffer size is ${buffer.payloadSize.byteCountString}")

                    // flush if reached buffer size
                    if (targetData.noBuffering || buffer.payloadSize >= targetConfig.bufferSize) {
                        loggers.trace("${targetConfig.bufferSize.byteCountString}  buffer size reached, flushing buffer")
                        timer.cancel()
                        flush()
                        timer = timerJob()

                    }
                }
                timer.onJoin {
                    loggers.trace("${targetConfig.interval / 1000} seconds buffer interval reached, flushing buffer")
                    flush()
                    timer = timerJob()
                }
            }
        }

    }

    private fun flush() {

        val log = logger.getCtxLoggers(className, "flush")
        if (buffer.size == 0) {
            return
        }

        val bucketName = targetConfig.bucketName
        log.trace("Writing data to bucket \"$bucketName\"")

        val start = DateTime.systemDateTime().toEpochMilli()

        try {

            val request = buildPutObjectRequest()
            val content = buildContent(request.key())
            val resp = clientHelper.executeServiceCallWithRetries {
                try {
                    log.info("Creating S3 object ${request.key()} containing ${content.optionalContentLength().get().byteCountString}")
                    val resp = s3Client.putObject(request, content)
                    targetResults?.ackBuffered()

                    val writeDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()
                    createMetrics(targetID, metricDimensions, writeDurationInMillis)

                    resp
                } catch (e: AwsServiceException) {
                    log.trace("S3 putObject error ${e.message}")
                    // Check the exception, it will throw an AwsServiceRetryableException if the error is recoverable
                    clientHelper.processServiceException(e)
                    // Non recoverable service exceptions
                    throw e
                }
            }

            log.trace("S3  putObject result is ${resp.sdkHttpResponse()?.statusCode()}")

        } catch (e: Throwable) {
            log.error("Error writing to bucket \"$bucketName\" for target \"$targetID\", ${e.message}")
            runBlocking { metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions) }

            if (canNotReachAwsService(e)) {
                targetResults?.nackBuffered()
            } else {
                targetResults?.errorBuffered()
            }
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

    private fun CoroutineScope.timerJob() = launch("Timeout timer") {
        try {
            delay(targetConfig.interval.toLong())
        } catch (e: Exception) {
            // no harm done, timer is just used to guard for timeouts
        }
    }

    private fun buildContent(key: String): RequestBody {
        val content: String = buffer.payloads.joinToString(prefix = "[", postfix = "]", separator = ",")

        return if (targetConfig.compressionType == CompressionType.NONE) RequestBody.fromString(content) else {
            val compressedData = compressContent(content, key)
            RequestBody.fromBytes(compressedData)
        }
    }

    private fun compressContent(content: String, key: String): ByteArray {
        val inputStream = content.byteInputStream(Charsets.UTF_8)
        val outputStream = ByteArrayOutputStream(2048)
        Compress.compress(targetConfig.compressionType, inputStream, outputStream, entryName = key.split("/").last())
        val info = logger.getCtxInfoLog(className, "buildContent")
        val compressedData = outputStream.toByteArray()
        info("Used ${targetConfig.compressionType} compression to compress ${content.length.byteCountString} to ${compressedData.size.byteCountString} bytes, ${(100 - (compressedData.size.toFloat() / content.length.toFloat()) * 100).toInt()}% size reduction")
        return compressedData
    }


    private fun buildPutObjectRequest(): PutObjectRequest {
        val request = PutObjectRequest.builder().bucket(targetConfig.bucketName).key(objectKey())
        if (targetConfig.contentType != null) {
            request.contentType(targetConfig.compressionType.mimeType)
        } else {
            if (targetConfig.compressionType != CompressionType.NONE) {
                request.contentType(targetConfig.compressionType.mimeType)
            }
        }
        return request.build()
    }

    private fun objectKey(): String {
        val now = systemCalendarUTC()
        val key =
            "${now.get(Calendar.YEAR)}/${now.get(Calendar.MONTH) + 1}/${now.get(Calendar.DAY_OF_MONTH)}/${now.get(Calendar.HOUR_OF_DAY)}/${now.get(Calendar.MINUTE)}/${UUID.randomUUID()}"
        return if (targetConfig.prefix.isBlank()) key else "${targetConfig.prefix}/$key"
    }


    override suspend fun writeTargetData(targetData: TargetData) {
        targetDataChannel.send(targetData)

    }

    override suspend fun close() {
        flush()
        writer.cancel()
        s3Client.close()
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any?) =
            newInstance(createParameters[0] as ConfigReader, createParameters[1] as String, createParameters[2] as Logger, createParameters[3] as TargetResultHandler?)

        /**
         * Creates an instance of an AWS S3 writer from the passed configuration
         * @param configReader ConfigReader Reads the configuration for the writer
         * @see AwsS3WriterConfiguration
         * @param targetID String ID of the target
         * @param logger Logger Logger for output
         * @return TargetWriter
         * @throws Exception
         */
        @JvmStatic
        fun newInstance(configReader: ConfigReader, targetID: String, logger: Logger, resultHandler: TargetResultHandler?): TargetWriter {
            return try {
                AwsS3TargetWriter(targetID, configReader, logger, resultHandler)
            } catch (e: Throwable) {
                throw TargetException("Error creating AWS S3 target writer, ${e.message}")
            }
        }

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET)


    }
}