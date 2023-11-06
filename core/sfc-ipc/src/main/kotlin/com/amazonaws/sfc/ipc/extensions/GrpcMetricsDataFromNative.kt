
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.ipc.extensions

import com.amazonaws.sfc.metrics.MetricsData

val MetricsData.grpcMetricsDataMessage: com.amazonaws.sfc.ipc.Metrics.MetricsDataMessage
    get() {
        val metricsDataBuilder = com.amazonaws.sfc.ipc.Metrics.MetricsDataMessage.newBuilder()

        metricsDataBuilder.source = this.source
        metricsDataBuilder.sourceType = this.sourceType.grpcMetricSourceType

        if (!this.commonDimensions.isNullOrEmpty()) {

            (this.commonDimensions as Map<String, String>).forEach { _ ->
                metricsDataBuilder.putAllCommonDimensions(this.commonDimensions)
            }
            //  metricsDataBuilder.commonDimensionsMap.put() .putAll(this.commonDimensions!!)
        }
        metricsDataBuilder.addAllDataPoints(this.dataPoints.map { it.grpcDataPoint })

        return metricsDataBuilder.build()
    }


val com.amazonaws.sfc.metrics.MetricsSourceType.grpcMetricSourceType: com.amazonaws.sfc.ipc.Metrics.MetricsSourceType
    get() =
        when (this) {
            com.amazonaws.sfc.metrics.MetricsSourceType.SFC_CORE -> com.amazonaws.sfc.ipc.Metrics.MetricsSourceType.SFC_CORE
            com.amazonaws.sfc.metrics.MetricsSourceType.PROTOCOL_ADAPTER -> com.amazonaws.sfc.ipc.Metrics.MetricsSourceType.PROTOCOL_ADAPTER
            com.amazonaws.sfc.metrics.MetricsSourceType.TARGET_WRITER -> com.amazonaws.sfc.ipc.Metrics.MetricsSourceType.TARGET_WRITER
            else -> com.amazonaws.sfc.ipc.Metrics.MetricsSourceType.UNDEFINED
        }


