
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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

