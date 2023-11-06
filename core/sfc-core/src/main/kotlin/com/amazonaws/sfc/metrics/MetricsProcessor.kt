
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.metrics

import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.ConfigWithMetrics
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.util.buildScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class MetricsProcessor(private val configReader: ConfigReader,
                       private val logger: Logger,
                       private val metricProviders: Map<String, MetricsProvider>,
                       private val createMetricsWriterMethod: (m: MetricsConfiguration) -> MetricsWriter?) {

    private val className = this::class.java.simpleName

    private val scope = buildScope("Metrics Reader")

    private val metricsChannel = Channel<MetricsData>(100 * metricProviders.size)

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
                    readMetricsFromProvider(provider.key, provider.value, trace)
                }
            }.toMap()

        return readers
    }

    private suspend fun readMetricsFromProvider(source: String, provider: MetricsProvider, trace: (String) -> Unit) {
        provider.read(metricsConfiguration.interval) { metricsData ->
            if (metricsData.dataPoints.isNotEmpty()) {
                var s = "${metricsData.dataPoints.size} data points received from metrics source \"${metricsData.source}\" (${metricsData.sourceType})"
                if (source != metricsData.source) {
                    s += ", forward by target \"$source\""
                }
                trace(s)
                metricsChannel.send(metricsData)
            }
            true
        }
    }

    private val reader = scope.launch {


        val log = logger.getCtxLoggers(className, "reader")

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
                log.error("Error writing to metrics writer, $e")
            }
        }
    }


    suspend fun close() {
        reader.cancel()
        metricReaderJobs?.values?.forEach { it.cancel() }
        metricReaderJobs = null
        writer?.close()
    }

}