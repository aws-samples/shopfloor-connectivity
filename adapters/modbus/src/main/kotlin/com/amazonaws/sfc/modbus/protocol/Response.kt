/*
 *
 *    Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
 * Defines interface for a modbus response type
 */
interface Response {

    val function: FunctionCode
    val address: Address
    val deviceID: DeviceID
    val value: Any
    override fun toString(): String
    val transactionID: TransactionID?
    val error: ModbusError?

    /**
     * Read response from transport
     * @param transport ModbusTransport Transport to read from
     * @param timeout Duration Timeout for reading response
     * @return UByteArray Raw response bytes
     */
    suspend fun readResponse(transport: ModbusTransport, timeout: Duration = Modbus.READ_TIMEOUT): UByteArray
}


