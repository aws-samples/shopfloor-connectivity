
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


@file:Suppress("unused", "unused")

package com.amazonaws.sfc.modbus.protocol


/**
 * Implements Modbus WriteSingleCoilRequest
 */
class WriteSingleCoilRequest(address: Address, deviceID: DeviceID, val value: DiscreteValue, transactionID: TransactionID? = null) :
        RequestBase(
            address = address,
            deviceID = deviceID,
            function = Modbus.FUNCTION_CODE_WRITE_SINGLE_COIL,
            transactionID = transactionID,
            quantity = 1u
        ) {

    /**
     * Payload for request
     */
    override val payload = super.writeRequestPayload + encodeDiscreteValue(value)

    /**
     * Request as string
     * @return String
     */
    override fun toString(): String = "${super.toString()}, Value=${Modbus.discreteStr(value)}"

}