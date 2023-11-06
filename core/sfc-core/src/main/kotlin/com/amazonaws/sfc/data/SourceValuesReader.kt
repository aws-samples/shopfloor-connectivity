
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data

import com.amazonaws.sfc.metrics.MetricsProvider

/**
 * Interface for a source reader
 */
interface SourceValuesReader {

    // read method will call the consumer method for every source data item, until that method return false.
    // This is similar to a Kotlin flow with a collector, but this interface makes interoperability for Java
    // implementations easier by hiding the Kotlin specific Flow.
    suspend fun read(consumer: ReadResultConsumer)

    val isInitialized: Boolean

    val metricsProvider: MetricsProvider?

    /**
     * Close the reader
     */
    suspend fun close()
}

// signature for reader consumer function
typealias ReadResultConsumer = suspend (ReadResult) -> Boolean
