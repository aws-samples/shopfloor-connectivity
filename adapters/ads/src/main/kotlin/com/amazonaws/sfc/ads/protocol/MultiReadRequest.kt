/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */


package com.amazonaws.sfc.ads.protocol

import com.amazonaws.sfc.ads.protocol.Decoder.bytes
import java.io.ByteArrayOutputStream

open class MultiReadRequest(
    targetAmsNetId: String,
    targetAmsPort: Int,
    sourceAmsNetId: String,
    sourceAmsPort: Int,
    var symbols: List<Symbol>,
) : RequestResponse(
    targetAmsNetId = targetAmsNetId,
    targetAmsPort = targetAmsPort,
    sourceAmsNetId = sourceAmsNetId,
    sourceAmsPort = sourceAmsPort,
    commandId = CommandId.READ_WRITE,
    buildMultiReadData(symbols)
) {

    companion object {
        fun buildMultiReadData(handles: List<Symbol>): ByteArray {

            val data = ByteArrayOutputStream(12 * handles.size)

            handles.forEach { handle ->
                // index group for handle
                data.write( handle.indexGroup.bytes)

                // index offset for handle
                data.write(handle.indexOffset.bytes)

                // data length for handle
                data.write(handle.size.bytes)
            }


            val cmd = ByteArrayOutputStream(16 + data.size())
            // Index group
            cmd.write(SUM_UP_READ.bytes)
            // Index offset
            cmd.write((handles.size.bytes))
            // Total size to read
            val totalReadLen = handles.map { 4 + it.size  }.sum()
            cmd.write(totalReadLen.bytes)

            // write size
            val dataBytes = data.toByteArray()
            cmd.write(dataBytes.size.bytes)

            cmd.write(dataBytes)

            return cmd.toByteArray()
        }

        private const val SUM_UP_READ = 0xF080

    }
}