
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data

import com.amazonaws.sfc.metrics.MetricsCollectorReader
import kotlin.time.Duration

/**
 * Interface for source protocol input adapter
 */
interface ProtocolAdapter {

    suspend fun init() {
    }

    // Read channel values from a source
    suspend fun read(sourceID: String, channels: List<String>?): SourceReadResult

    // Stop the adapter
    suspend fun stop(timeout: Duration)

    val metricsCollector: MetricsCollectorReader?

}