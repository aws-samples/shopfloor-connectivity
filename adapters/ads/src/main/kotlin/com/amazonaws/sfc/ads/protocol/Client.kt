/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.ads.protocol

import com.amazonaws.sfc.ads.protocol.Decoder.toInt32
import com.amazonaws.sfc.ads.protocol.RequestResponse.Companion.AMS_TCP_HEADER_LEN
import com.amazonaws.sfc.log.Logger
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

class Client(
    private val tcpClient: LockableTcpClient,
    private val config: ClientConfig,
    private val logger: Logger,
) {

    private val className = this::class.java.simpleName

    private val requestBuilder = RequestBuilder(config)

    private var _invokeId: Int = 0
    private fun getNextInvokeId(): Int {
        if (_invokeId == Int.MAX_VALUE) _invokeId = 0
        _invokeId += 1
        return _invokeId
    }

    suspend fun getSymbols(): List<Symbol>? {

        val log = logger.getCtxLoggers(className, "getSymbols")

        log.trace("Getting symbols from PLC")

        // get length of symbols data to read
        val getSymbolLengthRequest = requestBuilder.getSymbolsLengthRequest()
        val symbolLengthResponse = executeCommand(getSymbolLengthRequest) as GetSymbolsLengthResponse? ?: return null

        log.trace("Length of symbol data is ${symbolLengthResponse.symbolLength} bytes")

        // get symbol data
        val getSymbolsRequest = requestBuilder.getSymbolsRequest(symbolLengthResponse.symbolLength)

        return (executeCommand(getSymbolsRequest) as GetSymbolsResponse?)?.symbols
    }


    suspend fun readValues(symbols: List<Symbol>): List<ReadResult> {
        if (symbols.isEmpty()) return emptyList()
        val request = requestBuilder.multiReadRequest(symbols)
        return readValues(request)
    }


    suspend fun readValues(request: MultiReadRequest): List<ReadResult> {

        val log = logger.getCtxLoggers(className, "readValues")

        log.trace("Reading values for symbols ${request.symbols.joinToString { it.symbolName }}")

        val response = executeCommand(request) as MultiReadResponse? ?: return emptyList()
        if (response.readResult != 0) throw AdsException(
            "Error reading data from symbol handles ${request.symbols.joinToString { it.symbolName }}",
            response.readResult
        )
        return response.readResults
    }


    private suspend fun executeCommand(request: RequestResponse): RequestResponse? {

        val invokeId = getNextInvokeId()

        if (!tcpClient.acquire(config.commandTimeout)) {
            throw AdsException("Timeout acquiring access to TCP client")
        }


        return try {
            withTimeout(config.commandTimeout) {
                sendRequest(request, invokeId)
                readResponse(request, invokeId)
            }
        } catch (t: TimeoutCancellationException) {
            throw AdsException("Timeout executing command")
        } catch (e: Exception) {
            drain()
            if (e is NegativeArraySizeException) null else throw e

        } finally {
            tcpClient.release()
        }

    }

    private suspend fun drain() {
        withTimeoutOrNull(config.commandTimeout) {
            tcpClient.read()
        }
    }

    private suspend fun Client.readResponse(request: RequestResponse, invokeId: Int): RequestResponse {

        val log = logger.getCtxLoggers(className, "readResponse")


        val responseBytes = readResponseBytes()
        val resp = RequestResponse.decode(responseBytes)

        log.trace("Received ${resp::class.java.simpleName} response from device")
        val response = when (request) {
            is GetSymbolsLengthRequest -> GetSymbolsLengthResponse(resp, invokeId)
            is GetSymbolsRequest -> GetSymbolsResponse(resp, request.length, invokeId)
            is MultiReadRequest -> MultiReadResponse(resp, request.symbols, invokeId)
            else -> resp
        }
        return response
    }

    private suspend fun sendRequest(request: RequestResponse, invokeId: Int) {

        val log = logger.getCtxLoggers(className, "sendRequest")
        log.trace("Sending ${request::class.java.simpleName} request with invokeID $invokeId to device")

        val buffer = request.buildRequest(invokeId)
        tcpClient.write(buffer)
    }


    private suspend fun readResponseBytes(): ByteArray {

        try {
            val header = ByteArray(AMS_TCP_HEADER_LEN)
            return withTimeout(config.commandTimeout) {

                repeat(AMS_TCP_HEADER_LEN) { index ->
                    val b = tcpClient.read()
                    header[index] = b
                }

                val len = header.toInt32(2)
                val buffer = ByteArray(len)
                val byteReadTimeout = config.readTimeout
                repeat(len) { index ->
                    // short timeout for reading single bytes
                    withTimeout(byteReadTimeout) {
                        val b = tcpClient.read()
                        buffer[index] = b
                    }
                }

                buffer

            }

        } catch (_: TimeoutCancellationException) {
            throw AdsException("Timeout reading response from target ${config.targetAmsNetId} port ${config.targetAmsPort}")
        }
    }

}