
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.tcp.protocol

import com.amazonaws.sfc.modbus.protocol.*
import com.amazonaws.sfc.modbus.protocol.Modbus.asHex
import kotlin.time.Duration

/**
 * Implements Modbus TCP MBAP header
 */
class MBAPHeader {

    private var _transactionID: TransactionID = 0u

    /**
     * Modbus TCP MBAP header transaction number
     */
    val transactionID: TransactionID
        get() = _transactionID

    private var _length: UShort = 0u

    /**
     * Modbus TCP MBAP header length
     */
    val length
        get() = _length

    private var _unitID: DeviceID = 0u

    /**
     * Modbus TCP MBAP header unit number
     */
    val unitID
        get() = _unitID

    /**
     * Reads Modbus TCP  MBAP header transaction ID
     * @param transport ModbusTransport Transport to read from
     * @param readTimeout Duration Timeout for reading the ID
     * @param transactionIDHigh UByte High bytes already read for transaction ID
     */
    private suspend fun readTransactionId(transport: ModbusTransport, readTimeout: Duration, transactionIDHigh: UByte) {
        val transactionIdLow = ResponseBase.readResponseBytes(transport, timeout = readTimeout, n = 1)
                               ?: throw Modbus.ModbusException("timeout reading MBAP transaction ID LOW")
        _transactionID = (transactionIDHigh.toInt() shl 8).toTransactionID() or transactionIdLow[0].toTransactionID()
    }

    /**
     * Reads Modbus TCP MBAP header protocol ID
     * @param transport ModbusTransport Transport to read from
     * @param readTimeout Duration Timeout for reading the protocol ID
     */
    private suspend fun readProtocolId(transport: ModbusTransport, readTimeout: Duration) {
        val id = ResponseBase.readResponseBytes(transport, timeout = readTimeout, n = 2)
                 ?: throw Modbus.ModbusException("timeout MBAP reading protocol ID")

        if (!(id contentEquals ModbusTcpProtocolID)) {
            val received = "${asHex(id[0])} ${asHex(id[1])}"
            val expected = "${asHex(ModbusTcpProtocolID[0])} ${
                asHex(ModbusTcpProtocolID[1])
            }"
            throw Modbus.ModbusException("$received is not the expected protocol ID $expected")
        }
    }

    /**
     * Reads Modbus TCP MBAP header response length
     * @param transport ModbusTransport Transport to read from
     * @param readTimeout Duration Timeout for reading the protocol ID
     */
    private suspend fun readResponseLength(transport: ModbusTransport, readTimeout: Duration) {
        val l = ResponseBase.readResponseBytes(transport, timeout = readTimeout, n = 2)
                ?: throw Modbus.ModbusException("timeout reading MBAP length")
        _length = (l[0].toInt() shl 8).toUShort() or l[1].toUShort()
    }

    /**
     * Reads Modbus TCP MBAP header unit ID
     * @param transport ModbusTransport Transport to read from
     * @param readTimeout Duration Timeout for reading the unit ID
     */
    private suspend fun readUnitID(transport: ModbusTransport, readTimeout: Duration) {

        val unitID = ResponseBase.readResponseBytes(transport, timeout = readTimeout, n = 1)
                     ?: throw Modbus.ModbusException("timeout reading MBAP unit ID")
        _unitID = unitID[0]
    }

    // Reads byte from the transport
    internal suspend fun read(transport: ModbusTransport, readTimeout: Duration, transactionIDHigh: UByte) {
        readTransactionId(transport, readTimeout = readTimeout, transactionIDHigh = transactionIDHigh)
        readProtocolId(transport, readTimeout = readTimeout)
        readResponseLength(transport, readTimeout = readTimeout)
        readUnitID(transport, readTimeout = readTimeout)
    }

    internal fun init(request: Request) {
        _transactionID = request.transactionID ?: 0u
        _unitID = request.deviceID
        _length = (request.payload.size + 1).toUShort()
    }

    /**
     * Payload of the encoded MBAP header
     */
    val payload: UByteArray
        get() = RequestBase.encodeShort(transactionID) + ModbusTcpProtocolID + RequestBase.encodeShort(
            _length
        ) + ubyteArrayOf(unitID)


    companion object {

        /**
         * Protocol ID for Modbus TCP
         */
        val ModbusTcpProtocolID = ubyteArrayOf(0u, 0u)

        /**
         * Reads a MBAP header
         * @param device ModbusTransport Transport to read from
         * @param readTimeout Duration Timeout for reading the header
         * @param transactionIDHigh UByte High byte already read for the header
         * @return MBAPHeader Read MBAP header
         */
        internal suspend fun read(device: ModbusTransport, readTimeout: Duration, transactionIDHigh: UByte): MBAPHeader {
            val header = MBAPHeader()
            header.read(device, readTimeout = readTimeout, transactionIDHigh = transactionIDHigh)
            return header
        }

        /**
         * Creates a MBAP header for a request
         * @param request Request
         * @return MBAPHeader
         */
        internal fun create(request: Request): MBAPHeader {
            val header = MBAPHeader()
            header.init(request)
            return header
        }
    }
}