
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.ipc.extensions

import com.amazonaws.sfc.ipc.Metrics.MetricsDataPoint.*
import com.amazonaws.sfc.metrics.*
import java.time.Instant


val com.amazonaws.sfc.ipc.Metrics.MetricsDataPoint.metricsDataPoint: MetricsDataPoint?
    get() {
        val value = this.nativeValue

        return if (value != null) MetricsDataPoint(
            name = this.name ?: "",
            dimensions = if (this.dimensionsMap.isNotEmpty()) this.dimensionsMap.entries.associate { it.key to it.value } else emptyMap(),
            units = this.units.nativeUnits,
            value = value,
            timestamp = Instant.ofEpochSecond(this.timestamp.seconds, this.timestamp.nanos.toLong()))
        else null
    }


private val com.amazonaws.sfc.ipc.Metrics.MetricsDataPoint.nativeValue: MetricsDataValue?
    get() =
        when (this.valueCase.number) {
            SINGLEVALUE_FIELD_NUMBER -> MetricsValue(this.singleValue)
            VALUES_FIELD_NUMBER -> MetricsValues(values = this.values.valuesList)
            STATISTICS_FIELD_NUMBER -> MetricsStatistics(
                maximum = this.statistics.maximum,
                minimum = this.statistics.minimum,
                sampleCount = this.statistics.sampleCount,
                sum = this.statistics.sampleCount)

            else -> null
        }


private val com.amazonaws.sfc.ipc.Metrics.MetricUnits.nativeUnits: MetricUnits
    get() =
        when (this) {
            com.amazonaws.sfc.ipc.Metrics.MetricUnits.SECONDS -> MetricUnits.SECONDS
            com.amazonaws.sfc.ipc.Metrics.MetricUnits.MICROSECONDS -> MetricUnits.MICROSECONDS
            com.amazonaws.sfc.ipc.Metrics.MetricUnits.MILLISECONDS -> MetricUnits.MILLISECONDS

            com.amazonaws.sfc.ipc.Metrics.MetricUnits.BYTES -> MetricUnits.BYTES
            com.amazonaws.sfc.ipc.Metrics.MetricUnits.KILOBYTES -> MetricUnits.KILOBYTES
            com.amazonaws.sfc.ipc.Metrics.MetricUnits.MEGABYTES -> MetricUnits.MEGABYTES
            com.amazonaws.sfc.ipc.Metrics.MetricUnits.GIGABYTES -> MetricUnits.GIGABYTES
            com.amazonaws.sfc.ipc.Metrics.MetricUnits.TERABYTES -> MetricUnits.TERABYTES

            com.amazonaws.sfc.ipc.Metrics.MetricUnits.BITS -> MetricUnits.BITS
            com.amazonaws.sfc.ipc.Metrics.MetricUnits.KILOBITS -> MetricUnits.KILOBITS
            com.amazonaws.sfc.ipc.Metrics.MetricUnits.MEGABITS -> MetricUnits.MEGABITS
            com.amazonaws.sfc.ipc.Metrics.MetricUnits.GIGABITS -> MetricUnits.GIGABITS
            com.amazonaws.sfc.ipc.Metrics.MetricUnits.TERABITS -> MetricUnits.TERABITS

            com.amazonaws.sfc.ipc.Metrics.MetricUnits.PERCENT -> MetricUnits.PERCENT
            com.amazonaws.sfc.ipc.Metrics.MetricUnits.COUNT -> MetricUnits.COUNT

            com.amazonaws.sfc.ipc.Metrics.MetricUnits.BYTES_SECOND -> MetricUnits.BYTES_PER_SECOND
            com.amazonaws.sfc.ipc.Metrics.MetricUnits.KILOBYTES_SECOND -> MetricUnits.KILOBYTES_PER_SECOND
            com.amazonaws.sfc.ipc.Metrics.MetricUnits.MEGABYTES_SECOND -> MetricUnits.MEGABYTES_PER_SECOND
            com.amazonaws.sfc.ipc.Metrics.MetricUnits.GIGABYTES_SECOND -> MetricUnits.GIGABYTES_PER_SECOND
            com.amazonaws.sfc.ipc.Metrics.MetricUnits.TERABYTES_SECOND -> MetricUnits.TERABYTES_PER_SECOND

            com.amazonaws.sfc.ipc.Metrics.MetricUnits.BITS_SECOND -> MetricUnits.BITS_PER_SECOND
            com.amazonaws.sfc.ipc.Metrics.MetricUnits.KILOBITS_SECOND -> MetricUnits.KILOBITS_PER_SECOND
            com.amazonaws.sfc.ipc.Metrics.MetricUnits.MEGABITS_SECOND -> MetricUnits.MEGABITS_PER_SECOND
            com.amazonaws.sfc.ipc.Metrics.MetricUnits.GIGABITS_SECOND -> MetricUnits.GIGABITS_PER_SECOND
            com.amazonaws.sfc.ipc.Metrics.MetricUnits.TERABITS_SECOND -> MetricUnits.TERABITS_PER_SECOND

            com.amazonaws.sfc.ipc.Metrics.MetricUnits.COUNT_SECOND -> MetricUnits.COUNT_PER_SECOND

            else -> MetricUnits.NONE
        }

