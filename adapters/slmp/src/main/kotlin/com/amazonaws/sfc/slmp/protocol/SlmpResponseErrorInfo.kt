// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.slmp.protocol

import com.amazonaws.sfc.slmp.protocol.SlmpDecoder.readInt16


class SlmpResponseErrorInfo(
    val networkNumber: Byte,
    val stationNumber: Byte,
    val moduleNumber: Short,
    val multiDropStationNumber: Byte,
    val command: Short,
    val subCommand: Short
) {

    override fun toString(): String {
        return "networkNumber:$networkNumber, stationNumber:$stationNumber, moduleNumber:$moduleNumber, multiDropStationNumber:$multiDropStationNumber, command:$command, subCommand:$subCommand"
    }

    companion object {
        fun parseFromBytes(bytes: ByteArray): SlmpResponseErrorInfo? {
            if (bytes.size < 8) return null
            return SlmpResponseErrorInfo(
                networkNumber = bytes[0],
                stationNumber = bytes[1],
                moduleNumber = bytes.readInt16(2),
                multiDropStationNumber = bytes[4],
                command = bytes.readInt16(5),
                subCommand = bytes.readInt16(7)
            )
        }
    }
}