/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */


package com.amazonaws.sfc.ads.protocol

import com.amazonaws.sfc.ads.protocol.Decoder.toInt32
import com.amazonaws.sfc.ads.protocol.Decoder.toInt64
import com.amazonaws.sfc.ads.protocol.GetSymbolsResponse.Companion.RESPONSE_EXPECTED_STATE_FLAGS
import java.io.ByteArrayInputStream

open class MultiReadResponse(
    private val source: RequestResponse,
    private val symbols: List<Symbol>,
    expectedInvokeId: Int?) : RequestResponse(source) {

    init {
        // each value has additional 4 bytes for the result
        val expectedDataSize = symbols.sumOf { 4 + it.size }
        checkResponse(
            message = "Reading symbol values",
            expectedInvokeId = expectedInvokeId,
            expectedCommandId = CommandId.READ_WRITE,
            expectedDataSize = expectedDataSize,
            expectedStateFlags = RESPONSE_EXPECTED_STATE_FLAGS
        )
    }

    val readResult: Int by lazy {
        source.data?.toInt32 ?: 0
    }

    val readResults: List<ReadResult>
        get() : List<ReadResult> {
            return if (source.data == null || source.data.isEmpty()) emptyList<ReadResult>() else {
                val resultDataLength = symbols.size * 4
                val startOfValuesData = 8 + resultDataLength
                val results = source.data.slice(8..<startOfValuesData).chunked(4)
                val valuesData = ByteArrayInputStream(source.data.sliceArray(startOfValuesData..<source.data.size))
                var index = -1
                results.map { r ->
                    index += 1
                    val result = r.toByteArray().toInt32
                    val symbol = symbols[index]
                    val valueBytes = ByteArray(symbol.size)
                    valuesData.read(valueBytes)
                    if (result == 0) {
                        val symbolValue = decodeSymbolValue(symbol, valueBytes)
                        ReadResult(value = symbolValue, symbol = symbol)
                    } else {
                        ReadResult(result = result, symbol = symbol)
                    }
                }
            }
        }

    private fun decodeSymbolValue(symbol: Symbol, valueBytes: ByteArray?, arrayDimensions: List<Int>? = null): Any? {

        if (valueBytes == null || valueBytes.isEmpty()) return null

        val arrayDimensionSizes = arrayDimensions ?: (symbol.arrayDimensions?.reversed())

        return if (!arrayDimensionSizes.isNullOrEmpty()) {

            val arrayDimensionSize = arrayDimensionSizes.last()
            val listData = valueBytes.toList().chunked(valueBytes.size / arrayDimensionSize)
            listData.map {
                if (arrayDimensionSizes.size > 1) {
                    // Process lists for next array dimension
                    decodeSymbolValue(
                        symbol,
                        it.toByteArray(),
                        arrayDimensionSizes.subList(0, arrayDimensionSizes.size - 1)
                    )
                } else {
                    // List items for array dimension
                    decodeSymbolValue(symbol, it.toByteArray(), emptyList())
                }
            }
        } else {
            decodeValue(symbol, valueBytes)
        }
    }

    private fun decodeValue(symbol: Symbol, valueBytes: ByteArray) = when (symbol.dataType) {
        DataType.BOOL -> Decoder.decodeBoolean(valueBytes)
        DataType.BYTE, DataType.USINT -> Decoder.decodeUByte(valueBytes)
        DataType.DATE -> Decoder.decodeDate(valueBytes)
        DataType.DATE_AND_TIME -> Decoder.decodeDateTime(valueBytes)
        DataType.TIME_OF_DAY -> Decoder.decodeTimeOfDay(valueBytes)
        DataType.DINT -> Decoder.decodeInt32(valueBytes)
        DataType.DWORD, DataType.UDINT, DataType.OTCID -> Decoder.decodeUInt32(valueBytes)
        DataType.INT -> Decoder.decodeShort(valueBytes)
        DataType.LINT -> valueBytes.toInt64
        DataType.LREAL -> Decoder.decodeDouble(valueBytes)
        DataType.TIME -> Decoder.decodeTime(valueBytes)
        DataType.LTIME -> Decoder.decodeLTime(valueBytes)
        DataType.REAL -> Decoder.decodeFloat(valueBytes)
        DataType.SINT -> Decoder.decodeByte(valueBytes)
        DataType.STRING -> Decoder.decodeString(valueBytes)
        DataType.WORD, DataType.UINT -> Decoder.decodeUShort(valueBytes)
        DataType.ULINT -> Decoder.decodeULong(valueBytes)
        DataType.WSTRING -> Decoder.decodeWString(symbol, valueBytes)
        DataType.APP_INFO -> Decoder.decodeAppInfo(valueBytes)
        DataType.TASK_SYSTEM_INFO -> Decoder.decodeTaskSystemInfo(valueBytes)
        DataType.LIB_VERSION -> Decoder.decodeLibVersion(valueBytes)
        DataType.VERSION -> Decoder.decodeVersion(valueBytes)
        else -> valueBytes

    }
}


