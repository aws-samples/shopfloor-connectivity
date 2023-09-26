/*
 Copyright (c) 2021. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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