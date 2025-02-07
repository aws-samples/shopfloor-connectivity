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

        private fun extractRawValueY(signal: J1939Signal, payload: ByteArray): Float? {
            var rawValue: Long = 0

            // For J1939, both bytes and bits are indexed in reverse
            // Convert start bit to account for reverse byte and bit ordering
            val byteIndex = 7 - (signal.startBit / 8)  // Reverse byte indexing
            val bitIndex = 7 - (signal.startBit % 8)    // Reverse bit indexing within byte

            if (byteIndex >= payload.size) return null // Invalid start bit

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
                        rawValue = rawValue or (bitValue.toLong() shl shift)

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
                        rawValue = (rawValue shl 1) or bitValue.toLong()

                        currentBit--
                        if (currentBit < 0) {
                            currentBit = 7
                            currentByte--
                        }

                        remainingBits--
                    }
                }
            }

            return rawValue.toFloat()
        }

        private fun extractRawValueX(signal: J1939Signal, payload: ByteArray): Float? {
            var rawValue: Long = 0

            //   val payload = reversePayload(payloadIn)
            // Calculate start byte and bit positions
            val startByte = signal.startBit / 8
            if (startByte >= payload.size) return null // Invalid start bit (past end of payload
            //if (payload[startByte] == 0xFF.toByte()) return null

            //val startBitInByte = signal.startBit % 8

            when (signal.byteOrder) {
                ByteOrder.INTEL -> {
                    // Intel (little-endian) byte order
                    var remainingBits = signal.length
                    var currentBit = signal.startBit

                    while (remainingBits > 0) {
                        val byteIndex = currentBit / 8
                        val bitIndex = currentBit % 8

                        // Don't read past the payload
                        if (byteIndex >= payload.size) break

                        // Calculate how many bits to read from this byte
                        val bitsToRead = minOf(8 - bitIndex, remainingBits)

                        // Create a mask for the bits we want
                        val mask = ((1L shl bitsToRead) - 1) shl bitIndex

                        // Extract the bits and shift them to the right position
                        val extractedBits = (payload[byteIndex].toInt() and mask.toInt()) shr bitIndex

                        // Add the bits to our result
                        val shift = signal.length - remainingBits
                        rawValue = rawValue or (extractedBits.toLong() shl shift)

                        remainingBits -= bitsToRead
                        currentBit += bitsToRead
                    }
                }

                ByteOrder.MOTOROLA -> {
                    // Motorola (big-endian) byte order
                    var remainingBits = signal.length
                    var currentBit = signal.startBit

                    while (remainingBits > 0) {
                        val byteIndex = currentBit / 8
                        val bitIndex = 7 - (currentBit % 8)  // Reverse bit order within byte

                        // Don't read past the payload
                        if (byteIndex >= payload.size) break

                        // Calculate how many bits to read from this byte
                        val bitsToRead = minOf(bitIndex + 1, remainingBits)

                        // Create a mask for the bits we want
                        val mask = ((1L shl bitsToRead) - 1) shl (bitIndex - bitsToRead + 1)

                        // Extract the bits and shift them to the right position
                        val extractedBits = (payload[byteIndex].toInt() and mask.toInt()) shr
                                (bitIndex - bitsToRead + 1)

                        // Add the bits to our result
                        val shift = remainingBits - bitsToRead
                        rawValue = rawValue or (extractedBits.toLong() shl shift)

                        remainingBits -= bitsToRead
                        currentBit += bitsToRead
                    }
                }
            }

            // Handle signed values
            if (signal.valueType == ValueType.SIGNED) {
                val signBit = 1L shl (signal.length - 1)
                if (rawValue and signBit != 0L) {
                    // If sign bit is set, extend the sign
                    rawValue = rawValue or (-1L shl signal.length)
                }
            }

            return rawValue.toFloat()
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

        fun reversePayload(bytes: ByteArray): ByteArray {
            return bytes.reversed().map { reverseBits(it) }.toByteArray()

        }
    }
}