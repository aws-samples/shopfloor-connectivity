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
import kotlin.time.Duration

/**
 * Implements Modbus ReadHoldingRegistersResponse
 */
class ReadHoldingRegistersResponse(deviceID: DeviceID, transactionID: TransactionID? = null) :
        ResponseBase(
            deviceID = deviceID,
            function = Modbus.FUNCTION_CODE_READ_HOLDING_REGISTERS,
            transactionID = transactionID
        ) {

    /**
     * Response as string
     * @return String
     */
    override fun toString(): String {
        return super.toString() + ", Register Values=${(registerValues.map { r -> asHex(r) }).joinToString(prefix = "[", postfix = "]")}"
    }

    /**
     * Values from response = registerValues
     * @see registerValues
     */
    override val value: Any
        get() = registerValues

    /**
     * Register values from response
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val registerValues: Array<RegisterValue>
        get() = decodeRegisterValues()

    /**
     * Maps register values to address range
     * @param startAddress UShort Start address
     * @return Mapping<Address, RegisterValue>
     */
    fun registerValuesMapped(startAddress: Address): Map<Address, RegisterValue> {
        val mapped = mutableMapOf<Address, RegisterValue>()
        registerValues.forEachIndexed { index, value -> mapped[(index + startAddress.toInt()).toAddress()] = value }
        return mapped
    }

    /**
     * Read response from transport
     * @param transport ModbusTransport Transport to read from
     * @param timeout Duration Timeout for reading response
     * @return UByteArray Raw response bytes
     */
    override suspend fun readResponse(transport: ModbusTransport, timeout: Duration): UByteArray {
        return ubyteArrayOf(
            deviceID,
            *readByteCount(transport, timeout),
            *readResponseData(transport, timeout)
        )
    }
}