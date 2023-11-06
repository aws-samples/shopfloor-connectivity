
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.protocol


/**
 * RequestInterface defines common Modbus request methods.
 */
// Interface for Modbus requests
interface Request {
    val address: Address
    val deviceID: DeviceID
    val function: FunctionCode
    val transactionID: TransactionID?
    val payload: Payload
    val quantity: UShort
}



