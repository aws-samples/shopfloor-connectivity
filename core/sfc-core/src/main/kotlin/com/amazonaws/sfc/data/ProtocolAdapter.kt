/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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