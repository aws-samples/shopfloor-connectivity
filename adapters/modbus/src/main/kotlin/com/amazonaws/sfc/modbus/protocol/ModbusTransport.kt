
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.protocol


import kotlin.time.Duration

/**
 * Transport to send/receive data in modbus implementation
 */
interface ModbusTransport {
    /**
     * Writes bytes.
     * @param bytes UByteArray Bytes to write
     */
    suspend fun write(bytes: UByteArray)

    /**
     * Reads byte
     * @return UByte? Byte read
     */
    suspend fun read(): UByte?

    /**
     * Lock for exclusive access
     */
    suspend fun lock()

    /**
     * Unlock, release exclusive access
     */
    suspend fun unlock()

    /**
     * Close transport.
     * @param timeout Duration? Timeout for closing transport
     * @return Boolean True if transport was closed within timeout
     */
    suspend fun close(timeout: Duration? = null): Boolean
}