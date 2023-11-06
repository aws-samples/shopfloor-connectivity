
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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