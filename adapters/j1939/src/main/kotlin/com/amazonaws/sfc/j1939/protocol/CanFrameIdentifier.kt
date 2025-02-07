// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.j1939.protocol

import com.amazonaws.sfc.util.asHexString

class CanFrameIdentifier(val canId: UInt) {

    val pgn = extractPgn(canId)
    val sourceAddress: UByte = (SA_MASK and canId.toInt()).toUByte()
    private val pf: Int = PF_MASK and canId.toInt() shr 16
    var destinationAddress: UByte = (DA_MASK and canId.toInt() shr 8).toUByte()

    override fun toString(): String {
        val canIdStr = "$canId(0x${canId.asHexString()})"
        val pgnStr = "$pgn(0x${pgn.asHexString().trimStart('0')})"
        val pduFormatStr = "$pf(0x${pf.asHexString().trimStart('0')})"
        val sourceAddressStr = "$sourceAddress(0x${sourceAddress.asHexString()})"
        val destinationAddressStr = "$destinationAddress(0x${destinationAddress.asHexString()})"
        return "CanID=$canIdStr, PGN=$pgnStr, PDU Format=$pduFormatStr, Source Address=$sourceAddressStr, Destination Address=$destinationAddressStr"
    }

    companion object {
        private const val SA_MASK = 0x000000FF
        private const val PF_MASK = 0xFF0000
        private const val DA_MASK = 0x0000FF00

        fun extractPgn(canId: UInt): UInt {
            val pf: Int = PF_MASK and canId.toInt() shr 16
            var destinationAddress: UByte = (DA_MASK and canId.toInt() shr 8).toUByte()
            var pgn: UInt = (pf * 256).toUInt()
            if (pf >= 240) {
                pgn += destinationAddress.toUInt()
            }
            return pgn
        }
    }

}