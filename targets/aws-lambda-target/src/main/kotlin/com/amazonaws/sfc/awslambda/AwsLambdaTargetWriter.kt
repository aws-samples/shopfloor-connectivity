
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awslambda


import com.amazonaws.sfc.awslambda.config.AwsLambdaTargetConfiguration
import com.amazonaws.sfc.awslambda.config.AwsLambdaWriterConfiguration
import com.amazonaws.sfc.awslambda.config.AwsLambdaWriterConfiguration.Companion.AWS_LAMBDA
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_MESSAGES
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITES
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_DURATION
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_SIZE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_SUCCESS
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.targets.AwsServiceTargetClientHelper
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.byteCountString
import com.amazonaws.sfc.util.canNotReachAwsService
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import java.util.*

// AWS Lambda target
class AwsLambdaTargetWriter(
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

    private val clientHelper = AwsServiceTargetClientHelper(
        configReader.getConfig<AwsLambdaWriterConfiguration>(),
        targetID,
        LambdaClient.builder(),
        logger
    )
    private val lambdaClient: AwsLambdaClient
        get() = AwsLambdaClientWrapper(clientHelper.serviceClient as LambdaClient)

    private val targetDataChannel = Channel<TargetData>(100)

    private val scope = buildScope("Lambda Target")

    private val targetResults = if (resultHandler != null) TargetResultBufferedHelper(targetID, resultHandler, logger) else null
    private val buffer = TargetDataBuffer(storeFullMessage = false)

    private val config: AwsLambdaWriterConfiguration
        get() {
            return clientHelper.writerConfig(configReader, AWS_LAMBDA)
        }

    private val targetConfig: AwsLambdaTargetConfiguration by lazy {
        clientHelper.targetConfig(config, targetID, AWS_LAMBDA)
    }

    private val transformation by lazy { if (targetConfig.template != null) OutputTransformation(targetConfig.template!!, logger) else null }

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

        val log = logger.getCtxLoggers(AwsLambdaTargetWriter::class.java.simpleName, "writer")
        log.info("AWS Lambda writer for target \"$targetID\" calling lambda function \"${targetConfig.functionName}\" ${if (targetConfig.qualifier != null) " (${targetConfig.qualifier})}" else ""} in region ${targetConfig.region}")

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

        if (payload.length > LAMBDA_MAX_PAYLOAD_MSG_SIZE) {
            logger.getCtxErrorLog(className, "handleTargetData")("Target data with serial ${targetData.serial}, payload exceeds max Lambda payload size")
            targetResults?.error(targetData)
            return
        }

        // Check max payload size for lambda function, flush buffer
        if (payload.length + 1 + buffer.payloadSize > LAMBDA_MAX_PAYLOAD_MSG_SIZE) {
            flush()
        }

        targetResults?.add(targetData)
        buffer.add(targetData, payload)

        // flush if reached buffer size
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


    private fun flush() {

        val log = logger.getCtxLoggers(className, "flush")
        if (buffer.size == 0) {
            return
        }

        val functionName = targetConfig.functionName

        try {
            val payload = buildPayload()
            val request = buildInvokeRequest(functionName, payload)
            log.trace("Invoking Lambda function \"$functionName\" with payload\n$payload")

            val start = DateTime.systemDateTime().toEpochMilli()

            val resp = clientHelper.executeServiceCallWithRetries {
                try {
                    val r = lambdaClient.invoke(request)
                    targetResults?.ackBuffered()
                    val writeDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()
                    createMetrics(targetID, metricDimensions, payload.length, writeDurationInMillis)
                    r
                } catch (e: AwsServiceException) {
                    log.trace("Lambda invokeRequest error ${e.message}")
                    runBlocking { metricsCollector?.put(targetID, MetricsCollector.METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions) }
                    // Check the exception, it will throw an AwsServiceRetryableException if the error is recoverable
                    clientHelper.processServiceException(e)
                    // Non recoverable service exceptions
                    throw e
                }
            }

            log.trace("Lambda Invoke result is ${resp.sdkHttpResponse()?.statusCode()}")

        } catch (e: Throwable) {
            log.error("Error invoking function \"$functionName\" for target \"$targetID\", ${e.message}")
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
                              size: Int,
                              writeDurationInMillis: Double) {

        runBlocking {
            metricsCollector?.put(adapterID,
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITES, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_MESSAGES, buffer.size.toDouble(), MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_DURATION, writeDurationInMillis, MetricUnits.MILLISECONDS, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SIZE, size.toDouble(), MetricUnits.BYTES, metricDimensions))
        }
    }

    private fun buildPayload(): String {

        val log = logger.getCtxInfoLog(className, "buildPayload")
        val data = if (targetConfig.batchSize == 1)
            buffer.payloads[0]
        else {
            // if buffer contains multiple message send as an array
            buffer.payloads.joinToString(prefix = "[", postfix = "]", separator = ",")
        }

        return if (targetConfig.compressionType == CompressionType.NONE) data
        else {
            val compressedPayload = Compress.compressedDataPayload(targetConfig.compressionType, data, UUID.randomUUID().toString())
            val reduction = (100 - (compressedPayload.length.toFloat() / data.length.toFloat()) * 100).toInt()
            log("Used ${targetConfig.compressionType} compression to compress ${data.length.byteCountString} to ${compressedPayload.length.byteCountString}, $reduction% size reduction")
            compressedPayload
        }

    }


    private fun buildInvokeRequest(functionName: String?, payload: String): InvokeRequest {
        val request = InvokeRequest.builder().functionName(functionName)
            .payload(SdkBytes.fromUtf8String(payload))
            .invocationType("Event")
        if (targetConfig.qualifier != null) {
            request.qualifier(targetConfig.qualifier)
        }
        return request.build()
    }


    override suspend fun writeTargetData(targetData: TargetData) {
        targetDataChannel.send(targetData)

    }

    override suspend fun close() {
        flush()
        writer.cancel()
        lambdaClient.close()
    }

    companion object {
        const val LAMBDA_MAX_PAYLOAD_MSG_SIZE = (1024 * 256) - 2

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any?) =
            newInstance(createParameters[0] as ConfigReader, createParameters[1] as String, createParameters[2] as Logger, createParameters[3] as TargetResultHandler?)

        @JvmStatic
        fun newInstance(configReader: ConfigReader, targetID: String, logger: Logger, resultHandler: TargetResultHandler?): TargetWriter {
            return try {
                AwsLambdaTargetWriter(targetID, configReader, logger, resultHandler)
            } catch (e: Throwable) {
                throw TargetException("Error creating AWS SQS target writer, ${e.message}")
            }
        }

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET)

    }
}