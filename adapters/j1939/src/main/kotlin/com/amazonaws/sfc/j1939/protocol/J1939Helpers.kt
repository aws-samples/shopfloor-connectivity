// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.j1939.protocol

fun getUByte(s: String?): UByte? {
    if (s.isNullOrEmpty()) {
        return null
    }

    val u = (if (s.lowercase().startsWith("0x") && s.length > 2)
        s.substring(2, s.length ).toLongOrNull(16)?.toUByte()
    else
        s.toUByteOrNull(10))
            ?: throw Exception("Invalid Unsigned Byte: $s")

    return u
}

fun getUInt(s: String?): UInt? {
    if (s.isNullOrEmpty()) {
        return null
    }

    val u = (if (s.lowercase().startsWith("0x") && s.length > 2)
        s.substring(2,s.length).toLongOrNull(16)?.toUInt()
    else
        s.toUIntOrNull(10))
            ?: throw Exception("Invalid Unsigned Integer: $s")

    return u
}

fun isNumeric(s: String?): Boolean {
    if (s.isNullOrEmpty()) {
        return false
    }
    if (s.toCharArray().all { it.isDigit() || it == '.'}) return true
    val lowerCase = s.lowercase()
    return lowerCase.startsWith("0x") && s.length > 2 && lowerCase.substring(2, s.length).toCharArray().all { it.isDigit() || it in "abcdef" }
}