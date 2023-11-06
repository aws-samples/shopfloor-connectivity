
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.protocol


/**
 * Implements Modbus ReadCoilsRequest
 */
class ReadCoilsRequest(address: Address, deviceID: DeviceID, transactionID: TransactionID? = null, quantity: UShort = 1u) :
        RequestBase(
            address = address,
            deviceID = deviceID,
            function = Modbus.FUNCTION_CODE_READ_COILS,
            transactionID = transactionID,
            quantity = quantity
        ) {

    init {
        checkCoilsOrInputsQuantity()
    }

    /**
     * Payload for the request
     */
    override val payload = super.readRequestPayload

    /**
     * Request as string
     * @return String
     */
    override fun toString(): String = "${super.toString()}, Quantity=$quantity"
}