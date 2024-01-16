/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.ads.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object Decoder {

    val ByteArray.toInt32: Int
        get() = if (this.size < 4) throw IllegalArgumentException("ByteArray must contain 4 bytes") else
            (this[3].toInt() and 0xFF shl 24) or (this[2].toInt() and 0xFF shl 16) or (this[1].toInt() and 0xFF shl 8) or (this[0].toInt() and 0xFF)


    val ByteArray.toInt64: Long
        get() = if (this.size < 8) throw IllegalArgumentException("ByteArray must contain 8 bytes")
        else
            (this[7].toLong() and 0xFF shl 56) or
                    (this[6].toLong() and 0xFF shl 48) or
                    (this[5].toLong() and 0xFF shl 40) or
                    (this[4].toLong() and 0xFF shl 32) or
                    (this[3].toLong() and 0xFF shl 24) or
                    (this[2].toLong() and 0xFF shl 16) or
                    (this[1].toLong() and 0xFF shl 8) or
                    (this[0].toLong() and 0xFF)

    fun ByteArray.toInt64(offset: Int) =
        this.sliceArray(offset..offset + 7).toInt64


    fun ByteArray.toInt32(offset: Int): Int {
        return this.sliceArray(offset..offset + 3).toInt32
    }

    val ByteArray.toInt16: Short
        get() = if (this.size < 2) throw IllegalArgumentException("ByteArray must contain 2 bytes") else
            ((this[1].toInt() and 0xFF shl 8) or (this[0].toInt() and 0xFF)).toShort()


    fun ByteArray.toInt16(offset: Int): Short {
        return this.sliceArray(offset..offset + 1).toInt16
    }

    // Encodes an INT32 as Little Endian bytes
    val Int.bytes: ByteArray
        get() = byteArrayOf(
            (this and 0xFF).toByte(),
            (this shr 8 and 0xFF).toByte(),
            (this shr 16 and 0xFF).toByte(),
            (this shr 24 and 0xFF).toByte()
        )

    val Short.bytes: ByteArray
        get() = byteArrayOf(
            (this.toInt() and 0xFF).toByte(),
            (this.toInt() shr 8 and 0xFF).toByte(),
        )

    fun decodeWString(symbol: Symbol, valueBytes: ByteArray): String {
        var i = 0
        while (i < symbol.size && valueBytes[i] != 0.toByte()) i += 2
        return String(valueBytes.sliceArray(0..<i), charset("UTF-16LE"))
    }

    fun decodeULong(valueBytes: ByteArray) = valueBytes.toInt64.toULong()

    fun decodeUShort(valueBytes: ByteArray) = valueBytes.toInt16.toUShort()

    fun decodeString(valueBytes: ByteArray) = String(valueBytes.sliceArray(0..<valueBytes.indexOf(0)))

    fun decodeByte(valueBytes: ByteArray) = valueBytes[0]

    fun decodeFloat(valueBytes: ByteArray?): Float {
        val buffer = ByteBuffer.wrap(valueBytes)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        return buffer.getFloat()
    }

    fun decodeLTime(valueBytes: ByteArray): String {
        val nanoSeconds = valueBytes.toInt64
        val dt = nanoSeconds.toDuration(DurationUnit.NANOSECONDS)
        return dt.toIsoString()
    }

    fun decodeTime(valueBytes: ByteArray): String {
        val milliseconds = valueBytes.toInt32
        val dt = milliseconds.toDuration(DurationUnit.MILLISECONDS)
        return dt.toIsoString()
    }

    fun decodeDouble(valueBytes: ByteArray?): Double {
        val buffer = ByteBuffer.wrap(valueBytes)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        return buffer.getDouble()
    }

    fun decodeShort(valueBytes: ByteArray) = valueBytes.toInt16

    fun decodeUInt32(valueBytes: ByteArray) = valueBytes.toInt32.toUInt()

    fun decodeTimeOfDay(valueBytes: ByteArray): String {
        val seconds = valueBytes.toInt32
        return seconds.toDuration(DurationUnit.SECONDS).toIsoString()
    }

    fun decodeDateTime(valueBytes: ByteArray): String {
        val seconds = valueBytes.toInt32
        return Instant.ofEpochSecond(seconds.toLong()).toString()
    }

    fun decodeLDateTime(valueBytes: ByteArray): String {
        val nanoSeconds = valueBytes.toInt64.toULong()

        return Instant.ofEpochSecond(
            (nanoSeconds / (1.0E9.toULong())).toLong(),
            (nanoSeconds % (1.0E9.toULong())).toInt().toLong()
        ).toString()
    }

    fun decodeDate(valueBytes: ByteArray): String {
        val seconds = valueBytes.toInt32
        return Instant.ofEpochSecond(seconds.toLong()).toString().split("T")[0]
    }

    fun decodeInt32(valueBytes: ByteArray) = valueBytes.toInt32

    fun decodeUByte(valueBytes: ByteArray) = valueBytes.first().toInt().toUByte()

    fun decodeBoolean(valueBytes: ByteArray) =
        valueBytes.first().toInt() != 0

    fun decodeAppInfo(valueBytes: ByteArray) = PlcAppSystemInfo.fromBytes(valueBytes)

    fun decodeTaskSystemInfo(valueBytes: ByteArray) = PlcTaskSystemInfo.fromBytes(valueBytes)

    fun decodeLibVersion(valueBytes: ByteArray): Any = LibVersion.fromBytes(valueBytes)

    fun decodeVersion(valueBytes: ByteArray): Any = Version.fromBytes(valueBytes)
}