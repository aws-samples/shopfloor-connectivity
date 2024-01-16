/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.ads.protocol

import com.amazonaws.sfc.ads.protocol.Decoder.bytes
import java.io.ByteArrayOutputStream

open class GetSymbolsLengthRequest(
    targetAmsNetId: String,
    targetAmsPort: Int,
    sourceAmsNetId: String,
    sourceAmsPort: Int
) : RequestResponse(
    targetAmsNetId = targetAmsNetId,
    targetAmsPort = targetAmsPort,
    sourceAmsNetId = sourceAmsNetId,
    sourceAmsPort = sourceAmsPort,
    commandId = CommandId.READ,
    buildReadData()
) {

    companion object {
        fun buildReadData(): ByteArray {
            val data = ByteArrayOutputStream(12)
            // Index group
            data.write(0xF00F.bytes)
            // Index offset
            data.write(0.bytes)
            // write length of data to read
            data.write(0x30.bytes)
            return data.toByteArray()
        }

    }
}


