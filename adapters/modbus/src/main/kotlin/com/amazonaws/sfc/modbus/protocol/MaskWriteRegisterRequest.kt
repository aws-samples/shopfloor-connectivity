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