
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.protocol

import com.amazonaws.sfc.modbus.protocol.Modbus.asHex

/**
 * Implements Modbus WriteSingleRegisterRequest
 */
class WriteSingleRegisterRequest(address: Address, deviceID: DeviceID, val value: RegisterValue, transactionID: TransactionID? = null) :
        RequestBase(
            address = address,
            deviceID = deviceID,
            function = Modbus.FUNCTION_CODE_WRITE_SINGLE_REGISTER,
            transactionID = transactionID,
            quantity = 1u
        ) {

    /**
     * Payload for request
     */
    override val payload = super.writeRequestPayload + encodeRegisterValue(value)

    /**
     * Request as a string
     * @return String
     */
    override fun toString(): String = "${super.toString()}, Value=${asHex(value)}"

}