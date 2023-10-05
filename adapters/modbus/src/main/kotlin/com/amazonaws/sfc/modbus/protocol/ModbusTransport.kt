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