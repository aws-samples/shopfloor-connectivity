/*
 Copyright (c) 2021. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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