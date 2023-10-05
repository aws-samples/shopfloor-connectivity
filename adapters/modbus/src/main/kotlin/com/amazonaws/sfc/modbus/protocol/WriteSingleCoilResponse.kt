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
 * Implements Modbus WriteSingleCoilResponse
 */
class WriteSingleCoilResponse(deviceID: DeviceID, transactionID: TransactionID? = null) :
        ResponseBase(
            deviceID = deviceID,
            function = Modbus.FUNCTION_CODE_WRITE_SINGLE_COIL,
            transactionID = transactionID
        ) {

    /**
     * Response as string
     * @return String
     */
    override fun toString(): String =
        if (error == null)
            "${super.toString()}, Output Address=${asHex(address)}, Output Value=${
                Modbus.discreteStr(
                    outputValue
                )
            }"
        else super.toString()

    /**
     * Value from response - outputValue
     * @see outputValue
     */
    override val value: Any
        get() = outputValue

    /**
     * Output value of coil from request
     */
    val outputValue: DiscreteValue
        get() = decodeDiscreteValues()[0]


    fun outputValueMapped(startAddress: Address) = mapOf(startAddress to outputValue)

    /**
     * Read response from transport
     * @param transport ModbusTransport Transport to read from
     * @param timeout Duration Timeout for reading response
     * @return UByteArray Raw response bytes
     */
    override suspend fun readResponse(transport: ModbusTransport, timeout: Duration): UByteArray {
        byteCount = 2u
        return ubyteArrayOf(
            deviceID,
            *readAddressData(transport, timeout),
            *readResponseData(transport, timeout)
        )
    }
}