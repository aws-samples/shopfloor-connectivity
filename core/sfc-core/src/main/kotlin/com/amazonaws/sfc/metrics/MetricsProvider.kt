
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.metrics

import kotlin.time.Duration

interface MetricsProvider {
    // read method will call the consumer method for every metric data item, until that method return false.
    // This is similar to a Kotlin flow with a collector, but this interface makes interoperability for Java
    // implementations easier by hiding the Kotlin specific Flow.
    suspend fun read(interval: Duration, consumer: MetricsConsumer)
    suspend fun close()
}