// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.j1939.protocol

class J1939Decoder {

    companion object {
        fun hasFractional(value: Double) = (value % 1.0 != 0.0)

        fun decode(signal: J1939Signal, payload: ByteArray, rangeCheck: Boolean = true): Any? {

            val rawValue = extractRawValue(signal, payload)
            if (rawValue == null) return null

            val value = (rawValue.toDouble() * signal.factor) + signal.offset
            if (rangeCheck && (value < signal.minimum || value > signal.maximum)) {
                return null
            }

            if ((!hasFractional(signal.factor)) && (!hasFractional(signal.offset))) {
                if (signal.length == 1) return value.toInt() == 1
                if (signal.length in 2..8) return if (signal.valueType == ValueType.SIGNED) value.toInt().toByte() else value.toUInt().toUByte()
                if (signal.length in 9..16) return if (signal.valueType == ValueType.SIGNED) value.toInt().toShort() else value.toUInt().toUShort()
                if (signal.length in 17..32) return if (signal.valueType == ValueType.SIGNED) value.toInt() else value.toUInt()
                return if (signal.valueType == ValueType.SIGNED) value.toLong() else value.toULong()
            }

            return value.toFloat()
        }


        private fun extractRawValue(signal: J1939Signal, payload: ByteArray): ULong? {

            // For J1939, both bytes and bits are indexed in reverse
            var byteIndex = (payload.size) - (signal.startBit / 8) - 1 // Fallback to forward indexing if reverse index is negative

            if (byteIndex >= payload.size || byteIndex < 0) return null



            when (signal.length) {
                8 -> if ((payload[byteIndex]) == FF) return null
                16 -> if (byteIndex > 1 && payload.sliceArray(byteIndex - 1..byteIndex).contentEquals(FFFF)) return null
                32 -> if (byteIndex > 3 && payload.sliceArray(byteIndex - 3..byteIndex).contentEquals(FFFFFFFF)) return null

            }

            var rawValue: ULong = 0.toULong()
            val bitIndex = 7 - (signal.startBit % 8)    // Reverse bit indexing within byte

            when (signal.byteOrder) {
                ByteOrder.INTEL -> {
                    // Intel (little-endian) byte order
                    var remainingBits = signal.length
                    var currentByte = byteIndex
                    var currentBit = bitIndex

                    while (remainingBits > 0) {
                        if (currentByte < 0) break

                        val bitValue = (payload[currentByte].toInt() shr currentBit) and 1
                        val shift = signal.length - remainingBits
                        rawValue = rawValue or (bitValue.toULong() shl shift)

                        currentBit--
                        if (currentBit < 0) {
                            currentBit = 7
                            currentByte--
                        }

                        remainingBits--
                    }
                }

                ByteOrder.MOTOROLA -> {
                    // Motorola (big-endian) byte order
                    var remainingBits = signal.length
                    var currentByte = byteIndex
                    var currentBit = bitIndex

                    while (remainingBits > 0) {
                        if (currentByte < 0) break

                        val bitValue = (payload[currentByte].toInt() shr currentBit) and 1
                        rawValue = (rawValue shl 1) or bitValue.toULong()

                        currentBit--
                        if (currentBit < 0) {
                            currentBit = 7
                            currentByte--
                        }

                        remainingBits--
                    }
                }
            }

            return rawValue
        }


        const val FF = 0xFF.toByte()
        val FFFF = ByteArray(2) { FF }
        val FFFFFFFF = ByteArray(4) { FF }

        fun reverseBits(byte: Byte): Byte {
            var b = byte.toInt() and 0xFF  // Convert to unsigned int
            var result = 0

            // Process each bit
            for (i in 0 until 8) {
                result = (result shl 1) or (b and 1)
                b = b shr 1
            }

            return result.toByte()
        }


        fun bytesToLongLittleEndian(bytes: ByteArray): Long {
            var result = 0L
            for (i in bytes.indices) {
                result = result or ((bytes[i].toLong() and 0xFF) shl (8 * i))
            }
            return result
        }

        fun bytesToLongBigEndian(bytes: ByteArray): Long {
            var result = 0L
            for (i in bytes.indices) {
                result = result or ((bytes[i].toLong() and 0xFF) shl (8 * (bytes.size - 1 - i)))
            }
            return result
        }
    }
}