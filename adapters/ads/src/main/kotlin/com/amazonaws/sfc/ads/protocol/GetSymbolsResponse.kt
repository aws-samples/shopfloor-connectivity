/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.ads.protocol

import com.amazonaws.sfc.ads.protocol.Decoder.toInt16
import com.amazonaws.sfc.ads.protocol.Decoder.toInt32
import java.util.regex.Pattern
import kotlin.math.abs

class GetSymbolsResponse(source: RequestResponse, length: Int, expectedInvokeId: Int? = null) :
    ReadResponse(source, expectedInvokeId = expectedInvokeId) {

    init {
        checkResponse("Reading symbols", expectedInvokeId, CommandId.READ, expectedDataSize = length + 8, RESPONSE_EXPECTED_STATE_FLAGS)
    }

    val symbols by lazy {

        sequence {

            var offset: Int
            val symbolData = source.data!!.sliceArray(8..<source.data.size)
            var symbolOffset = 0

            while (symbolOffset < symbolData.size) {

                val symbolBuilder = Symbol.builder()

                offset = symbolOffset
                symbolOffset += symbolData.toInt32(offset)

                symbolBuilder.indexGroup = symbolData.toInt32(offset + 4)
                symbolBuilder.indexOffset = symbolData.toInt32(offset + 8)
                symbolBuilder.size = symbolData.toInt32(offset + 12)

                val nameLength = symbolData.toInt16(offset + 24) + 1
                val typeLength = symbolData.toInt16(offset + 26) + 1

                offset += 30

                // name of the symbol
                val nameBuffer = symbolData.sliceArray(offset..<offset + nameLength - 1)
                symbolBuilder.symbolName = String(nameBuffer)
                offset += nameLength

                // type from the buffer as string
                val typeBuffer = symbolData.sliceArray(offset..<offset + typeLength - 1)
                val typeName = String(typeBuffer)
                // map type string to enum of types that can be decoded
                symbolBuilder.dataType = DataType.fromString(typeName)
                symbolBuilder.typeName = String(typeBuffer)
                // get dimensions if type is an array, up to 3 dimensions
                symbolBuilder.arrayDimensions = getArrayDimensions(typeName)
                offset += typeLength


                yield(symbolBuilder.build())


            }

        }.toList()
    }

    private fun getArrayDimensions(typeName: String) =
        if (typeName.startsWith("ARRAY")) {
            val matcher = TYPE_ARRAY_PATTERN.matcher(typeName)
            sequence {
                if (matcher.find()) {

                    // get uo to 3 dimensions from type name
                    listOf("x", "y", "z").forEach {

                        if (matcher.group(it) != null) {
                            val first: Int = try {
                                matcher.group(it + "1").toInt()
                            } catch (_: Exception) {
                                0
                            }
                            val last = try {
                                matcher.group(it + "2").toInt()
                            } catch (_: Exception) {
                                0
                            }
                            val len = abs(last - first) + 1
                            this.yield(len)
                        }
                    }
                }
            }.toList()
        } else null // null means no  array dimensions

    override fun toString(): String {
        return "AdsGetSymbolsResponse(symbols=$symbols)"
    }

    companion object {
        val TYPE_ARRAY_PATTERN: Pattern =
            Pattern.compile("""\[(?<x>(?<x1>\d+)\.\.(?<x2>\d+))(,?(?<y>(?<y1>\d+)\.\.(?<y2>\d+))?)(,?(?<z>(?<z1>\d+)\.\.(?<z2>\d+))?)]""")

        const val RESPONSE_EXPECTED_STATE_FLAGS = 0x05.toShort()

    }
}
