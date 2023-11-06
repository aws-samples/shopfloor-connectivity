
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsiot.http


import com.amazonaws.sfc.awsiot.http.config.AwsIotHttpTargetConfiguration
import com.amazonaws.sfc.awsiot.http.config.AwsIotHttpWriterConfiguration
import com.amazonaws.sfc.awsiot.http.config.AwsIotHttpWriterConfiguration.Companion.AWS_IOT_HTTP_TARGET
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
import com.amazonaws.sfc.targets.AwsServiceTargetClientHelper
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.canNotReachAwsService
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.iot.IotClient
import software.amazon.awssdk.services.iot.model.DescribeEndpointRequest
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClientBuilder
import software.amazon.awssdk.services.iotdataplane.model.PublishRequest
import java.net.URI


/**
 * @property targetID String ID of the target
 * @property configReader configuration reader
 * @property logger Logger Logger to use for output
 * @see TargetWriter
 */
class AwsIotHttpTargetWriter(
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

    private val scope = CoroutineScope(Dispatchers.Default) + CoroutineName("IoT Data Plane Target")

    private val dataClientBuilder: IotDataPlaneClientBuilder = IotDataPlaneClient.builder().endpointOverride(URI.create("https://$endpoint"))

    private val clientHelper =
        AwsServiceTargetClientHelper(
            configReader.getConfig<AwsIotHttpWriterConfiguration>(),
            targetID,
            dataClientBuilder,
            logger
        )

    private val targetResults = if (resultHandler != null) TargetResultHelper(targetID, resultHandler, logger) else null

    private val iotDataPlaneClient: AwsIoTDataPlaneClient
        get() {
            return AwsIotDataPlaneClientWrapper(clientHelper.serviceClient as IotDataPlaneClient)
        }

    private val endpoint: String?
        get() {
            val iotControlHelper = AwsServiceTargetClientHelper(
                configReader.getConfig<AwsIotHttpWriterConfiguration>(),
                targetID,
                IotClient.builder(),
                logger
            )
            val iotControlClient = iotControlHelper.serviceClient as IotClient
            val endpoint = iotControlClient.describeEndpoint { r: DescribeEndpointRequest.Builder -> r.endpointType("iot:Data-ATS") }?.endpointAddress()
            iotControlClient.close()
            return endpoint
        }

    // Channel to pass data to be sent to target to worker that does the actual sending
    private val targetDataChannel = Channel<TargetData>(100)

    private val config: AwsIotHttpWriterConfiguration
        get() {
            return clientHelper.writerConfig(configReader, AWS_IOT_HTTP_TARGET)
        }

    private val targetConfig: AwsIotHttpTargetConfiguration by lazy {
        clientHelper.targetConfig(config, targetID, AWS_IOT_HTTP_TARGET)
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

    /**
     *
     * @param targetData TargetData data to be published to the target.
     */
    override suspend fun writeTargetData(targetData: TargetData) {
        // Accept data and send for worker for processing and  sending to IoT topic target
        targetDataChannel.send(targetData)
    }

    /**
     * Closes the target
     * @see TargetWriter
     */
    override suspend fun close() {
        targetDataChannel.close()
        writer.cancel()
    }

    // Worker coroutine that handles the publishing of the data to the topic
    private val writer = scope.launch("Writer") {
        targetWriter()
    }

    private val transformation by lazy { if (targetConfig.template != null) OutputTransformation(targetConfig.template!!, logger) else null }

    private fun buildPayload(targetData: TargetData): String =
        if (transformation == null) targetData.toJson(config.elementNames) else transformation!!.transform(targetData, config.elementNames) ?: ""


    // Logic used by worker to send data to the topic
    private suspend fun targetWriter() {
        val log = logger.getCtxLoggers(className, "targetWriter")

        log.info("AWS IoT HTTP writer for target \"$targetID\" publishing to topic \"${targetConfig.topicName}\" in region ${targetConfig.region} on target $targetID")

        // Receive all data passed to worker through channel
        for (targetData in targetDataChannel) {
            val topicName = targetConfig.topicName ?: targetData.schedule

            val payload = buildPayload(targetData)

            val start = DateTime.systemDateTime().toEpochMilli()

            log.trace("Sending to topic \"$topicName\", $payload on target $targetID")
            try {
                // Build request and make client API call
                val request = buildRequest(payload, topicName)
                val resp = iotDataPlaneClient.publish(request)
                val writeDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()
                createMetrics(targetID, metricDimensions, payload.length, writeDurationInMillis)

                log.trace("Publish result is ${resp.sdkHttpResponse().statusText()}")
                targetResults?.ack(targetData)
            } catch (e: Throwable) {
                log.error("Error publishing to topic \"$topicName\" for target \"$targetID\", ${e.message}")
                runBlocking { metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions) }
                if (canNotReachAwsService(e)) {
                    targetResults?.nack(targetData)
                } else {
                    targetResults?.error(targetData)
                }
            }
        }
    }

    private fun createMetrics(adapterID: String,
                              metricDimensions: MetricDimensions,
                              size: Int,
                              writeDurationInMillis: Double) {

        runBlocking {
            metricsCollector?.put(adapterID,
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITES, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_MESSAGES, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_DURATION, writeDurationInMillis, MetricUnits.MILLISECONDS, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SIZE, size.toDouble(), MetricUnits.BYTES, metricDimensions))
        }

    }

    // Builds a request for publishing a message to a topic
    private fun buildRequest(data: String, topicName: String) =
        PublishRequest.builder()
            .payload(SdkBytes.fromUtf8String(data))
            .topic(topicName)
            .build()

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any?) =
            newInstance(createParameters[0] as ConfigReader, createParameters[1] as String, createParameters[2] as Logger, createParameters[3] as TargetResultHandler?)

        /**
         * Creates a new instance of an AWS IoT target writer.
         * @param configReader ConfigReader Reader for reading the target configuration
         * @param targetID String ID of the target
         * @param logger Logger Logger to use for output
         * @return TargetWriter Created AWS IoT target writer
         * @see TargetWriter,
         * @see AwsIotHttpTargetConfiguration
         * @see AwsIotHttpTargetWriter
         * @throws Exception
         */
        @JvmStatic
        fun newInstance(configReader: ConfigReader, targetID: String, logger: Logger, resultHandler: TargetResultHandler?): TargetWriter {

            return try {
                AwsIotHttpTargetWriter(targetID, configReader, logger, resultHandler)
            } catch (e: Throwable) {
                throw TargetException("Error creating AWS IoT core target writer, ${e.message}")
            }
        }

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET)

    }
}