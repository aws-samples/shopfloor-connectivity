/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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