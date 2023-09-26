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
 * Implements WriteMultipleCoilsRequest
 */
class WriteMultipleCoilsRequest(address: Address, deviceID: DeviceID, val values: Array<DiscreteValue>, transactionID: TransactionID? = null) : RequestBase(
    address = address,
    deviceID = deviceID,
    function = Modbus.FUNCTION_CODE_WRITE_MULTIPLE_COILS,
    transactionID = transactionID,
    quantity = values.size.toUShort()) {

    init {
        checkWriteCoilsQuantity()
    }

    /**
     * Payload for the request.
     */
    override val payload = super.readRequestPayload + encodeDiscreteValues()

    override fun toString(): String = "${super.toString()}, Quantity=$quantity, Values=${
        values.joinToString(prefix = "[", postfix = "]") {
            Modbus.discreteStr(
                it
            )
        }
    }"

    /**
     * Encodes the discrete values
     * @return UByteArray
     */
    private fun encodeDiscreteValues(): UByteArray {

        val output = Array(((quantity.toInt() - 1) / 8 + 1) * 8) { if (it < quantity.toInt()) values[it] else Modbus.DISCRETE_OFF }

        // put values in order as they are transmitted
        for (i in output.indices step 8) {
            for (j in 0 until 4) {
                val i1 = i + j
                val i2 = i + 7 - j
                val temp = output[i1]
                output[i1] = output[i2]
                output[i2] = temp
            }
        }

        val outputBytes = UByteArray((values.size - 1) / 8 + 1)

        val zeroMask: UByte = 0x00u
        var mask: UByte = zeroMask

        for (i in output.indices) {
            val b = output[i]

            if (mask == zeroMask) {
                mask = 0x80u
            }

            val index = i / 8

            if (b != Modbus.DISCRETE_OFF) {
                outputBytes[index] = outputBytes[index] or mask
            }
            mask = (mask.toInt() shr 1).toUByte()
            continue
        }

        return ubyteArrayOf(*encodeShort(outputBytes.size.toUShort()), *outputBytes)
    }


}