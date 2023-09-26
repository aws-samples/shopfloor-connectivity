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

import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import kotlin.time.Duration

/**
 * Base class for Modbus responses
 */
abstract class ResponseBase(
    final override val deviceID: DeviceID,
    final override val function: FunctionCode,
    final override val transactionID: TransactionID? = null) : Response {

    protected var byteCount: UByte = 0u
    private var _responseData: UByteArray? = null

    /**
     * Value(s) from a response
     */
    abstract override val value: Any

    private var _address: Address? = null

    /**
     * Response address
     */
    override val address
        get() = _address ?: 0u

    private var _error: ModbusError? = null

    /**
     * Error from a response
     */
    override val error: ModbusError?
        get() = _error

    /**
     * Read response from transport
     * @param transport ModbusTransport Transport to read from
     * @param timeout Duration Timeout for reading response
     * @return UByteArray Raw response bytes
     */
    abstract override suspend fun readResponse(transport: ModbusTransport, timeout: Duration): UByteArray

    /**
     * Response as string
     * @return String
     */
    override fun toString(): String {

        val s = mutableListOf(Modbus.asString(transactionID, deviceID, function))

        if (_address != null) {
            s.add("Address=${address}")
        }
        if (_error != null) {
            s.add("Modbus Error=${error}")
        }
        return s.joinToString()
    }

    /**
     * Reads the byte count from the transport
     * @param transport ModbusTransport Transport to read from
     * @param timeout Duration Read timeout
     * @return UByteArray Raw bytes
     */
    suspend fun readByteCount(transport: ModbusTransport, timeout: Duration = Modbus.READ_TIMEOUT): UByteArray {
        val data = readResponseBytes(transport, 1, timeout)
                   ?: throw Modbus.ModbusException("error reading MODBUS response byte count")
        if (data[0].toInt() <= 0) {
            throw Modbus.ModbusException("invalid value 0x${data[0].toString(16).padStart(2, '0')} for MODBUS response byte count")
        }
        byteCount = data[0]

        return data
    }

    /**
     * Reads the address from the transport
     * @param transport ModbusTransport Transport to read from
     * @param timeout Duration Read timeout
     * @return UByteArray Raw bytes
     */
    suspend fun readAddressData(transport: ModbusTransport, timeout: Duration = Modbus.READ_TIMEOUT): UByteArray {

        val data = readResponseBytes(transport, 2, timeout)
                   ?: throw Modbus.ModbusException("error reading MODBUS response address")

        _address = ((data[0].toUInt() shl 8) or (data[1].toUInt()) + 1u).toAddress()

        return data
    }

    /**
     * Reads the error from the transport
     * @param transport ModbusTransport Transport to read from
     * @param timeout Duration Read timeout
     * @return UByteArray Raw bytes
     */
    suspend fun readError(transport: ModbusTransport, errorCode: ErrorCode, timeout: Duration = Modbus.READ_TIMEOUT): UByteArray {
        _error = ModbusError(errorCode = errorCode)
        return _error!!.readError(transport, timeout)
    }

    /**
     * Reads the response data (e.g. values) from the transport
     * @param transport ModbusTransport Transport to read from
     * @param timeout Duration Read timeout
     * @return UByteArray Raw bytes
     */
    suspend fun readResponseData(transport: ModbusTransport, timeout: Duration = Modbus.READ_TIMEOUT): UByteArray {
        _responseData = readResponseBytes(transport, byteCount.toInt(), timeout)
                        ?: throw Modbus.ModbusException("error reading MODBUS response data")

        return _responseData as UByteArray
    }

    /**
     * Reads the quantity from the transport
     * @param transport ModbusTransport Transport to read from
     * @param timeout Duration Read timeout
     * @return UByteArray Raw bytes
     */
    suspend fun readQuantityData(transport: ModbusTransport, timeout: Duration = Modbus.READ_TIMEOUT): UByteArray {
        byteCount = 2u
        _responseData = readResponseBytes(transport, 2, timeout)
                        ?: throw Modbus.ModbusException("error reading MODBUS quantity data")

        return _responseData as UByteArray
    }

    /**
     * Reads the mask data from the transport
     * @param transport ModbusTransport Transport to read from
     * @param timeout Duration Read timeout
     * @return UByteArray Raw bytes
     */
    suspend fun readMaskData(transport: ModbusTransport, timeout: Duration = Modbus.READ_TIMEOUT): UByteArray {
        byteCount = 4u
        _responseData = readResponseBytes(transport, 4, timeout)
                        ?: throw Modbus.ModbusException("error reading MODBUS mask data")

        return _responseData as UByteArray
    }


    companion object {

        /**
         * Reads the specified number of bytes from the transport
         * @param transport ModbusTransport Transport to read from
         * @param n Int Number of bytes to read
         * @param timeout Duration Read timeout
         * @return UByteArray? Read bytes
         */
        suspend fun readResponseBytes(transport: ModbusTransport, n: Int, timeout: Duration): UByteArray? {

            val buffer = UByteArray(n)
            var i = 0
            repeat(n) {
                val b = withTimeoutOrNull(timeout.inWholeMilliseconds) {
                    transport.read()
                } ?: return null

                buffer[i] = b
                i++
                yield()
            }
            return buffer
        }
    }


    /**
     * Decodes coil and discrete input values. The result is a byte array with values 0 or 1 ordered in
     * the order of the coils or discrete inputs.
     * @return Array<DiscreteValue>
     */
    fun decodeDiscreteValues(): Array<DiscreteValue> {

        val result = Array(byteCount.toInt() * 8) { Modbus.DISCRETE_OFF }

        var mask = 0x00

        for (i in 0 until byteCount.toInt() * 8) {
            mask = mask shr 1
            if (mask == 0) {
                mask = 0x80
            }
            val index = (i + 8) / 8 - 1

            if (((_responseData?.get(index)?.toInt() ?: 0) and mask) != 0) {
                result[i] = Modbus.DISCRETE_ON
            }
        }
        for (i in 0 until byteCount.toInt()) {
            for (j in 0 until 4) {
                val i1 = i * 8 + j
                val i2 = i * 8 + (j - 8) * -1 - 1
                val temp = result[i1]
                result[i1] = result[i2]
                result[i2] = temp
            }
        }
        return result
    }

    /**
     * Decodes register values from input of registers on LSB,MSB order.
     * @return Array<RegisterValue>
     */
    fun decodeRegisterValues(): Array<RegisterValue> {
        val dataLen = (_responseData?.size ?: 0)
        //val n = (byteCount) / 2u
        val result = Array(dataLen / 2) { 0.toRegisterValue() }
        // convert registers from pairs of two bytes, if the buffer contains an odd number of bytes, the last bytes is ignored
        for (i in 0 until dataLen - 1 step 2) {
            result[i / 2] = (((_responseData?.get(i)?.toInt() ?: 0) shl 8) or (_responseData?.get(i + 1)?.toInt()
                                                                               ?: 0)).toRegisterValue()
        }
        return result
    }

    /**
     * Decodes quantity bytes
     * @return UShort
     */
    fun decodeQuantity(): UShort {
        return decodeRegisterValues()[0]
    }
}