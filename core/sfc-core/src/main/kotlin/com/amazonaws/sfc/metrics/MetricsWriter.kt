
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.metrics

interface MetricsWriter {
    suspend fun writeMetricsData(metricsData: MetricsData)
    suspend fun close()
}