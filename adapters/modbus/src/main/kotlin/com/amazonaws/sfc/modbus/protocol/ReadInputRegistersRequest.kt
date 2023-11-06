
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.protocol


/**
 * Implements Modbus ReadInputRegistersRequest
 * @property payload UByteArray
 * @constructor
 */
class ReadInputRegistersRequest(address: Address, deviceID: DeviceID, quantity: UShort = 1u, transactionID: TransactionID? = null) :
        RequestBase(
            address = address,
            deviceID = deviceID,
            function = Modbus.FUNCTION_CODE_READ_INPUT_REGISTERS,
            transactionID = transactionID,
            quantity = quantity
        ) {

    init {
        checkReadRegisterQuantity()
    }

    /**
     * Request payload
     */
    override val payload = super.readRequestPayload

    /**
     * Request as string
     * @return String
     */
    override fun toString(): String = "${super.toString()}, Quantity=$quantity"


}