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


import com.amazonaws.sfc.metrics.MetricsProvider


/**
 * Target writer interface
 */

interface TargetWriter {

    /**
     * Writes data to the target
     * @param targetData TargetData Data to be written
     */
    suspend fun writeTargetData(targetData: TargetData)
    suspend fun close()
    val isInitialized: Boolean
        get() = true

    val metricsProvider: MetricsProvider?

    companion object {
        const val TIMOUT_TARGET_WRITE = 60000L
    }

}

