
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.ipc.extensions

import com.amazonaws.sfc.metrics.MetricsData
import com.amazonaws.sfc.metrics.MetricsSourceType

val com.amazonaws.sfc.ipc.Metrics.MetricsDataMessage.asNativeMetricsData: MetricsData
    get() = MetricsData(
        source = this.source,
        sourceType = this.sourceType.nativeSourceType,
        dataPoints = this.dataPointsList.mapNotNull { it.metricsDataPoint },
        commonDimensions = if (!this.commonDimensionsMap.isNullOrEmpty())
            commonDimensionsMap.entries.associate { it.key to it.value }
        else
            null
    )

private val com.amazonaws.sfc.ipc.Metrics.MetricsSourceType.nativeSourceType: MetricsSourceType
    get() = when (this) {
        com.amazonaws.sfc.ipc.Metrics.MetricsSourceType.SFC_CORE -> MetricsSourceType.SFC_CORE
        com.amazonaws.sfc.ipc.Metrics.MetricsSourceType.PROTOCOL_ADAPTER -> MetricsSourceType.PROTOCOL_ADAPTER
        com.amazonaws.sfc.ipc.Metrics.MetricsSourceType.TARGET_WRITER -> MetricsSourceType.TARGET_WRITER
        else -> MetricsSourceType.UNDEFINED
    }