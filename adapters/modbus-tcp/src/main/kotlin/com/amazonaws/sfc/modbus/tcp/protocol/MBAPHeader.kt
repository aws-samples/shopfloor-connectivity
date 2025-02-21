
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.tcp.protocol

import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.modbus.protocol.*
import com.amazonaws.sfc.util.asHexString
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

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

    private suspend fun readTransactionIdAndProtocolID(deviceID : String, transport: ModbusTransport, readTimeout: Duration, transactionIDHigh: UByte, logger : Logger) {

        val trace = logger.getCtxTraceLog("MBAPHeader", "readTransactionIdAndProtocolID")

        trace("Start reading MBAP transaction ID and protocol ID from adapter device \"$deviceID\"")

        val timeoutMoment = System.currentTimeMillis() + readTimeout.inWholeMilliseconds

        // transaction ID bytes
        var transactionHigh = transactionIDHigh
        val bytes: UByteArray = ResponseBase.readResponseBytes(transport, n = 3, timeout = readTimeout) ?:  throw Modbus.ModbusException("timeout MBAP reading transactionID and protocol ID")
        var transactionIdLow = bytes[0]

        // protocol ID bytes
        var protocolIDHigh = bytes[1]
        var protocolIDLow = bytes[2]

        // Read until the expected protocol ID is read
        while (!( arrayOf(protocolIDHigh, protocolIDLow).toUByteArray() contentEquals ModbusTcpProtocolID)){
            val timeRemaining = max(timeoutMoment - System.currentTimeMillis().toInt(), 0)
            trace("Received protocol ID bytes ${protocolIDHigh.asHexString()}${protocolIDLow.asHexString()} which are not the expected protocol bytes ${ModbusTcpProtocolID.map { it.asHexString() }}, reading next byte, remaining time is $timeRemaining ms")
            if (timeRemaining == 0L) {
                throw Modbus.ModbusException("timeout MBAP reading transactionID and protocol ID from adapter device \"$deviceID\"")
            }
            transactionHigh = transactionIdLow
            transactionIdLow = protocolIDHigh
            protocolIDHigh = protocolIDLow

            // read the next byte which could be the low byte of the protocol ID
            protocolIDLow  = ResponseBase.readResponseBytes(transport, n = 1, timeout = timeRemaining.toDuration(DurationUnit.MILLISECONDS))?.firstOrNull() ?: throw Modbus.ModbusException("timeout MBAP reading transactionID and protocol ID")
        }

        // now with a valid protocol ID we know the previous 2 bytes dir contain the transaction ID
        _transactionID = (transactionHigh.toInt() shl 8).toTransactionID() or transactionIdLow.toTransactionID()
        trace("Read MBAP transaction ID $_transactionID and protocol ID ${protocolIDHigh.asHexString()}${protocolIDLow.asHexString()} from device \"$deviceID\"")
    }


    private suspend fun readResponseLength(deviceID: String, transport: ModbusTransport, readTimeout: Duration, logger : Logger) {
        val trace = logger.getCtxTraceLog("MBAPHeader", "readResponseLength")
        trace("Start reading MBAP header length from adapter device \"$deviceID\"")
        val l = ResponseBase.readResponseBytes(transport, n = 2, timeout = readTimeout)
                ?: throw Modbus.ModbusException("timeout reading MBAP length")

        _length = (l[0].toInt() shl 8).toUShort() or l[1].toUShort()
        trace("Read MBAP header length $_length from adapter device \"$deviceID\"")
    }

    /**
     * Reads Modbus TCP MBAP header unit ID
     * @param transport ModbusTransport Transport to read from
     * @param readTimeout Duration Timeout for reading the unit ID
     */
    private suspend fun readUnitID(deviceID : String, transport: ModbusTransport, readTimeout: Duration, logger : Logger) {

        val trace = logger.getCtxTraceLog("MBAPHeader", "readUnitID")
        trace("Start reading MBAP header unit ID from adapter device \"$deviceID\"")
        val unitID = ResponseBase.readResponseBytes(transport, n = 1, timeout = readTimeout)
                     ?: throw Modbus.ModbusException("timeout reading MBAP unit ID")
        _unitID = unitID[0]
        trace("Read MBAP header unit ID $_unitID from adapter device \"$deviceID\"")
    }

    internal suspend fun read(deviceID : String, transport: ModbusTransport, readTimeout: Duration, transactionIDHigh: UByte, logger : Logger) {
        readTransactionIdAndProtocolID(deviceID, transport, readTimeout = readTimeout, transactionIDHigh = transactionIDHigh, logger)
        readResponseLength( deviceID, transport, readTimeout = readTimeout, logger)
        readUnitID(deviceID, transport, readTimeout = readTimeout, logger = logger)
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


        internal suspend fun read(deviceID : String,device: ModbusTransport, readTimeout: Duration, transactionIDHigh: UByte, logger : Logger): MBAPHeader {
            val header = MBAPHeader()
            header.read(deviceID =deviceID, transport =device, readTimeout = readTimeout, transactionIDHigh = transactionIDHigh, logger = logger)
            return header
        }


        internal fun create(request: Request): MBAPHeader {
            val header = MBAPHeader()
            header.init(request)
            return header
        }
    }
}