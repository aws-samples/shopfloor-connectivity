
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.protocol

import kotlin.time.Duration

/**
 * Implements Modbus ReadDiscreteInputsResponse
 */
class ReadDiscreteInputsResponse(deviceID: DeviceID, transactionID: TransactionID? = null) :
        ResponseBase(
            deviceID = deviceID,
            function = Modbus.FUNCTION_CODE_READ_DISCRETE_INPUTS,
            transactionID = transactionID
        ) {

    /**
     * Response as string
     * @return String
     */
    override fun toString(): String {
        return super.toString() + ", Input Status=${inputStatus.joinToString(prefix = "[", postfix = "]") { Modbus.discreteStr(it) }}"
    }

    /**
     * Value in response = input status.
     * @see inputStatus
     */
    override val value: Any
        get() = inputStatus

    /**
     * Input status from response.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val inputStatus: Array<DiscreteValue>
        get() = decodeDiscreteValues()

    /**
     * Input status values mapped to address range
     * @param startAddress UShort Start address
     * @return Mapping<Address, DiscreteValue>
     */
    fun inputStatusMapped(startAddress: Address): Map<Address, DiscreteValue> {
        val mapped = mutableMapOf<Address, DiscreteValue>()
        inputStatus.forEachIndexed { index, value -> mapped[(index + startAddress.toInt()).toAddress()] = value }
        return mapped
    }

    /**
     * Read response from transport
     * @param transport ModbusTransport Transport to read from
     * @param timeout Duration Timeout for reading response
     * @return UByteArray Raw response bytes
     */
    override suspend fun readResponse(transport: ModbusTransport, timeout: Duration): UByteArray {
        return ubyteArrayOf(
            deviceID,
            *readByteCount(transport, timeout),
            *readResponseData(transport, timeout)
        )
    }
}