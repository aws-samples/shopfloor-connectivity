
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.protocol

import com.amazonaws.sfc.modbus.protocol.Modbus.asHex
import kotlin.time.Duration

/**
 * Implements Modbus class WriteSingleRegisterResponse
 */
class WriteSingleRegisterResponse(deviceID: DeviceID, transactionID: TransactionID? = null) :
        ResponseBase(
            deviceID = deviceID,
            function = Modbus.FUNCTION_CODE_WRITE_SINGLE_REGISTER,
            transactionID = transactionID
        ) {

    /**
     * Response as string
     * @return String
     */
    override fun toString(): String {
        return super.toString() + ", Register Address=${asHex(address)}, Register Value=${
            asHex(registerValue)
        }"
    }

    /**
     * Value from response = registerValue
     */
    override val value: Any
        get() = registerValue

    /**
     * Value from register in response
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val registerValue: RegisterValue
        get() = decodeRegisterValues()[0]

    /**
     * Maps register to address
     * @param startAddress UShort Address
     * @return Mapping<Address, RegisterValue>
     */
    @Suppress("unused")
    fun registerValueMapped(startAddress: Address): Map<Address, RegisterValue> = mapOf(startAddress to registerValue)

    override suspend fun readResponse(transport: ModbusTransport, timeout: Duration): UByteArray {
        byteCount = 2u
        return ubyteArrayOf(
            deviceID,
            *readAddressData(transport, timeout),
            *readResponseData(transport, timeout)
        )
    }
}