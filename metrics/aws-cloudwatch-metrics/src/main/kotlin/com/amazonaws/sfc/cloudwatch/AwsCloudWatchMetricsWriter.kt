// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.cloudwatch


import com.amazonaws.sfc.channels.channelSubmitEventHandler
import com.amazonaws.sfc.channels.submit
import com.amazonaws.sfc.cloudwatch.config.AwsCloudWatchConfiguration
import com.amazonaws.sfc.cloudwatch.config.AwsCloudWatchConfiguration.Companion.CONFIG_CW_MAX_BATCH_SIZE
import com.amazonaws.sfc.cloudwatch.config.AwsCloudWatchConfiguration.Companion.CONFIG_DEFAULT_CW_WRITE_INTERVAL
import com.amazonaws.sfc.cloudwatch.config.AwsCloudWatchMetricsWriterConfiguration
import com.amazonaws.sfc.cloudwatch.config.AwsCloudWatchMetricsWriterConfiguration.Companion.CONFIG_METRICS_CHANNEL_SIZE
import com.amazonaws.sfc.cloudwatch.config.AwsCloudWatchMetricsWriterConfiguration.Companion.CONFIG_METRICS_CHANNEL_TIMEOUT
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsConfiguration.Companion.CONFIG_METRICS_NAMESPACE
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.byteCountString
import com.amazonaws.sfc.util.launch
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.cloudwatch.model.Dimension
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest
import software.amazon.awssdk.services.cloudwatch.model.StatisticSet
import kotlin.time.DurationUnit
import kotlin.time.toDuration


class AwsCloudWatchMetricsWriter(private val configReader: ConfigReader, private val logger: Logger) : MetricsWriter {

    private val className = this::class.java.simpleName

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val metricDatumBuffer = mutableListOf<MetricDatum>()
    private var bufferedDataSize = 0

    private val config by lazy {
        configReader.getConfig<AwsCloudWatchMetricsWriterConfiguration>()
    }

    private val metricsChannel = Channel<MetricsData>(config.metricsChannelSize)

    private val MetricDatum.dataSize
        get() =
            this.metricName().length +
                    this.unit().name.length +
                    this.timestamp().toString().length +
                    this.dimensions().sumOf { it.name().length + it.value().length } +
                    this.counts().size * Double.SIZE_BYTES +
                    this.values().size * Double.SIZE_BYTES +
                    Double.SIZE_BYTES +  // value
                    5 * Double.SIZE_BYTES // statistics


    private var _clientHelper: AwsCloudWatchClientHelper? = null

    private val clientHelper: AwsCloudWatchClientHelper
        get() {
            if (_clientHelper == null) {
                _clientHelper = AwsCloudWatchClientHelper(config, config.metrics?.cloudWatch ?: AwsCloudWatchConfiguration(), logger)
            }
            return _clientHelper as AwsCloudWatchClientHelper
        }

    private val metricsClient: AwsCloudWatchClient by lazy { AwsCloudWatchClientWrapper(clientHelper.serviceClient as CloudWatchClient) }

    private val scope = buildScope("AWS CloudWatch Metrics Writer")

    val writer = scope.launch(context = Dispatchers.IO) {

        val log = logger.getCtxLoggers(className, "writer")

        var timer = timerJob()

        while (isActive) {
            try {
                select {
                    metricsChannel.onReceive { metricsData ->

                        metricsData.dataPoints.forEach { datapoint ->
                            val metricDatum = buildDataPoint(datapoint, metricsData.commonDimensions)

                            val metricDatumSize = metricDatum.dataSize
                            if ((metricDatumSize + bufferedDataSize) > CW_MAX_REQUEST_SIZE) {
                                log.info("Max put metrics request size reached reached, writing metric data points")
                                timer.cancel()
                                flush()
                                timer = timerJob()
                            }

                            bufferedDataSize += metricDatum.dataSize
                            metricDatumBuffer.add(metricDatum)

                            if (metricDatumBuffer.size >= (config.metrics?.cloudWatch?.batchSize ?: CONFIG_CW_MAX_BATCH_SIZE)) {
                                timer.cancel()
                                flush()
                                timer = timerJob()
                            }
                        }
                        if (metricsData.dataPoints.isNotEmpty()) {
                            log.trace("Metrics data point buffer contains ${metricDatumBuffer.size} entries with a total size of ${bufferedDataSize.byteCountString}")
                        }
                    }
                    timer.onJoin {
                        if (metricDatumBuffer.isNotEmpty()) {
                            log.info("${config.metrics?.cloudWatch?.interval?.inWholeSeconds} seconds buffer writer interval reached, writing metric data points")
                            flush()
                        }
                        timer = timerJob()

                    }
                }
            } catch (e: Exception) {
                if (e !is IllegalStateException) // service shutting down, not an error
                    log.errorEx("Error writing metrics", e)
            }
        }
    }

