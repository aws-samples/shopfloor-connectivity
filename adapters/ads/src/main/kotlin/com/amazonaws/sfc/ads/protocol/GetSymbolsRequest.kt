/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.ads.protocol

import com.amazonaws.sfc.ads.protocol.Decoder.bytes
import java.io.ByteArrayOutputStream

open class GetSymbolsRequest(
    targetAmsNetId: String,
    targetAmsPort: Int,
    sourceAmsNetId: String,
    sourceAmsPort: Int,
    val length : Int
) : RequestResponse(
    targetAmsNetId = targetAmsNetId,
    targetAmsPort = targetAmsPort,
    sourceAmsNetId = sourceAmsNetId,
    sourceAmsPort = sourceAmsPort,
    commandId = CommandId.READ,
    buildReadData(length)
) {

    companion object {
        fun buildReadData(length : Int): ByteArray {
            val data = ByteArrayOutputStream(12)
            // Index group

            data.write(0xF00B.bytes)
            // Index offset
            data.write(0.bytes)
            // write length of data to read
            data.write(length.bytes)
            return data.toByteArray()
        }

    }
}


