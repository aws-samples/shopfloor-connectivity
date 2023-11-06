
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.metrics

interface MetricsCollectorReader {

    suspend fun read(): List<MetricsData>?

    fun close()
}

// signature for reader consumer function
typealias MetricsConsumer = suspend (MetricsData) -> Boolean



