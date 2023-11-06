
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.protocol

import com.amazonaws.sfc.modbus.protocol.Modbus.asHex
import kotlin.time.Duration

/**
 * Implements WriteMultipleCoilsResponse
 */
class WriteMultipleCoilsResponse(deviceID: DeviceID, transactionID: TransactionID? = null) :
        ResponseBase(
            deviceID = deviceID,
            function = Modbus.FUNCTION_CODE_WRITE_MULTIPLE_COILS,
            transactionID = transactionID
        ) {

    /**
     * Response as string
     * @return String
     */
    override fun toString(): String =
        if (error == null)
            "${super.toString()}, Output Address=${asHex(address)}, Quantity of Outputs=$quantityOfOutputs"
        else super.toString()


    /**
     * Value from the request = quantityOfOutputs
     * @see quantityOfOutputs
     */
    override val value: Any
        get() = quantityOfOutputs

    /**
     * Quantity of outputs from response
     */
    val quantityOfOutputs: UShort
        get() = decodeQuantity()


    /**
     * Read response from transport
     * @param transport ModbusTransport Transport to read from
     * @param timeout Duration Timeout for reading response
     * @return UByteArray Raw response bytes
     */
    override suspend fun readResponse(transport: ModbusTransport, timeout: Duration): UByteArray {
        return ubyteArrayOf(
            deviceID,
            *readAddressData(transport, timeout),
            *readQuantityData(transport, timeout)
        )
    }
}