
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.protocol

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Interface for sending Modbus requests and receiving Modbus responses, RTU and TCP
 */
interface ModbusHandler {

    /**
     * Channel to pass requests to be send to the handler
     */
    val requests: SendChannel<Request>

    /**
     * Channel to read requests from the handler
     */
    val responses: ReceiveChannel<Response>

    /**
     * Transport used by the handler
     */
    val modbusDevice: ModbusTransport

    /**
     * Stops the handler
     * @param timeout Duration Timeout for stopping the handler
     * @return Boolean True if handler was stopped withing timeout period
     */
    suspend fun stop(timeout: Duration = 1.toDuration(DurationUnit.SECONDS)): Boolean
}