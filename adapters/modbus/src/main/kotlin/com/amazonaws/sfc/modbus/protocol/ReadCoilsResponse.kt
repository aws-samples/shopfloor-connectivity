/*
 *
 *     Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *     Licensed under the Amazon Software License (the "License"). You may not use this  file except in  compliance with the License. A copy of the License is located at :
 *
 *       http://aws.amazon.com/asl/
 *
 *     or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific language governing permissions and limitations under the License.
 *
 */

package com.amazonaws.sfc.modbus.protocol

import kotlin.time.Duration

/**
 * Implements Modbus ReadCoilsResponse
 */
class ReadCoilsResponse(deviceID: DeviceID, transactionID: TransactionID? = null) :
        ResponseBase(
            function = Modbus.FUNCTION_CODE_READ_COILS,
            deviceID = deviceID,
            transactionID = transactionID
        ) {

    /**
     * Response as string
     * @return String
     */
    override fun toString(): String =
        if (error == null)
            "${super.toString()}, CoilStatus=${coilStatus.joinToString(prefix = "[", postfix = "]") { Modbus.discreteStr(it) }}"
        else super.toString()


    /**
     * Response value = coil status
     */
    override val value: Any
        get() = coilStatus

    /**
     * Coil status in response
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val coilStatus: Array<DiscreteValue>
        get() = decodeDiscreteValues()

    /**
     * Coil status from response mapped to address
     * @param startAddress UShort Address
     * @return Mapping<Address, DiscreteValue>
     */
    fun coilStatusMapped(startAddress: Address): Map<Address, DiscreteValue> {
        val mapped = mutableMapOf<Address, DiscreteValue>()
        coilStatus.forEachIndexed { index, value -> mapped[(index + startAddress.toInt()).toAddress()] = value }
        return mapped
    }

    /**
     * Reads the response from the device
     * @param transport ModbusTransport Transport to read response from
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
