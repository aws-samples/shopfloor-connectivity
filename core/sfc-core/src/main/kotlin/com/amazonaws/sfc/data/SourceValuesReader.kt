/*
 Copyright (c) 2021. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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
