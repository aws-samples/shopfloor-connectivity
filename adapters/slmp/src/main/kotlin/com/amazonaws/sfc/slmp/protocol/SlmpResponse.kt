// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.slmp.protocol

import com.amazonaws.sfc.slmp.config.SlmpControllerConfiguration
import com.amazonaws.sfc.slmp.protocol.SlmpDecoder.readInt16
import com.amazonaws.sfc.slmp.protocol.SlmpError.Companion.NO_ERROR
import com.amazonaws.sfc.tcp.TcpClient
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

open class SlmpResponse(
    val slmpHeader: SlmpHeader,
    val endCode: Short = NO_ERROR,
    val responseData: ByteArray,
    val errorInfo: SlmpResponseErrorInfo? = null
) {


    constructor(slmpHeader: SlmpHeader, endCode: Short, errorInfo: SlmpResponseErrorInfo?) : this(
        slmpHeader = slmpHeader,
        endCode = endCode,
        responseData = ByteArray(0),
        errorInfo = errorInfo
    )

    companion object {

        suspend fun read(client: TcpClient, config: SlmpControllerConfiguration): SlmpResponse {


            return try {
                withTimeout(config.readTimeout) {
                    val (header, subHeader) = readHeader(client, config)

                    val dataSize = subHeader.readInt16(subHeader.lastIndex - 1)
                    val (endCode, responseData) = readResponseData(dataSize.toInt(), client)

                    val slmpHeader = SlmpHeader(
                        header = header,
                        networkNumber = subHeader[0],
                        stationNumber = subHeader[1],
                        moduleNumber = subHeader.readInt16(2),
                        multiDropStationNumber = subHeader[4],
                        monitoringTimer = 0,
                        dataLength = dataSize
                    )

                    if (endCode == NO_ERROR)
                        SlmpResponse(slmpHeader, responseData = responseData)
                    else
                        SlmpResponse(slmpHeader, endCode, SlmpResponseErrorInfo.parseFromBytes(responseData))
                }
            } catch (_: TimeoutCancellationException) {
                throw SlmpException("Timeout reading response from ${config.address} after ${config.readTimeout}, increase read ${SlmpControllerConfiguration.CONFIG_READ_TIMEOUT} or check for other clients using SLMP to access ${config.address}:${config.port}")
            }
        }

        private suspend fun readHeader(
            client: TcpClient,
            config: SlmpControllerConfiguration
        ): Pair<ByteArray, ByteArray> {


            val header = receiveHeaderBytes(client, config)
                ?: throw SlmpException("Timeout reading response header from ${config.address} after ${config.readTimeout}")

            val subheaderSize = when {
                header.contentEquals(SlmpHeader.COMMAND_RESPONSE_NO_SERIAL) -> 7
                header.contentEquals(SlmpHeader.COMMAND_RESPONSE_WITH_SERIAL) -> 12
                else -> 0
            }
            val subHeader = ByteArray(subheaderSize)

            repeat(subheaderSize) { i ->
                subHeader[i] = client.read()
            }

            return Pair(header, subHeader)
        }

        private suspend fun receiveHeaderBytes(
            client: TcpClient,
            config: SlmpControllerConfiguration
        ): ByteArray? {
            val buffer = ByteArray(2)
            buffer[1] = client.read()

            var validHeaderBytesReceived = false
            withTimeoutOrNull(config.readTimeout) {
                while (!validHeaderBytesReceived) {
                    buffer[0] = buffer[1]
                    buffer[1] = client.read()
                    validHeaderBytesReceived = buffer.contentEquals(SlmpHeader.COMMAND_RESPONSE_NO_SERIAL) ||
                            buffer.contentEquals(SlmpHeader.COMMAND_RESPONSE_WITH_SERIAL)
                }
            } ?: return null
            return buffer
        }


        private suspend fun readResponseData(
            dataSize: Int,
            receiveChannel: TcpClient
        ): Pair<Short, ByteArray> {

            val buffer = ByteArray(dataSize)
            repeat(dataSize) { i ->
                buffer[i] = receiveChannel.read()
            }
            val endCode = buffer.readInt16(0)
            val responseData = buffer.sliceArray(2..buffer.lastIndex)
            return endCode to responseData

        }

    }
}