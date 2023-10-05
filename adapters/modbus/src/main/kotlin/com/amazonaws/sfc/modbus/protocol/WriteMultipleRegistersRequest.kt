/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.modbus.protocol

import com.amazonaws.sfc.modbus.protocol.Modbus.asHex

/**
 * Implements WriteMultipleRegistersRequest
 */
class WriteMultipleRegistersRequest(address: Address, deviceID: DeviceID, val values: Array<RegisterValue>, transactionID: TransactionID? = null) : RequestBase(
    address = address,
    deviceID = deviceID,
    function = Modbus.FUNCTION_CODE_WRITE_MULTIPLE_REGISTERS,
    transactionID = transactionID,
    quantity = values.size.toUShort()) {

    init {
        checkWriteRegistersQuantity()
    }

    /**
     * Payload for the request
     */
    override val payload = super.writeRequestPayload + encodeRegisterValues()

    /**
     * Request as a string
     * @return String
     */
    override fun toString(): String = "${super.toString()}, Quantity=$quantity, Values=Values=${
        values.joinToString(prefix = "[", postfix = "]") {
            asHex(it)
        }
    } "


    /**
     * Encodes request register values to byte array
     * @return UByteArray
     */
    private fun encodeRegisterValues(): UByteArray {
        val buffer = UByteArray((quantity.toInt() * 2) + 2)

        encodeRegisterValue(quantity).copyInto(buffer)
        for (i in values.indices) {
            encodeRegisterValue(values[i]).copyInto(buffer, (i * 2) + 2)
        }
        return buffer
    }
}