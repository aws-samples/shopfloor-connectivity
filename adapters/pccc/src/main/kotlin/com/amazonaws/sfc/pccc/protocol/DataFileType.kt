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

import com.amazonaws.sfc.pccc.protocol.Decoders.decodeAscii
import com.amazonaws.sfc.pccc.protocol.Decoders.decodeAsciiList

import com.amazonaws.sfc.pccc.protocol.Decoders.decodeString
import com.amazonaws.sfc.pccc.protocol.Decoders.decodeStructuredData
import com.amazonaws.sfc.pccc.protocol.Decoders.toBoolean
import com.amazonaws.sfc.pccc.protocol.Decoders.toBooleanList
import com.amazonaws.sfc.pccc.protocol.Decoders.toFloat
import com.amazonaws.sfc.pccc.protocol.Decoders.toFloatListLE
import com.amazonaws.sfc.pccc.protocol.Decoders.toIn16List
import com.amazonaws.sfc.pccc.protocol.Decoders.toIn32List
import com.amazonaws.sfc.pccc.protocol.Decoders.toInt16
import com.amazonaws.sfc.pccc.protocol.Decoders.toInt32


private const val CONTROLLER_STATUS_AREA = 0x84.toByte()
private const val CONTROLLER_BIT_AREA = 0x85.toByte()
private const val CONTROLLER_TIMER_AREA = 0x86.toByte()
private const val CONTROLLER_COUNTER_AREA = 0x87.toByte()
private const val CONTROLLER_CONTROL_AREA = 0x88.toByte()
private const val CONTROLLER_INT_AREA = 0x89.toByte()

// private const val CONTROLLER_NSTRING_AREA = 0x89.toByte()
private const val CONTROLLER_FLOAT_AREA = 0x8a.toByte()
private const val CONTROLLER_OUTPUT_AREA = 0x8b.toByte()
private const val CONTROLLER_INPUT_AREA = 0x8c.toByte()
private const val CONTROLLER_STRING_AREA = 0x8d.toByte()
private const val CONTROLLER_ASCII_AREA = 0x8e.toByte()
private const val CONTROLLER_LONG_AREA = 0x91.toByte()


enum class DataFileType : DataFile {

    OUTPUT {
        override val prefix = "O"
        override val area = CONTROLLER_OUTPUT_AREA
        override val sizeOfSingleItemInBytes = 2
        override val defaultFileNumber = 0.toShort()
        override fun decodeValue(bytes: ByteArray, addressSubElement: AddressSubElement?): Any =
            bytes.toBoolean(addressSubElement?.bitOffset)

        override fun decodeArrayValue(bytes: ByteArray): ArrayList<Any> = bytes.toBooleanList
    },

    INPUT {
        override val prefix = "I"
        override val area = CONTROLLER_INPUT_AREA
        override val sizeOfSingleItemInBytes = 2
        override val defaultFileNumber = 1.toShort()
        override fun decodeValue(bytes: ByteArray, addressSubElement: AddressSubElement?): Any =
            bytes.toBoolean(addressSubElement?.bitOffset)

        override fun decodeArrayValue(bytes: ByteArray): ArrayList<Any> = bytes.toBooleanList

    },

    STATUS {
        override val prefix = "S"
        override val area = CONTROLLER_STATUS_AREA
        override val sizeOfSingleItemInBytes = 2
        override val defaultFileNumber = 2.toShort()
        override fun decodeValue(bytes: ByteArray, addressSubElement: AddressSubElement?): Any = bytes.toInt16
        override fun decodeArrayValue(bytes: ByteArray): ArrayList<Any> = bytes.toIn16List
    },

    BINARY {
        override val prefix = "B"
        override val area = CONTROLLER_BIT_AREA
        override val sizeOfSingleItemInBytes = 2
        override val defaultFileNumber = 3.toShort()
        override fun decodeValue(bytes: ByteArray, addressSubElement: AddressSubElement?): Any =
            bytes.toBoolean(addressSubElement?.bitOffset)

        override fun decodeArrayValue(bytes: ByteArray): ArrayList<Any> = bytes.toBooleanList
    },

    TIMER {
        override val prefix = "T"
        override val area = CONTROLLER_TIMER_AREA
        override val sizeOfSingleItemInBytes = 6
        override val defaultFileNumber = 4.toShort()
        override fun decodeValue(bytes: ByteArray, addressSubElement: AddressSubElement?): Any =
            bytes.decodeStructuredData(addressSubElement, knownFields)

        override fun decodeArrayValue(bytes: ByteArray): ArrayList<Any> {
            throw NotImplementedError()
        }

        override val knownFields = listOf(
            AddressNamedSubElement.EN,
            AddressNamedSubElement.TT,
            AddressNamedSubElement.DN,
            AddressNamedSubElement.BASE,
            AddressNamedSubElement.PRE,
            AddressNamedSubElement.ACC
        )
    },

