
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.service

/**
 * Interface for service running SFC core, protocol adapter or target IPC service process
 */
interface Service {
    /**
     * Start the service
     */
    suspend fun start()

    suspend fun stop()

    /**
     * Blocks until service is shut down
     */
    suspend fun blockUntilShutdown()

}