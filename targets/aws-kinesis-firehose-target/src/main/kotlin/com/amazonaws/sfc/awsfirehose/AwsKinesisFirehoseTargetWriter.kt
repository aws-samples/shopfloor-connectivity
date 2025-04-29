// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsfirehose


import com.amazonaws.sfc.awsfirehose.config.AwsFirehoseWriterConfiguration
import com.amazonaws.sfc.awsfirehose.config.AwsFirehoseWriterConfiguration.Companion.AWS_KINESIS_FIREHOSE
import com.amazonaws.sfc.awsfirehose.config.AwsKinesisFirehoseTargetConfiguration
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
import com.amazonaws.sfc.targets.*
import com.amazonaws.sfc.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.firehose.FirehoseClient
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest
import software.amazon.awssdk.services.firehose.model.Record

/**

 * Implements AWS Kinesis firehose target.
 * @property targetID String ID of the target
 * @property configReader configReader for reading target configuration
 * @property logger Logger Logger for output
 * @see TargetWriter
 */
class AwsKinesisFirehoseTargetWriter(
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
        configReader.getConfig<AwsFirehoseWriterConfiguration>(),
        targetID,
        FirehoseClient.builder(),
        logger
    )

    private val firehoseClient: AwsFirehoseClient
        get() = AwsFirehoseClientWrapper(clientHelper.serviceClient as FirehoseClient)

    private val targetResults = if (resultHandler != null) TargetResultHelper(targetID, resultHandler, logger) else null
    private val scope = CoroutineScope(Dispatchers.IO) + buildContext("Kinesis Firehose Target")

    /**
     * Writes a message to publish to a Kinesis Firehose stream.
     * @param targetData TargetData
     */
    override suspend fun writeTargetData(targetData: TargetData) {
        targetDataChannel.submit(targetData, logger.getCtxLoggers(className, "writeTargetData"))
    }

    /**
     * Closes the writer.
     */
    override suspend fun close() {
        flush()
        writer.cancel()
        firehoseClient.close()
    }

    private val targetConfig: AwsKinesisFirehoseTargetConfiguration by lazy {
        clientHelper.targetConfig(config, targetID, AWS_KINESIS_FIREHOSE)
    }

    // Channel to pass message to coroutine that batches and sends messages to the stream
    private val targetDataChannel = TargetDataChannel.create(targetConfig, "$className:targetDataChannel")

    private val formatter: TargetFormatter? by lazy {
        TargetFormatterFactory.createTargetFormatter(configReader, targetID, targetConfig, logger)
    }

    private val customPayload = (formatter != null)


    // Message buffer
    private val buffer = newTargetDataBuffer(resultHandler, customPayload)


    // Coroutine that sends the messages to the stream
    private val writer = scope.launch(context = Dispatchers.IO, name = "Writer") {

        val log = logger.getCtxLoggers(className, "writer")
        try {
            log.info("AWS Kinesis Firehose writer for target \"$targetID\" writing to stream \"${targetConfig.streamName}\" in region ${targetConfig.region} on target \"$targetID\"")

            for (targetData in targetDataChannel.channel) {
                sendTargetData(targetData)
            }
        } catch (e: Exception) {
            if (!e.isJobCancellationException)
                log.errorEx(
                    "Error in AWS Kinesis Firehose writer for target \"$targetID\" writing to stream \"${targetConfig.streamName}\" in region ${targetConfig.region} on target \"$targetID\"",
                    e)
        }
    }

    private val transformation by lazy { if (targetConfig.template != null) OutputTransformation(targetConfig.template!!, logger) else null }

    private fun buildPayload(targetData: TargetData): String =
        if (transformation == null) targetData.toJson(config.elementNames, targetConfig.unquoteNumericJsonValues) else transformation!!.transform(
            targetData,
            config.elementNames,
            targetConfig.templateEpochTimestamp) ?: ""

    // Send messages to stream
    private fun sendTargetData(targetData: TargetData) {

        val log = logger.getCtxLoggers(className, "sendTargetData")

        val (messagePayload: String?, payloadSize: Int) = if (customPayload) {
            val formattedItemSize = try {
                formatter?.itemPayloadSize(targetData) ?: altSize(targetData)
            } catch (e: Exception) {
                log.errorEx("Error getting payload size using custom formatter", e)
                altSize(targetData)
            }
            null to (formattedItemSize * 1.33).toInt() + 2
        } else {
            val messagePayload = buildPayload(targetData)
            messagePayload to (messagePayload.length * 1.33).toInt() + 2
        }

        // test if buffer can hold additional payload, if not flush buffer
        if (payloadSize + buffer.payloadSize > FIREHOSE_MAX_BATCH_MSG_SIZE) {
            flush()
        }

        if (customPayload) {
            buffer.add(targetData, payloadSize)
        } else {
            buffer.add(targetData, messagePayload)
        }

        // flush if number of messages greater than configured batch size
        if (targetData.noBuffering || buffer.size >= targetConfig.batchSize) {
            flush()
        }
    }

    private fun altSize(targetData: TargetData): Int = if (targetConfig.batchSize != 0) 0 else targetData.toJson(config.elementNames, targetConfig.unquoteNumericJsonValues).length


    // Send all messages in buffer to the stream
    private fun flush() {

        val log = logger.getCtxLoggers(object {})
        if (buffer.size == 0) {
            return
        }

        val streamName = targetConfig.streamName
        log.trace("Sending ${buffer.size} records to kinesis firehose delivery stream \"$streamName\" on target \"$targetID\"")

        val start = DateTime.systemDateTime().toEpochMilli()
        val request = buildRequest(streamName)

        try {

            if (request.records().isEmpty()){
                log.error("No records to send to kinesis firehose delivery stream \"$streamName\" on target \"$targetID\"")
                targetResults?.errorList(buffer.items)
                return
            }

            val resp = clientHelper.executeServiceCallWithRetries {
                try {
                    val resp = firehoseClient.putRecordBatch(request)
                    targetResults?.ack(buffer.items)
                    val writeDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()
                    createMetrics(targetID, metricDimensions, writeDurationInMillis)

                    resp
                } catch (e: AwsServiceException) {

                    log.trace("putRecordBatch error ${e.message}")

                    // Check the exception, it will throw an AwsServiceRetryableException if the error is recoverable
                    clientHelper.processServiceException(e)

                    if (canNotReachAwsService(e)) {
                        targetResults?.nackList(buffer.items)
                    } else {
                        targetResults?.errorList(buffer.items)
                    }

                    // Non recoverable service exceptions
                    throw e
                }
            }

            log.trace("putMessageBatch result is status ${resp.sdkHttpResponse()?.statusCode()}, ${resp.requestResponses()}")
            val errors = mutableListOf<TargetDataSerialMessagePair>()
            val ok = mutableListOf<TargetDataSerialMessagePair>()

            resp.requestResponses().forEachIndexed { i, r ->
                val targetData = buffer.items[i]
                if (r.errorMessage().isNullOrBlank()) {
                    ok.add(targetData)
                } else {
                    log.error("Error sending target data, serial number ${targetData.serial}, error code ${r.errorCode()}, ${r.errorMessage()}")
                    errors.add(targetData)
                }
            }
            targetResults?.ackAndError(ok, errors)

        } catch (e: Exception) {
            log.errorEx("Error sending to kinesis firehose delivery stream \"$streamName\" for target \"$targetID\"", e)
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
                metricsCollector?.buildValueDataPoint(adapterID, MetricsCollector.METRICS_MEMORY, MemoryMonitor.getUsedMemoryMB().toDouble(), MetricUnits.MEGABYTES),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITES, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_MESSAGES, buffer.size.toDouble(), MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_DURATION, writeDurationInMillis, MetricUnits.MILLISECONDS, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SIZE, buffer.payloadSize.toDouble(), MetricUnits.BYTES, metricDimensions)
            )
        }
    }

    // Builds send request for buffered messages
    private fun buildRequest(streamName: String?): PutRecordBatchRequest {

        val builder = PutRecordBatchRequest.builder()
        builder.deliveryStreamName(streamName)
        builder.records(
            if (customPayload) {
                buildCustomPayloadRecords()
            } else {
                buildRequestRecords()
            })
        return builder.build()
    }

    private fun buildRequestRecords(): List<Record?> = buffer.payloads.map { payload ->
        Record.builder()
            .data(SdkBytes.fromUtf8String(payload + "\n"))
            .build()
    }

    private fun buildCustomPayloadRecords(): List<Record> {
        val log = logger.getCtxLoggers(className, "buildCustomPayloadRecords")
        return sequence {
            buffer.messages.forEach { targetData: TargetData ->
                try {
                    yield(SdkBytes.fromByteArray(formatter!!.apply(targetData)))
                } catch (e: Exception) {
                    log.error("Error formatting record target data, $e")
                }
            }
        }.map { bytes: SdkBytes ->
            Record.builder()
                .data(bytes)
                .build()
        }.toList<Record>()
    }

    private val config: AwsFirehoseWriterConfiguration
        get() {
            return clientHelper.writerConfig(configReader, AWS_KINESIS_FIREHOSE)
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
         * Creates an instance of an AWS Kinesis Firehose writer from the passed configuration
         * @param configReader ConfigReader Reads the configuration for the writer
         * @see AwsFirehoseWriterConfiguration
         * @param targetID String ID of the target
         * @param logger Logger Logger for output
         * @return TargetWriter
         * @throws Exception
         */
        @JvmStatic
        fun newInstance(configReader: ConfigReader, targetID: String, logger: Logger, resultHandler: TargetResultHandler?): TargetWriter {

            try {
                return AwsKinesisFirehoseTargetWriter(targetID, configReader, logger, resultHandler)
            } catch (e: Throwable) {
                throw TargetException("Error creating AWS Firehose target, ${e.message}")
            }
        }

        const val FIREHOSE_MAX_BATCH_MSG_SIZE = 1024 * 1024 * 4

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
        )


    }
}