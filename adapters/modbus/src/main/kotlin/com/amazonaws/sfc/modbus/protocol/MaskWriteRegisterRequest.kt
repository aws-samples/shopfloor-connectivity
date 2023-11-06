
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.protocol

import com.amazonaws.sfc.modbus.protocol.Modbus.asHex

// Implements Modbus MaskWriteRegisterRequest
class MaskWriteRegisterRequest(
    address: Address,
    deviceID: DeviceID,
    private val andMask: UShort = 0xFFFFu,
    private val orMask: UShort = 0x0000u,
    transactionID: TransactionID? = null) : RequestBase(
    address = address,
    deviceID = deviceID,
    function = Modbus.FUNCTION_CODE_MASK_WRITE_REGISTER,
    transactionID = transactionID,
    quantity = 1u) {

    override val payload = super.writeRequestPayload + encodeShort(andMask) + encodeShort(orMask)

    override fun toString(): String = "${super.toString()}, AND Mask = ${asHex(andMask)}, OR MASK=${asHex(orMask)}"

}