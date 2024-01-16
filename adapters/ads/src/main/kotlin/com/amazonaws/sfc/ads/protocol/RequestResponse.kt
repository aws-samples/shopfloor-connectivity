/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.ads.protocol

import com.amazonaws.sfc.ads.protocol.Decoder.bytes
import com.amazonaws.sfc.ads.protocol.Decoder.toInt16
import com.amazonaws.sfc.ads.protocol.Decoder.toInt32
import com.amazonaws.sfc.util.asHexString
import java.io.ByteArrayOutputStream

open class RequestResponse(
    val targetAmsNetId: String,
    val targetAmsPort: Int,
    val sourceAmsNetId: String,
    val sourceAmsPort: Int,
    val commandId: CommandId,
    val data: ByteArray? = null,
    val stateFlags: Short = AMS_COMMAND_STATUS,
    val result: Int = 0,
    val invokeId: Int = 0
) {

    constructor(a: RequestResponse) : this(
        targetAmsNetId = a.targetAmsNetId,
        targetAmsPort = a.targetAmsPort,
        sourceAmsNetId = a.sourceAmsNetId,
        sourceAmsPort = a.sourceAmsPort,
        commandId = a.commandId,
        data = a.data,
        stateFlags = a.stateFlags,
        result = a.result,
        invokeId = a.invokeId
    )

    private fun netIdAndPortBytes(netID: String, amsPort: Int): ByteArray {
        val buffer = ByteArray(8)
        val a = netID.split(".")
        if (a.size != AMS_TCP_HEADER_LEN) throw Exception("invalid AmsNetId, must contain 6 elements")
        return try {
            a.forEachIndexed { index, entry ->
                buffer[index] = Integer.decode(entry).toByte()
            }
            amsPort.toShort().bytes.forEachIndexed { index, b ->
                buffer[index + AMS_TCP_HEADER_LEN] = b
            }
            buffer
        } catch (e: Exception) {
            throw AdsException("Invalid AmsNetId")
        }
    }

    fun buildRequest(invokeId: Int): ByteArray {

        val dataLen = data?.size ?: 0

        val request = ByteArray(AMS_TCP_HEADER_LEN + AMS_HEADER_LEN + dataLen)
        val adsTcpHeader = byteArrayOf(0, 0) + (AMS_HEADER_LEN + dataLen).bytes
        adsTcpHeader.copyInto(request, 0)

        val adsHeader = headerBytes(dataLen, invokeId)
        adsHeader.copyInto(request, AMS_TCP_HEADER_LEN, 0, AMS_HEADER_LEN)

        if (dataLen > 0) {
            data?.copyInto(request, AMS_TCP_HEADER_LEN + AMS_HEADER_LEN, 0, dataLen)
        }
        return request
    }


    private fun headerBytes(dataLen: Int, invokeId: Int): ByteArray {
        val AmsHeader = ByteArrayOutputStream(AMS_HEADER_LEN)
        AmsHeader.write(netIdAndPortBytes(targetAmsNetId, targetAmsPort))
        AmsHeader.write(netIdAndPortBytes(sourceAmsNetId, sourceAmsPort))
        AmsHeader.write(commandId.ordinal.toShort().bytes)
        AmsHeader.write(stateFlags.bytes)
        AmsHeader.write(dataLen.bytes)
        AmsHeader.write(result.bytes)
        AmsHeader.write(invokeId.bytes)
        return AmsHeader.toByteArray()
    }

    fun checkResponse(
        message: String,
        expectedInvokeId: Int? = null,
        expectedCommandId: CommandId? = null,
        expectedDataSize: Int? = null,
        expectedStateFlags: Short? = null
    ) {
        if (result != 0) throw AdsException(message, result)

        if (expectedCommandId != null && expectedCommandId != commandId)
            throw AdsException("$message, received ADS command ID $commandId but expected $expectedCommandId")

        if (expectedInvokeId != null && expectedInvokeId != invokeId) throw AdsException("$message, received ADS invoke ID $invokeId but expected $expectedInvokeId")

        val dataSize = data?.size ?: 0
        if (expectedDataSize != null && expectedDataSize > dataSize) throw AdsException("$message, received $dataSize data bytes but expected $expectedDataSize")

        if (expectedStateFlags != null && expectedStateFlags != stateFlags) throw AdsException("$message, received ADS state flags 0x${stateFlags.asHexString()} data bytes but expected 0x${expectedStateFlags.asHexString()}")
    }

    companion object {

        const val AMS_TCP_HEADER_LEN = 6
        const val AMS_HEADER_LEN = 32
        private const val AMS_COMMAND_STATUS = 4.toShort()
        private const val AMS_TARGET_OFFSET = 0
        private const val AMS_SOURCE_OFFSET = 8
        private const val AMS_COMMAND_ID_OFFSET = 16
        private const val AMS_STATE_FLAGS_OFFSET = 18
        private const val AMS_LENGTH_OFFSET = 20
        private const val AMS_RESULT_OFFSET = 24
        private const val AMS_INVOKE_ID_OFFSET = 28


        private const val AMS_DATA_OFFSET = AMS_HEADER_LEN


        private fun netIdAndPortBytesFromBytes(bytes: ByteArray, offset: Int): Pair<String, Int> {
            val netId = bytes.sliceArray(offset..<offset + 6).map {
                if (it < 0) it + 256 else it
            }.joinToString(separator = ".") { it.toString() }

            val port = bytes.toInt16(offset + 6)

            return netId to port.toInt()
        }

        fun decode(adsHeaderBytes: ByteArray): RequestResponse {

            if (adsHeaderBytes.size < AMS_HEADER_LEN) throw Exception("ADS response too short, expected ${AMS_TCP_HEADER_LEN + AMS_HEADER_LEN} or more bytes, got ${adsHeaderBytes.size}")

            val (sourceNetId, sourcePort) = netIdAndPortBytesFromBytes(adsHeaderBytes, AMS_TARGET_OFFSET)
            val (targetNetId, targetPort) = netIdAndPortBytesFromBytes(adsHeaderBytes, AMS_SOURCE_OFFSET)
            val dataLen = adsHeaderBytes.toInt32(AMS_LENGTH_OFFSET)

            val commandId = try {
                CommandId.entries[adsHeaderBytes.toInt16(AMS_COMMAND_ID_OFFSET).toInt()]
            } catch (_: Exception) {
                throw AdsException("${adsHeaderBytes.toInt16(AMS_COMMAND_ID_OFFSET)} is not a valid ADS command ID")
            }

            return RequestResponse(
                targetAmsNetId = targetNetId,
                targetAmsPort = targetPort,
                sourceAmsNetId = sourceNetId,
                sourceAmsPort = sourcePort,
                commandId = commandId,
                stateFlags = adsHeaderBytes.toInt16(AMS_STATE_FLAGS_OFFSET),
                result = adsHeaderBytes.toInt32(AMS_RESULT_OFFSET),
                invokeId = adsHeaderBytes.toInt32(AMS_INVOKE_ID_OFFSET),
                data = adsHeaderBytes.sliceArray(AMS_DATA_OFFSET..<AMS_DATA_OFFSET + dataLen)
            )
        }
    }
}