    COUNTER {
        override val prefix = "C"
        override val area = CONTROLLER_COUNTER_AREA
        override val sizeOfSingleItemInBytes = 6
        override val defaultFileNumber = 5.toShort()
        override fun decodeValue(bytes: ByteArray, addressSubElement: AddressSubElement?): Any =
            bytes.decodeStructuredData(addressSubElement, knownFields)

        override fun decodeArrayValue(bytes: ByteArray): ArrayList<Any> {
            throw NotImplementedError()
        }

        override val knownFields = listOf(
            AddressNamedSubElement.CU,
            AddressNamedSubElement.CD,
            AddressNamedSubElement.DN,
            AddressNamedSubElement.OV,
            AddressNamedSubElement.UN,
            AddressNamedSubElement.UA,
            AddressNamedSubElement.PRE,
            AddressNamedSubElement.ACC
        )
    },

    CONTROL {
        override val prefix = "R"
        override val area = CONTROLLER_CONTROL_AREA
        override val sizeOfSingleItemInBytes = 6
        override val defaultFileNumber = 6.toShort()
        override fun decodeValue(bytes: ByteArray, addressSubElement: AddressSubElement?): Any =
            bytes.decodeStructuredData(addressSubElement, knownFields)

        override fun decodeArrayValue(bytes: ByteArray): ArrayList<Any> {
            throw NotImplementedError()
        }

        override val knownFields = listOf(
            AddressNamedSubElement.EN,
            AddressNamedSubElement.EU,
            AddressNamedSubElement.DN,
            AddressNamedSubElement.EM,
            AddressNamedSubElement.ER,
            AddressNamedSubElement.UL,
            AddressNamedSubElement.IN,
            AddressNamedSubElement.FD,
            AddressNamedSubElement.LEN,
            AddressNamedSubElement.POS
        )
    },

    INT16 {
        override val prefix = "N"
        override val area = CONTROLLER_INT_AREA
        override val sizeOfSingleItemInBytes = 2
        override val defaultFileNumber = 7.toShort()
        override fun decodeValue(bytes: ByteArray, addressSubElement: AddressSubElement?): Any = bytes.toInt16
        override fun decodeArrayValue(bytes: ByteArray): ArrayList<Any> = bytes.toIn16List
    },

    FLOAT {
        override val prefix = "F"
        override val area = CONTROLLER_FLOAT_AREA
        override val sizeOfSingleItemInBytes = 4
        override val defaultFileNumber = 0.toShort()
        override fun decodeValue(bytes: ByteArray, addressSubElement: AddressSubElement?): Any = bytes.toFloat
        override fun decodeArrayValue(bytes: ByteArray): ArrayList<Any> = bytes.toFloatListLE
    },

    STRING {
        override val prefix = "ST"
        override val area = CONTROLLER_STRING_AREA
        override val sizeOfSingleItemInBytes = 84
        override fun decodeValue(bytes: ByteArray, addressSubElement: AddressSubElement?): Any = bytes.decodeString
        override fun decodeArrayValue(bytes: ByteArray): ArrayList<Any> {
            return ArrayList(bytes.toList().chunked(sizeOfSingleItemInBytes).map { it.toByteArray().decodeString })
        }
    },

    INT32 {
        override val prefix = "L"
        override val area = CONTROLLER_LONG_AREA
        override val sizeOfSingleItemInBytes = 4
        override fun decodeValue(bytes: ByteArray, addressSubElement: AddressSubElement?): Any = bytes.toInt32
        override fun decodeArrayValue(bytes: ByteArray): ArrayList<Any> = bytes.toIn32List
    },

    ASCII {
        override val prefix = "A"
        override val area = CONTROLLER_ASCII_AREA
        override val sizeOfSingleItemInBytes = 2
        override fun decodeValue(bytes: ByteArray, addressSubElement: AddressSubElement?): Any {
            return bytes.decodeAscii(addressSubElement)
        }

        override fun decodeArrayValue(bytes: ByteArray): ArrayList<Any> = bytes.decodeAsciiList
    };


//    NSTRING {
//        override val prefix = "NST"
//        override val area = CONTROLLER_NSTRING_AREA
//        override val dataLengthInBytes = 88
//        override fun decodeValue(bytes: ByteArray, addressElement: AddressElement?) = bytes.decodeNString
//
//        override fun decodeArrayValue(bytes: ByteArray): ArrayList<Any> {
//            return ArrayList(bytes.toList().chunked(dataLengthInBytes).map{it.toByteArray().decodeNString} )
//        }
//    };

    companion object {

        fun parseDataFileType(s: String): DataFile {
            val prefix: String = s.uppercase().takeWhile { it.category == CharCategory.UPPERCASE_LETTER }
            return entries.firstOrNull { prefix == it.prefix }
                ?: throw AddressException("Address \"$s\" has has no valid file type")
        }

        fun parseDataFileNumber(s: String): Short {
            val fileNumberStr: String = s.trimStart { !it.isDigit() }
            if (fileNumberStr.isNotEmpty()) {
                return try {
                    fileNumberStr.toShort()
                } catch (_: NumberFormatException) {
                    throw AddressException("No valid file number")
                }
            }

            return parseDataFileType(s).defaultFileNumber ?: throw AddressException("No valid file number")

        }


    }

}