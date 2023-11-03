/*
 *
 *
 *     Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *      Licensed under the Amazon Software License (the "License"). You may not use this file except in
 *      compliance with the License. A copy of the License is located at :
 *
 *      http://aws.amazon.com/asl/
 *
 *      or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 *      language governing permissions and limitations under the License.
 *
 *
 *
 */

package com.amazonaws.sfc.pccc.protocol

import java.io.Serializable
import java.nio.ByteBuffer
import java.nio.ByteOrder

object Decoders {


    // Decodes a 2 byte array to 16 boolean values, the first bit of the bytes (15) is stored at index 15,
    // the last bit is stored at index 0 so B0:0/15 is stores [15] and B0:0/0 at [0]
    private val ByteArray.to16Bits: ArrayList<Boolean>
        get() {
            val shortVal = this.toInt16
            return ArrayList((0..15).map { i ->
                shortVal.toInt() and (0x01 shl i) != 0
            })
        }

    // Encodes an INT16 as Little Endian bytes
    val Short.bytes: ByteArray
        get() = byteArrayOf((this.toInt() and 0xFF).toByte(), (this.toInt() shr 8 and 0xFF).toByte())

    // Decodes a list of ASCII values
    val ByteArray.decodeAsciiList: ArrayList<Any>
        get() = ArrayList(this.toList().chunked(2).flatMap { it.toByteArray().decodeAscii(null) as ArrayList<*> })

    // Decoded a string value
    val ByteArray.decodeString: String
        get() {
            if (this.isEmpty()) return ""

            // first byte is length
            val strLen = this[0].toInt()
            if (this.size < strLen + 2) return ""

            // Get start and end of character data
            val str = StringBuffer(strLen)
            val start = 2
            val end = minOf(this.size - 1, (1 + strLen + (strLen % 2)))

            // slice character data in pairs of 2
            this.sliceArray(start..end).toList().chunked(2).takeWhile { str.length < strLen }.forEach {
                // Characters pairs need to be swapped
                str.append(it[1].toInt().toChar())
                if (str.length < strLen) str.append(it[0].toInt().toChar())
            }
            return str.toString()
        }

    // Decodes a list of boolean values
    val ByteArray.toBooleanList: ArrayList<Any>
        get() = ArrayList(this.toList().chunked(2).map { it.toByteArray().to16Bits })

    // Decodes a float value from Little Endian bytes
    val ByteArray.toFloat: Float
        get() = ByteBuffer.wrap(this).order((ByteOrder.LITTLE_ENDIAN)).getFloat()

    // Decodes a list of float values from Little Endian bytes
    val ByteArray.toFloatListLE: ArrayList<Any>
        get() = ArrayList(this.toList().chunked(4).map { it.toByteArray().toFloat })

    // Decodes a list of INT16 values from Little Endian bytes
    val ByteArray.toIn16List: ArrayList<Any>
        get() = ArrayList(this.toList().chunked(2).map { it.toByteArray().toInt16 })

    // Decodes a list of INT32 values from Little Endian bytes
    val ByteArray.toIn32List: ArrayList<Any>
        get() = ArrayList(this.toList().chunked(4).map { it.toByteArray().toInt32 })

    // Decodes an INT16 value from Little Endian bytes
    val ByteArray.toInt16: Short
        get() = if (this.size == 2) ((this[1].toInt() and 0xFF shl 8) or (this[0].toInt() and 0xFF)).toShort() else
            throw IllegalArgumentException("ByteArray must contain 2 bytes")

    // Decodes an INT32 value from Little Endian bytes
    val ByteArray.toInt32: Int
        get() = if (this.size == 4) (this[3].toInt() and 0xFF shl 24) or (this[2].toInt() and 0xFF shl 16) or (this[1].toInt() and 0xFF shl 8) or (this[0].toInt() and 0xFF) else
            throw IllegalArgumentException("ByteArray must contain 4 bytes")

    // Decodes an ASCII value from a byte pair
    fun ByteArray.decodeAscii(addressSubElement: AddressSubElement?): Any {
        val a = arrayListOf(this[1].toInt().toChar(), this[0].toInt().toChar())
        return if (addressSubElement != null) {
            if (addressSubElement.bitOffset == 1 || addressSubElement.element == 1) a[1] else a[0]
        } else a
    }

    // Decodes a structure data type, returns a specified element or all elements as a map if no element is specified
    fun ByteArray.decodeStructuredData(offsets: AddressSubElement?, fields: List<AddressNamedSubElement>): Any {
        return if (offsets != null) {
            return this.decodeField(offsets)
        } else {
            this.decodeAsStruct(fields)
        }
    }

    // Read an INT16 from a byte array at the specified position
    fun ByteArray.readInt16(offset: Int): Short {
        if (this.size < offset + 2) throw IllegalArgumentException("Length of buffer is smaller than ${offset + 2}")
        return this.sliceArray(offset..offset + 1).toInt16
    }

    // Read an INT32 from a byte array at the specified position
    fun ByteArray.readInt32(offset: Int): Int {
        if (this.size < offset + 4) throw IllegalArgumentException("Length of buffer is smaller than ${offset + 4}")
        return this.sliceArray((offset..offset + 3)).toInt32
    }

    // Get a bit as a boolean from a byte array
    fun ByteArray.toBoolean(bitIndex: Int?): Serializable {
        val bits = this.to16Bits
        return if (bitIndex != null) bits[bitIndex % 16] else bits
    }

    // Decodes all fields in a structured value and returns it as a map with a value for every know fields
    private fun ByteArray.decodeAsStruct(knownFields: List<AddressNamedSubElement>) =
        knownFields.associate {
            it.name to this.decodeField(it)
        }


    private fun ByteArray.decodeField(offsets: AddressSubElement): Any {
        var b = this.sliceArray(offsets.wordOffset..offsets.wordOffset + 1).toInt16
        if (offsets.shr != null) b = (b.toInt() shr offsets.shr!!.toInt()).toShort()
        if (offsets.shl != null) b = (b.toInt() shl offsets.shl!!.toInt()).toShort()
        if (offsets.mask != null) b = (b.toInt() and offsets.mask!!.toInt()).toShort()
        return if (offsets.bitOffset == null) b else (b.toInt() and (0x01 shl offsets.bitOffset!!) != 0)
    }

    //    val ByteArray.decodeNString: String
//        get() {
//            if (this.isEmpty()) return ""
//
//            val strLen = this.readInt16(0).toInt()
//            if (this.size < strLen + 2) return ""
//
//            val str = StringBuffer(strLen)
//            val start = 4
//            val end = minOf(this.size - 1, (2 + strLen))
//
//            this.sliceArray(start..end).takeWhile { str.length < strLen }.forEach {
//                str.append(it.toInt().toChar())
//            }
//            return str.toString()
//        }

}