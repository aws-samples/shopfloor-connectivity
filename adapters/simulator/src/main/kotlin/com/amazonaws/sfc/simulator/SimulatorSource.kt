// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.simulator

import com.amazonaws.sfc.data.ChannelReadValue
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricDimensions
import com.amazonaws.sfc.metrics.MetricUnits
import com.amazonaws.sfc.metrics.MetricsCollector
import com.amazonaws.sfc.simulator.config.SimulatorSourceConfiguration
import com.amazonaws.sfc.system.DateTime
import java.io.Closeable
import java.time.Instant

class SimulatorSource(private val sourceID: String,
                      private val simulatorSourceConfiguration: SimulatorSourceConfiguration,
                      private val metricsCollector: MetricsCollector?,
                      adapterMetricDimensions: MetricDimensions?,
                      private val logger: Logger) : Closeable {

    private val className = this::class.simpleName.toString()


    private val protocolAdapterID = simulatorSourceConfiguration.protocolAdapterID
    private val sourceDimensions =
        mapOf(MetricsCollector.METRICS_DIMENSION_SOURCE to "$protocolAdapterID:$sourceID") + adapterMetricDimensions as Map<String, String>


    fun read(channels: List<String>?): Map<String, ChannelReadValue>? {

        val log = logger.getCtxLoggers(className, "read")
        val channelsToRead = if (channels.isNullOrEmpty()) simulatorSourceConfiguration.channels.keys else channels
        val start = DateTime.systemDateTime().toEpochMilli()

        val result = try {
            sequence {
                channelsToRead.forEach { channelName ->
                    val value = simulatorSourceConfiguration.channels[channelName]?.simulation?.value()
                    val readValue = if (value != null) {
                        val isBufferedValue = value is List<*> &&
                                value.isNotEmpty() &&
                                value.first() is Pair<*, *> &&
                                value.all { it is Pair<*, *> && it.first is Instant && it.second != null }
                        if (isBufferedValue) {
                            ChannelReadValue(
                                value = (value as List<*>).map { v ->
                                    val pair = v as Pair<*, *>
                                    ChannelReadValue(value = pair.second, timestamp = pair.first as Instant)
                                },
                                timestamp = DateTime.systemDateTime()
                            )
                        } else {
                            ChannelReadValue(value, DateTime.systemDateTime())
                        }
                    } else {
                        ChannelReadValue(value, DateTime.systemDateTime())
                    }
                    yield(Pair(channelName, readValue))
                }
            }.toMap()
        } catch (e: Exception) {
            log.error("Error reading data for source \"$sourceID\", ${e.message}")
            metricsCollector?.put(protocolAdapterID, MetricsCollector.METRICS_READ_ERRORS, 1.0, MetricUnits.COUNT, sourceDimensions)
            return null
        }

        val duration = DateTime.systemDateTime().toEpochMilli() - start
        createMetrics(protocolAdapterID, duration, result)
        return result.ifEmpty { null }

    }


    private fun createMetrics(
        protocolAdapterID: String,
        readDurationInMillis: Long,
        values: Map<String, ChannelReadValue>
    ) {
        metricsCollector?.put(
            protocolAdapterID,
            metricsCollector.buildValueDataPoint(protocolAdapterID, MetricsCollector.METRICS_READS, 1.0, MetricUnits.COUNT, sourceDimensions),
            metricsCollector.buildValueDataPoint(
                protocolAdapterID,
                MetricsCollector.METRICS_READ_DURATION,
                readDurationInMillis.toDouble(),
                MetricUnits.MILLISECONDS,
                sourceDimensions
            ),
            metricsCollector.buildValueDataPoint(
                protocolAdapterID,
                MetricsCollector.METRICS_VALUES_READ,
                values.size.toDouble(),
                MetricUnits.COUNT,
                sourceDimensions
            ),
            metricsCollector.buildValueDataPoint(protocolAdapterID, MetricsCollector.METRICS_READ_SUCCESS, 1.0, MetricUnits.COUNT, sourceDimensions)
        )
    }

    override fun close() {
    }


}