    private fun CoroutineScope.timerJob(): Job {
        return launch(context = Dispatchers.IO, name = "Timeout timer") {
            return@launch try {
                delay(config.metrics?.cloudWatch?.interval ?: CONFIG_DEFAULT_CW_WRITE_INTERVAL.toDuration(DurationUnit.SECONDS))
            } catch (e: Exception) {
                // no harm done, timer is just used to guard for timeouts
            }
        }
    }


    private fun flush() {

        val log = logger.getCtxLoggers(className, "flush")
        if (metricDatumBuffer.isEmpty()) {
            return
        }

        val request = PutMetricDataRequest.builder()
            .namespace(config.metrics?.nameSpace ?: CONFIG_METRICS_NAMESPACE)
            .metricData(metricDatumBuffer)
            .build()

        try {
            val resp = clientHelper.executeServiceCallWithRetries {
                try {
                    log.info("Writing ${metricDatumBuffer.size} metric data points")
                    val resp = metricsClient.putMetricData(request)
                    resp
                } catch (e: AwsServiceException) {
                    log.trace("CloudWatch putMetricData error ${e.message}")
                    // Check the exception, it will throw an AwsServiceRetryableException if the error is recoverable
                    clientHelper.processServiceException(e)
                    // Non recoverable service exceptions
                    throw e
                }
            }
            log.trace("putMetricData response status code is ${resp.sdkHttpResponse().statusCode()}")
        } catch (e: Exception) {
            if (e !is IllegalStateException) // service shutting down, not an error
                log.errorEx("Error writing metrics", e)
        } finally {
            bufferedDataSize = 0
            metricDatumBuffer.clear()
        }

    }


    val unitNames =
        MetricUnits.entries.map { it to it.declaringJavaClass.getField(it.name).getDeclaredAnnotation(SerializedName::class.java).value }


    private fun buildDataPoint(dataPoint: MetricsDataPoint, commonDimensions: MetricDimensions?): MetricDatum {

        val common: List<Dimension> = commonDimensions?.map { dimension ->
            (Dimension.builder().name(dimension.key).value(dimension.value)
                .build())
        } ?: emptyList()

        val builder = MetricDatum.builder()
            .metricName(dataPoint.name)
            .unit(dataPoint.units.serializedName)
            .timestamp(dataPoint.timestamp)
            .dimensions(
                buildDimensions(dataPoint, common).toList()
            )

        when (dataPoint.value) {
            is MetricsValue -> builder.value((dataPoint.value as MetricsValue).value)
            is MetricsStatistics -> builder.statisticValues(StatisticSet.builder().build())
            is MetricsValues -> {
                builder.values((dataPoint.value as MetricsValues).values)
                if (!(dataPoint.value as MetricsValues).counts.isNullOrEmpty()) {
                    builder.counts((dataPoint.value as MetricsValues).counts)
                }
            }
        }

        return builder.build()
    }

    private fun buildDimensions(dataPoint: MetricsDataPoint, commonDimensions: List<Dimension>?): List<Dimension> {
        return sequence {
            yieldAll(commonDimensions?.asSequence() ?: emptySequence())
            sequence {
                dataPoint.dimensions?.forEach { dimension ->
                    yield(Dimension.builder().name(dimension.key).value(dimension.value).build())
                }
            }
        }.toList()
    }

    override suspend fun writeMetricsData(metricsData: MetricsData) {
        val log = logger.getCtxLoggers(className, "write")
        log.trace("${metricsData.dataPoints} received")
        metricsChannel.submit(metricsData, config.metricsChannelTimeout) { event ->
            channelSubmitEventHandler(
                event = event,
                channelName = "$className:metricsChannel",
                tuningChannelSizeName = CONFIG_METRICS_CHANNEL_SIZE,
                currentChannelSize = config.metricsChannelSize,
                tuningChannelTimeoutName = CONFIG_METRICS_CHANNEL_TIMEOUT,
                log = log
            )
        }
    }

    override suspend fun close() {
        flush()
        _clientHelper?.serviceClient?.close()
    }

    companion object {
        const val CW_MAX_REQUEST_SIZE = 1000 * 1000

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any?) =
            newInstance(createParameters[0] as ConfigReader, createParameters[1] as Logger)


        @JvmStatic
        fun newInstance(configReader: ConfigReader, logger: Logger): MetricsWriter {
            return try {
                AwsCloudWatchMetricsWriter(configReader, logger)
            } catch (e: Throwable) {
                throw TargetException("Error creating AWS CloudWatch Metrics writer, ${e.message}")
            }
        }
    }


}











