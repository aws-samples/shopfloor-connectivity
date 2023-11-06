/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.modbus.protocol

import kotlin.time.Duration

/**
 * Implements Modbus ReadCoilsResponse
 */
class ReadCoilsResponse(deviceID: DeviceID, transactionID: TransactionID? = null) :
        ResponseBase(
            function = Modbus.FUNCTION_CODE_READ_COILS,
            deviceID = deviceID,
            transactionID = transactionID
        ) {

    /**
     * Response as string
     * @return String
     */
    override fun toString(): String =
        if (error == null)
            "${super.toString()}, CoilStatus=${coilStatus.joinToString(prefix = "[", postfix = "]") { Modbus.discreteStr(it) }}"
        else super.toString()


    /**
     * Response value = coil status
     */
    override val value: Any
        get() = coilStatus

    /**
     * Coil status in response
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val coilStatus: Array<DiscreteValue>
        get() = decodeDiscreteValues()

    /**
     * Coil status from response mapped to address
     * @param startAddress UShort Address
     * @return Mapping<Address, DiscreteValue>
     */
    fun coilStatusMapped(startAddress: Address): Map<Address, DiscreteValue> {
        val mapped = mutableMapOf<Address, DiscreteValue>()
        coilStatus.forEachIndexed { index, value -> mapped[(index + startAddress.toInt()).toAddress()] = value }
        return mapped
    }

    /**
     * Reads the response from the device
     * @param transport ModbusTransport Transport to read response from
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
