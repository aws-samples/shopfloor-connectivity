
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.ipc.extensions

import com.amazonaws.sfc.ipc.Metrics.ValueList
import com.amazonaws.sfc.metrics.*

val MetricsDataPoint.grpcDataPoint: com.amazonaws.sfc.ipc.Metrics.MetricsDataPoint
    get() {
        val dataPointBuilder = com.amazonaws.sfc.ipc.Metrics.MetricsDataPoint.newBuilder()
        dataPointBuilder.name = this.name
        if (!this.dimensions.isNullOrEmpty()) dataPointBuilder.putAllDimensions(this.dimensions!!)
        dataPointBuilder.units = this.units.grpcMetricUnits
        dataPointBuilder.timestamp = newTimestamp(this.timestamp)

        when (this.value) {
            is MetricsValue -> dataPointBuilder.singleValue = (this.value as MetricsValue).value
            is MetricsValues -> dataPointBuilder.values = (this.value as MetricsValues).grpcValues
            is MetricsStatistics -> dataPointBuilder.statistics = (this.value as MetricsStatistics).grpcStatistics
        }
        return dataPointBuilder.build()
    }


private val MetricsValues.grpcValues: ValueList
    get() {
        val valueListBuilder = ValueList.newBuilder()
        valueListBuilder.addAllValues(this.values)
        if (!this.counts.isNullOrEmpty()) valueListBuilder.addAllCount(this.counts)
        return valueListBuilder.build()
    }

private val MetricsStatistics.grpcStatistics: com.amazonaws.sfc.ipc.Metrics.MetricsStatistics
    get() {
        val statsBuilder = com.amazonaws.sfc.ipc.Metrics.MetricsStatistics.newBuilder()
        statsBuilder.maximum = this.maximum
        statsBuilder.minimum = this.minimum
        statsBuilder.sum = this.sum
        statsBuilder.sampleCount = this.sampleCount
        return statsBuilder.build()
    }

private val MetricUnits.grpcMetricUnits: com.amazonaws.sfc.ipc.Metrics.MetricUnits
    get() =
        when (this) {
            MetricUnits.SECONDS -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.SECONDS
            MetricUnits.MICROSECONDS -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.MICROSECONDS
            MetricUnits.MILLISECONDS -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.MILLISECONDS

            MetricUnits.BYTES -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.BYTES
            MetricUnits.KILOBYTES -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.KILOBYTES
            MetricUnits.MEGABYTES -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.MEGABYTES
            MetricUnits.GIGABYTES -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.GIGABYTES
            MetricUnits.TERABYTES -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.TERABYTES

            MetricUnits.BITS -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.BITS
            MetricUnits.KILOBITS -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.KILOBITS
            MetricUnits.MEGABITS -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.MEGABITS
            MetricUnits.GIGABITS -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.GIGABITS
            MetricUnits.TERABITS -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.TERABITS

            MetricUnits.PERCENT -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.PERCENT
            MetricUnits.COUNT -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.COUNT

            MetricUnits.BYTES_PER_SECOND -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.BYTES_SECOND
            MetricUnits.KILOBYTES_PER_SECOND -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.KILOBYTES_SECOND
            MetricUnits.MEGABYTES_PER_SECOND -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.MEGABYTES_SECOND
            MetricUnits.GIGABYTES_PER_SECOND -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.GIGABYTES_SECOND
            MetricUnits.TERABYTES_PER_SECOND -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.TERABYTES_SECOND

            MetricUnits.BITS_PER_SECOND -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.BITS_SECOND
            MetricUnits.KILOBITS_PER_SECOND -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.KILOBITS_SECOND
            MetricUnits.MEGABITS_PER_SECOND -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.MEGABITS_SECOND
            MetricUnits.GIGABITS_PER_SECOND -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.GIGABITS_SECOND
            MetricUnits.TERABITS_PER_SECOND -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.TERABITS_SECOND

            MetricUnits.COUNT_PER_SECOND -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.COUNT_SECOND

            else -> com.amazonaws.sfc.ipc.Metrics.MetricUnits.UNITS_NONE
        }
