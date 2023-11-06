
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.protocol

import com.amazonaws.sfc.modbus.protocol.Modbus.asHex
import kotlin.time.Duration

/**
 * Implements Modbus WriteSingleCoilResponse
 */
class WriteSingleCoilResponse(deviceID: DeviceID, transactionID: TransactionID? = null) :
        ResponseBase(
            deviceID = deviceID,
            function = Modbus.FUNCTION_CODE_WRITE_SINGLE_COIL,
            transactionID = transactionID
        ) {

    /**
     * Response as string
     * @return String
     */
    override fun toString(): String =
        if (error == null)
            "${super.toString()}, Output Address=${asHex(address)}, Output Value=${
                Modbus.discreteStr(
                    outputValue
                )
            }"
        else super.toString()

    /**
     * Value from response - outputValue
     * @see outputValue
     */
    override val value: Any
        get() = outputValue

    /**
     * Output value of coil from request
     */
    val outputValue: DiscreteValue
        get() = decodeDiscreteValues()[0]


    fun outputValueMapped(startAddress: Address) = mapOf(startAddress to outputValue)

    /**
     * Read response from transport
     * @param transport ModbusTransport Transport to read from
     * @param timeout Duration Timeout for reading response
     * @return UByteArray Raw response bytes
     */
    override suspend fun readResponse(transport: ModbusTransport, timeout: Duration): UByteArray {
        byteCount = 2u
        return ubyteArrayOf(
            deviceID,
            *readAddressData(transport, timeout),
            *readResponseData(transport, timeout)
        )
    }
}