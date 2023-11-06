
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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