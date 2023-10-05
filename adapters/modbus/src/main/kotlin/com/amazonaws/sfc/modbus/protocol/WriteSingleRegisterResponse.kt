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
import kotlin.time.Duration

/**
 * Implements Modbus class WriteSingleRegisterResponse
 */
class WriteSingleRegisterResponse(deviceID: DeviceID, transactionID: TransactionID? = null) :
        ResponseBase(
            deviceID = deviceID,
            function = Modbus.FUNCTION_CODE_WRITE_SINGLE_REGISTER,
            transactionID = transactionID
        ) {

    /**
     * Response as string
     * @return String
     */
    override fun toString(): String {
        return super.toString() + ", Register Address=${asHex(address)}, Register Value=${
            asHex(registerValue)
        }"
    }

    /**
     * Value from response = registerValue
     */
    override val value: Any
        get() = registerValue

    /**
     * Value from register in response
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val registerValue: RegisterValue
        get() = decodeRegisterValues()[0]

    /**
     * Maps register to address
     * @param startAddress UShort Address
     * @return Mapping<Address, RegisterValue>
     */
    @Suppress("unused")
    fun registerValueMapped(startAddress: Address): Map<Address, RegisterValue> = mapOf(startAddress to registerValue)

    override suspend fun readResponse(transport: ModbusTransport, timeout: Duration): UByteArray {
        byteCount = 2u
        return ubyteArrayOf(
            deviceID,
            *readAddressData(transport, timeout),
            *readResponseData(transport, timeout)
        )
    }
}