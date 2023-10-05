/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

@file:Suppress("MemberVisibilityCanBePrivate")

package com.amazonaws.sfc.modbus.protocol

import com.amazonaws.sfc.data.ProtocolAdapterException
import kotlin.time.Duration

/**
 * Implements Modbus protocol error
 */
class ModbusError(val errorCode: ErrorCode) {

    private var _exceptionCode: ExceptionCode = 0u

    /**
     * Modbus exception code
     */
    val exceptionCode
        get() = _exceptionCode

    /**
     * String representation of the error
     * @return String
     */
    override fun toString(): String = "ErrorCode=$errorCode, ExceptionCode=$exceptionCode"

    suspend fun readError(transport: ModbusTransport, timeout: Duration): UByteArray {
        val bytes = ResponseBase.readResponseBytes(transport, 1, timeout)
        _exceptionCode = if (!bytes.isNullOrEmpty()) bytes[0] else throw ProtocolAdapterException("No modbus exception code")
        if (!Modbus.isValidExceptionCode(_exceptionCode)) {
            throw Modbus.ModbusException(Modbus.ERROR_INVALID_EXCEPTION_CODE)
        }

        val r = UByteArray(2) { 0u }
        r[0] = errorCode
        r[1] = _exceptionCode
        return r

    }

}