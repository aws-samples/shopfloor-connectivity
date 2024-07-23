// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.slmp.protocol

object SlmpDecoder {

    fun decode(itemDefinition: SlmpDeviceItem, data : ByteArray): Any {

        return when (itemDefinition.dataType) {
            SlmpDataType.STRING -> {
                decodeStrings(data, itemDefinition.size, itemDefinition.strLen)
            }

            SlmpDataType.WORD -> {
                decodeWords(data, itemDefinition.size)
            }

            SlmpDataType.BIT -> decodeBits(data, itemDefinition.size)
            SlmpDataType.DOUBLEWORD -> decodeDoubleWords(data, itemDefinition.size)
            SlmpDataType.STRUCT -> itemDefinition.structureType!!.decode(data, itemDefinition)
        }

    }

    fun decodeWords(bytes: ByteArray, size: Int = 1): Any {
        if (bytes.size < size) throw IllegalArgumentException("Insufficient number of bytes to decode words, need at least $size bytes to decode $size words but only ${bytes.size} we provided")
        val words = ShortArray(size)

        for (i in 0 until size) {
            val index = i * 2
            words[i] = (((bytes[index + 1].toInt() and 0xff) shl 8) or (bytes[index].toInt() and 0xff)).toShort()
        }
        if (size == 1) return words.first()
        return words.toList()
    }

    private fun decodeBits(bytes: ByteArray, size: Int = 1): Any {
        val bytesNeeded = if (size % 2 == 0) size / 2 else (size / 2) + 1
        if (bytes.size < bytesNeeded) throw SlmpException(
            "Insufficient number of bytes to decode bits, need at least $bytesNeeded bytes to decode $size bits but only ${bytes.size} we provided"
        )
        val bits = BooleanArray(size)

        for (i in 0 until size) {
            val b = bytes[i / 2]

            bits[i] = if (i % 2 == 0) (b.toInt() and 0x10 == 0x10) else (b.toInt() and 0x01 == 0x01)
        }

        if (size == 1) return bits.first()
        return bits.toList().slice(0 until minOf(size, bytes.size * 2))
    }


     fun decodeDoubleWords(bytes: ByteArray, size: Int = 1): Any {
        val bytesNeeded = size * 4
        if (bytes.size < bytesNeeded) throw IllegalArgumentException("Insufficient number of bytes to decode  $size double words, need at least $bytesNeeded bytes to decode $size double words but only ${bytes.size} we provided")
        val doubleWords = IntArray(size)

        for (i in 0 until size) {
            val index = i * 4
            //According to Mitsubishi documentation the words forming the double word are in most significant - least
            // significant order whilst bytes in these words are LSB - MSB
            val doubleWord1 = (bytes[index].toInt() and 0xff) or ((bytes[index + 1].toInt() and 0xff) shl 8)
            val doubleWord2 = (bytes[index + 2].toInt() and 0xff) or ((bytes[index + 3].toInt() and 0xff) shl 8)
            doubleWords[i] = (doubleWord1 shl 16) or doubleWord2
        }
        if (size == 1) return doubleWords.first()
        return doubleWords.toList()
    }

    private fun decodeStrings(bytes: ByteArray, size: Int = 1, len: Int): Any {
        val bytesNeeded = len * size

        if (bytes.size < bytesNeeded) throw SlmpException(
            "Insufficient number of bytes to decode strings, need at least $bytesNeeded bytes to decode $size strings of $len characters but only ${bytes.size} we provided"
        )

        val strings = mutableListOf<String>()
        for (index in 0 until size) {

            val stringBytes = bytes.sliceArray(index * len until (index + 1) * len)

            val string = StringBuilder(len)

            var offset = 0
            while (offset < len && stringBytes[offset] != 0.toByte()) {
                string.append(stringBytes[offset].toInt().toChar())
                offset++
            }
            strings.add(string.toString())

        }

        return if (size == 1) strings.first() else strings
    }

    val Short.lowAndHighByte: ByteArray
        get() = byteArrayOf((this.toInt() and 0xFF).toByte(), (this.toInt() shr 8 and 0xFF).toByte())

    val UShort.lowAndHighByte: ByteArray
        get() = this.toShort().lowAndHighByte

    fun ByteArray.readInt16(offset: Int): Short {
        if (this.size < offset + 2) throw SlmpException("Length of buffer is smaller than ${offset + 2}")
        return this.sliceArray(offset..offset + 1).toInt16
    }

    private val ByteArray.toInt16: Short
        get() = if (this.size < 2) throw SlmpException("ByteArray must contain 2 bytes") else
            ((this[1].toInt() and 0xFF shl 8) or (this[0].toInt() and 0xFF)).toShort()


}