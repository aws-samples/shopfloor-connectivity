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

import com.amazonaws.sfc.modbus.protocol.Modbus.asHex
import kotlin.time.Duration

/**
 * Implements Modbus MaskWriteRegisterResponse
 */
class MaskWriteRegisterResponse(deviceID: DeviceID, transactionID: TransactionID? = null) :
        ResponseBase(deviceID = deviceID, function = Modbus.FUNCTION_CODE_MASK_WRITE_REGISTER, transactionID = transactionID) {

    /**
     * Returns string representation of response
     * @return String
     **/
    override fun toString(): String =
        if (error == null)
            "${super.toString()}, Output Address=${asHex(address)}, AND Mask=${asHex(andMask)}, OR Mask=${asHex(orMask)}"
        else super.toString()

    /**
     * Returns values of AND and OR mask from the response
     **/
    override val value: Any
        get() = ushortArrayOf(andMask, orMask)

    /**
     * Returns the AND mask from the response
     */
    val andMask: UShort by lazy { decodeRegisterValues()[0] }

    /**
     * Returns the OR mask from the response
     */
    val orMask: UShort by lazy { decodeRegisterValues()[1] }

    /**
     * Reads and decodes the response from the transport
     * @param transport ModbusTransport Transport to read bytes from
     * @param timeout Duration Timeout for receiving a response
     * @return UByteArray Raw response bytes
     */
    override suspend fun readResponse(transport: ModbusTransport, timeout: Duration): UByteArray {
        return ubyteArrayOf(
            deviceID,
            *readAddressData(transport, timeout),
            *readMaskData(transport, timeout)
        )
    }
}
