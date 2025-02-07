// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//
// Partial port to Kotlin port of https://github.com/jazdw/jnaCan
//

package com.amazonaws.sfc.canbus


import com.amazonaws.sfc.canbus.jna.TimeValue
import com.sun.jna.Native
import com.sun.jna.NativeLong
import java.io.UnsupportedEncodingException
import java.time.Instant

object Utils {

    fun millisToTimeValue(millis: Long): TimeValue {
        return TimeValue().apply {
            val microseconds = millis * 1000
            tv_sec = NativeLong(microseconds / 1000000)  // Convert to seconds
            tv_usec = NativeLong(microseconds % 1000000) // Remaining microseconds
        }
    }

    fun timeValueToInstant(time: TimeValue): Instant {
        return Instant.ofEpochMilli(microsToMillis(time.tv_sec.toLong(), time.tv_usec.toLong()))
    }

    val TimeValue.toInstant: Instant
        get() {
            return timeValueToInstant(this)
        }

    fun microsToMillis(sec: Long, usec: Long): Long {
        return sec * 1000 + usec / 1000
    }

    fun stringToFixedLengthByteArray(s: String, length: Int): ByteArray {
        var strBytes: ByteArray =
            try {
                s.toByteArray(charset(Native.getDefaultStringEncoding()))
            } catch (e: UnsupportedEncodingException) {
                System.err.println("Native String encoding not supported, falling back to Java default encoding")
                s.toByteArray()
            }
        if (strBytes.size > length) throw CanException("String too long to fit in byte array of length $length")
        val ret = ByteArray(length)
        System.arraycopy(strBytes, 0, ret, 0, strBytes.size)
        return ret
    }

    interface ValueEnum<V> {
        fun value(): V?
    }

    class ReverseEnumMap<V, E>(val valueType: Class<E>) where E : Enum<E>, E : ValueEnum<V> {
        private val map: MutableMap<V?, E?> = HashMap<V?, E?>()

        init {
            for (e in valueType.getEnumConstants()) {
                map.put(e.value(), e)
            }
        }

        fun get(v: V): E? {
            val enumObject = map[v]
            requireNotNull(enumObject) { "Cant convert " + v.toString() + " to " + valueType.getSimpleName() }
            return enumObject
        }

        companion object {
            fun <V, E> create(clazz: Class<E>): ReverseEnumMap<V, E> where E : Enum<E>, E : ValueEnum<V> {
                return ReverseEnumMap<V, E>(clazz)
            }
        }
    }
}