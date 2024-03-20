// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.metrics

import com.amazonaws.sfc.channels.*
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.ConfigWithMetrics
import com.amazonaws.sfc.config.ConfigWithTuningConfiguration
import com.amazonaws.sfc.config.TuningConfiguration
import com.amazonaws.sfc.config.TuningConfiguration.Companion.CONFIG_METRICS_CHANNEL_SIZE_PER_METRICS_PROVIDER
import com.amazonaws.sfc.config.TuningConfiguration.Companion.CONFIG_METRICS_CHANNEL_TIMEOUT
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.isJobCancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class MetricsProcessor(
    private val configReader: ConfigReader,
    private val logger: Logger,
    private val metricProviders: Map<String, MetricsProvider>,
    private val createMetricsWriterMethod: (m: MetricsConfiguration) -> MetricsWriter?
) {

    private val className = this::class.java.simpleName

    private val scope = buildScope("Metrics Reader", Dispatchers.IO)

    private val tuningConfiguration: TuningConfiguration by lazy {
        (configReader.getConfig<ConfigWithTuningConfiguration>().tuningConfiguration)
    }

    private val metricsChannel = Channel<MetricsData>(tuningConfiguration.channelSizePerMetricsProvider * metricProviders.size)

    private val metricsConfiguration: MetricsConfiguration by lazy {
        (configReader.getConfig<ConfigWithMetrics>().metrics) ?: MetricsConfiguration()
    }

    private val writer by lazy {
        createMetricsWriterMethod(metricsConfiguration)
    }

    fun start() {
        metricReaderJobs = createMetricReaders()
    }

    val isRunning: Boolean
        get() = metricReaderJobs?.values?.all { it.isActive } ?: true


    private var metricReaderJobs: Map<String, Job>? = null

    private fun createMetricReaders(): Map<String, Job> {
        val trace = logger.getCtxTraceLog(className, "readerJobs")

        val readers =

            metricProviders.map { provider ->
                trace("Create metrics provider for metric data from adapter ${provider.key}")

                provider.key to scope.launch {
                    readMetricsFromProvider(provider.key, provider.value, logger)
                }
            }.toMap()

        return readers
    }

    private suspend fun readMetricsFromProvider(source: String, provider: MetricsProvider, logger: Logger) {
        val log = logger.getCtxLoggers(className, "readMetricsFromProvider")
        provider.read(metricsConfiguration.interval) { metricsData ->
            if (metricsData.dataPoints.isNotEmpty()) {
                var s = "${metricsData.dataPoints.size} data points received from metrics source \"${metricsData.source}\" (${metricsData.sourceType})"
                if (source != metricsData.source) {
                    s += ", forward by target \"$source\""
                }
                log.trace(s)
                metricsChannel.submit(metricsData, tuningConfiguration.metricsChannelTimeout){ event ->
                    channelSubmitEventHandler(
                        event,
                        "MetricsProcessor:metricsChannel",
                        CONFIG_METRICS_CHANNEL_SIZE_PER_METRICS_PROVIDER,
                        tuningConfiguration.channelSizePerMetricsProvider,
                        CONFIG_METRICS_CHANNEL_TIMEOUT,
                        log
                    )
                }
            }
            true
        }
    }

    fun channelSubmitEventHandler(
        event: ChannelEvent<MetricsData>,
        channelName: String,
        tuningChannelSizeName: String,
        currentChannelSize: Int,
        tuningChannelTimeoutName: String,
        log: Logger.ContextLogger,
    ) {
        when (event) {
            is ChannelEventBlocking -> log.warning("Sending metrics to $channelName is blocking, consider setting tuning parameter $tuningChannelSizeName to a higher value, current value is $currentChannelSize")
            is ChannelEventTimeout -> log.error("Sending date to $channelName timeout after ${event.timeout}, consider setting tuning parameter $tuningChannelTimeoutName to a longer value")
            is ChannelEventSubmittedBlocking -> log.warning("Sending date to $channelName  was blocking for ${event.duration}, consider setting tuning parameter $tuningChannelSizeName to a higher value, current value is $currentChannelSize")
            is ChannelEventOutOfMemory -> throw OutOfMemoryError("Out of memory while submitting element to $channelName, ${event.outOfMemoryError}, consider setting tuning parameter $tuningChannelSizeName to a lower value, current value is $currentChannelSize")
            else -> {}
        }
    }

    private val reader = scope.launch {


        val log = logger.getCtxLoggers(className, "reader")
        try {

            if (writer != null) {
                log.info("Metrics writer of type  ${writer!!::class.java.simpleName} created")
            }

            for (metricsData in metricsChannel) {
                try {

                    if (writer != null) {
                        log.trace("Sending ${metricsData.dataPoints.size} data point to metrics writer ${writer!!::class.java.simpleName}")
                        writer?.writeMetricsData(metricsData)
                        log.trace("Done sending data point to metrics writer ")
                    } else {
                        log.error("No metrics writer for sending metrics data point")
                    }
                } catch (e: Exception) {
                    log.errorEx("Error writing to metrics writer", e)
                }
            }
        } catch (e: Exception) {
            if (!e.isJobCancellationException)
                log.errorEx("Error reading metrics", e)
        }
    }


    suspend fun close() {
        reader.cancel()
        metricReaderJobs?.values?.forEach { it.cancel() }
        metricReaderJobs = null
        writer?.close()
    }

}