
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.protocol


/**
 * Implements Modbus ReadDiscreteInputsRequest
 * @property payload UByteArray
 * @constructor
 */
class ReadDiscreteInputsRequest(address: Address, deviceID: DeviceID, quantity: UShort = 1u, transactionID: TransactionID? = null) :
        RequestBase(
            address = address,
            deviceID = deviceID,
            function = Modbus.FUNCTION_CODE_READ_DISCRETE_INPUTS,
            transactionID = transactionID,
            quantity = quantity
        ) {

    init {
        checkCoilsOrInputsQuantity()
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