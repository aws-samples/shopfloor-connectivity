/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.awsiota


import com.amazonaws.sfc.awsiota.config.AwsIotAnalyticsTargetConfiguration
import com.amazonaws.sfc.awsiota.config.AwsIotAnalyticsWriterConfiguration
import com.amazonaws.sfc.awsiota.config.AwsIotAnalyticsWriterConfiguration.Companion.AWS_IOT_ANALYTICS
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.data.TargetDataBuffer.Companion.newTargetDataBuffer
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.targets.AwsServiceTargetClientHelper
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.canNotReachAwsService
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.iotanalytics.IoTAnalyticsClient
import software.amazon.awssdk.services.iotanalytics.model.BatchPutMessageRequest
import software.amazon.awssdk.services.iotanalytics.model.BatchPutMessageResponse
import software.amazon.awssdk.services.iotanalytics.model.Message


/**
 * AWS IoT Analytics Target writer
 * @property targetID String ID of target
 * @property configReader configReader for reading target configuration
 * @property logger Logger Logger for output
 * @see TargetWriter
 */
class AwsIotAnalyticsTargetWriter(
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

    private val scope = buildScope("Iot Analytics Target")

    private val clientHelper = AwsServiceTargetClientHelper(
        configReader.getConfig<AwsIotAnalyticsWriterConfiguration>(),
        targetID,
        IoTAnalyticsClient.builder(),
        logger)

    private val iotaClient: AwsIotAnalyticsClient
        get() = AwsIotAnalyticsClientWrapper(clientHelper.serviceClient as IoTAnalyticsClient)

    private val targetResults = if (resultHandler != null) TargetResultHelper(targetID, resultHandler, logger) else null

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
     * Writes message to Iot Analytics target.
     * @param targetData TargetData
     */
    override suspend fun writeTargetData(targetData: TargetData) {
        targetDataChannel.send(targetData)
    }

    /**
     * Closes the writer
     */
    override suspend fun close() {
        targetDataChannel.close()
        flush()
        writer.cancel()
        iotaClient.close()
    }

    // channel for passing messages to coroutine that sends messages to IoT analytics  stream
    private val targetDataChannel = Channel<TargetData>(100)

    // buffer for message batches
    private val buffer = newTargetDataBuffer(resultHandler)


    // coroutine that writes messages to channel
    private val writer = scope.launch("Writer") {
        runWriter()
    }

    // batches and writes messages to the channel
    private suspend fun runWriter() {
        val infoLog = logger.getCtxInfoLog(AwsIotAnalyticsTargetWriter::class.java.simpleName, "writer")

        infoLog("AWS IoT Analytics writer for target \"$targetID\" sending to channel \"${targetConfig.channelName}\" in region ${targetConfig.region}")

        for (targetData in targetDataChannel) {
            sendTargetData(targetData)
        }
    }

    private val transformation by lazy { if (targetConfig.template != null) OutputTransformation(targetConfig.template!!, logger) else null }

    private fun buildPayload(targetData: TargetData): String =
        if (transformation == null) targetData.toJson(config.elementNames) else transformation!!.transform(targetData, config.elementNames) ?: ""


    // buffer messages and sends message to channel if buffer is reached
    private fun sendTargetData(targetData: TargetData) {
        val payload = buildPayload(targetData)

        buffer.add(targetData, payload)

        // flush if buffer size reached
        if (targetData.noBuffering || buffer.size >= targetConfig.batchSize) {
            flush()
        }
    }

    // writes all buffered messages to IoT Analytics channel
    private fun flush() {

        val log = logger.getCtxLoggers(className, "flush")

        // No buffered messages, nothing to do
        if (buffer.size == 0) {
            return
        }

        val streamName = targetConfig.channelName
        log.trace("Sending data to stream \"$streamName\"\n${buffer.payloads.joinToString(separator = "\n")}")

        val start = DateTime.systemDateTime().toEpochMilli()


        try {
            val request = buildBatchPutMessageRequest()

            val resp = clientHelper.executeServiceCallWithRetries {
                try {
                    val r = iotaClient.batchPutMessage(request)
                    val writeDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()
                    createMetrics(targetID, metricDimensions, writeDurationInMillis)
                    r
                } catch (e: AwsServiceException) {

                    log.trace("IoT Analytics batchPutMessage error ${e.message}")

                    // Check the exception, it will throw an AwsServiceRetryableException if the error is recoverable
                    clientHelper.processServiceException(e)

                    // Non recoverable service exceptions
                    throw e
                }
            }

            log.trace("SendMessageBatch result is ${resp.sdkHttpResponse()?.statusCode()}")

            resp.batchPutMessageErrorEntries().forEach {
                log.error("Message \"${buffer.payloads[it.messageId().toInt()]}\"\nError code : ${it.errorCode()}, Error message : ${it.errorMessage()}")
            }

            ackOrErrorTargetDataSerials(resp)

        } catch (e: Exception) {
            log.error("Error sending to channel \"$streamName\" for target \"$targetID\", ${e.message}")
            runBlocking { metricsCollector?.put(targetID, MetricsCollector.METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions) }
            nackOrErrorTargetDataSerials(e)
        } finally {

            buffer.clear()
        }
    }

    private fun createMetrics(adapterID: String,
                              metricDimensions: MetricDimensions,
                              writeDurationInMillis: Double) {

        runBlocking {
            metricsCollector?.put(adapterID,
                metricsCollector?.buildValueDataPoint(adapterID, MetricsCollector.METRICS_WRITES, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, MetricsCollector.METRICS_MESSAGES, buffer.size.toDouble(), MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, MetricsCollector.METRICS_WRITE_DURATION, writeDurationInMillis, MetricUnits.MILLISECONDS, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, MetricsCollector.METRICS_WRITE_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, MetricsCollector.METRICS_WRITE_SIZE, buffer.payloadSize.toDouble(), MetricUnits.BYTES, metricDimensions))
        }

    }

    private fun nackOrErrorTargetDataSerials(e: Exception) {
        if (canNotReachAwsService(e)) {
            targetResults?.nackList(buffer.items)
        } else {
            targetResults?.errorList(buffer.items)
        }
    }

    private fun ackOrErrorTargetDataSerials(resp: BatchPutMessageResponse?) {
        if (targetResults != null) {
            val noErrors = resp?.batchPutMessageErrorEntries()?.isEmpty() == true
            if (noErrors) {
                targetResults.ack(buffer.items)
            } else {
                val errorSerials = resp?.batchPutMessageErrorEntries()?.map { it.messageId() } ?: emptyList()
                val errors = buffer.items.filter { errorSerials.contains(it.serial) }
                if (errors.size == buffer.size) {
                    targetResults.errorList(errors)
                } else {
                    val acks: List<TargetDataSerialMessagePair> = buffer.items.filter { !errorSerials.contains(it.serial) }
                    targetResults.ackAndError(acks, errors)
                }
            }
        }
    }


    // build Iot Analytics batch message request
    private fun buildBatchPutMessageRequest() =
        BatchPutMessageRequest.builder().channelName(targetConfig.channelName)
            .messages(buffer.items.mapIndexed { i, message ->
                Message.builder()
                    .messageId(message.serial)
                    .payload(SdkBytes.fromUtf8String(buffer.payloads[i]))
                    .build()
            })
            .build()


    private val config: AwsIotAnalyticsWriterConfiguration
        get() {
            return clientHelper.writerConfig(configReader, AWS_IOT_ANALYTICS)
        }

    private val targetConfig: AwsIotAnalyticsTargetConfiguration by lazy {
        clientHelper.targetConfig(config, targetID, AWS_IOT_ANALYTICS)
    }


    companion object {

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any?) =
            newInstance(createParameters[0] as ConfigReader, createParameters[1] as String, createParameters[2] as Logger, createParameters[3] as TargetResultHandler?)


        /**
         * Creates new instance of AWS Iot Analytics target from configuration.
         * @param configReader ConfigReader Reader for reading configuration for target instance
         * @see AwsIotAnalyticsWriterConfiguration
         * @param targetID String ID of the target
         * @param logger Logger Logger for output
         * @return TargetWriter Created target
         * @throws Exception
         */
        @JvmStatic
        fun newInstance(configReader: ConfigReader, targetID: String, logger: Logger, resultHandler: TargetResultHandler?): TargetWriter {
            return try {
                AwsIotAnalyticsTargetWriter(targetID, configReader, logger, resultHandler)
            } catch (e: Throwable) {
                throw TargetException("Error creating AWS IoT Analytics target writer, ${e.message}")
            }
        }

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET)

    }
}











