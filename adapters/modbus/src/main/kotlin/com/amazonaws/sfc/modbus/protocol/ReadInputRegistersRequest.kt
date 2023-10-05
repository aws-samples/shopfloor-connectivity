/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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