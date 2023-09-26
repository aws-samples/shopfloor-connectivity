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
 * Base class for Modbus requests
 */
abstract class RequestBase(
    final override val address: Address,
    final override val deviceID: DeviceID,
    final override val function: FunctionCode,
    final override val quantity: UShort = 1u,
    final override val transactionID: TransactionID?) : Request {

    /**
     * Request as string
     * @return String
     */
    override fun toString(): String {
        return Modbus.asString(transactionID, deviceID, function) + "Address=${address}"
    }

    /**
     * Checks coils or input quantity of a request
     */
    internal open fun checkCoilsOrInputsQuantity() {
        if ((quantity < 1u) or (quantity > Modbus.MAX_READ_COILS_INPUTS.toUInt())) {
            throw Modbus.ModbusException(Modbus.ERROR_INVALID_OR_COIL_INPUT_QUANTITY)
        }
    }

    /**
     * Checks register quantity of a read request
     */
    internal fun checkReadRegisterQuantity() {
        if ((quantity < 1u) or (quantity > Modbus.MAX_READ_REGISTERS.toUInt())) {
            throw Modbus.ModbusException(Modbus.ERROR_INVALID_REGISTER_QUANTITY)
        }
    }

    /**
     * Checks coils quantity of a write request
     */
    internal fun checkWriteCoilsQuantity() {
        if ((quantity < 1u) or (quantity > Modbus.MAX_OUTPUT_COILS.toUInt())) {
            throw Modbus.ModbusException(Modbus.ERROR_INVALID_NUMBER_OF_OUTPUT_COILS)
        }
    }

    /**
     * Checks register quantity of a write request
     */
    internal fun checkWriteRegistersQuantity() {
        if ((quantity < 1u) or (quantity > Modbus.MAX_OUTPUT_REGISTERS.toUInt())) {
            throw Modbus.ModbusException(Modbus.ERROR_INVALID_REGISTER_QUANTITY)
        }
    }

    /**
     * Payload to send for a request
     */
    abstract override val payload: Payload

    // startAddressBytes returns the bytes for the start address as bytes in MSB, LSB order, used in the payload for the request.
    private val startAddressBytes = ubyteArrayOf(((this.address - 1u) shr 8).toUByte(), ((this.address) - 1u and 0xffu).toUByte())

    // startAddressBytes returns the bytes for the start address as bytes in MSB, LSB order, used in the payload for the request.
    private val quantityBytes = encodeShort(this.quantity)

    protected val readRequestPayload: Payload = ubyteArrayOf(this.function, *startAddressBytes, *quantityBytes)

    protected val writeRequestPayload = ubyteArrayOf(this.function, *startAddressBytes)


    companion object {
        /**
         * Encodes a short to byte array
         * @param s UShort value to encode
         * @return UByteArray
         */
        fun encodeShort(s: UShort) = ubyteArrayOf((s.toUInt() shr 8).toUByte(), (s and 0xffu).toUByte())

        /**
         * Encodes a register value to byte array
         * @param r UShort Value to encode
         * @return UByteArray
         */
        internal fun encodeRegisterValue(r: RegisterValue) = encodeShort(r)

        /**
         * Encodes a discrete value to a byte array
         * @param d Boolean Value to encode
         * @return UByteArray
         */
        internal fun encodeDiscreteValue(d: DiscreteValue): UByteArray = if (d == Modbus.DISCRETE_ON) ubyteArrayOf(0xFFu, 0x00u) else ubyteArrayOf(0x00u, 0x00u)
    }

}