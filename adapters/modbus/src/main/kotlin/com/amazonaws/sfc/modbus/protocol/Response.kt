
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.protocol

import kotlin.time.Duration

/**
 * Defines interface for a modbus response type
 */
interface Response {

    val function: FunctionCode
    val address: Address
    val deviceID: DeviceID
    val value: Any
    override fun toString(): String
    val transactionID: TransactionID?
    val error: ModbusError?

    /**
     * Read response from transport
     * @param transport ModbusTransport Transport to read from
     * @param timeout Duration Timeout for reading response
     * @return UByteArray Raw response bytes
     */
    suspend fun readResponse(transport: ModbusTransport, timeout: Duration = Modbus.READ_TIMEOUT): UByteArray
}


