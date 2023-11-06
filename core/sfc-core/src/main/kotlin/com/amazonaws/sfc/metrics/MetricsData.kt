
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.metrics

class MetricsData(val source: String,
                  val sourceType: MetricsSourceType,
                  val dataPoints: List<MetricsDataPoint>,
                  val commonDimensions: MetricDimensions?) {
    override fun toString(): String {
        return "MetricsData(source=$source, sourceType=$sourceType, commonDimensions=$commonDimensions, dataPoints=$dataPoints)"
    }
}