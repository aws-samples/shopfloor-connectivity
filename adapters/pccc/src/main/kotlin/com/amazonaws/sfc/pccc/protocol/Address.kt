/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.pccc.protocol

import java.io.ByteArrayOutputStream


class Address private constructor(
    // An application or user defined label for the address
    private val label: String,
    // The datafile type  the address is for, e.g. BINARY for B3:0/1
    val dataFile: DataFile,
    // The number of the data file, e.g. 3 for B3:0/1
    val dataFileNumber: Short,
    // The index of the item, e.g. 0 for B3:0/1
    val index: Short,
    // Bit indexes and offsets for sub element of value in the address
    val addressOffset: AddressSubElement?,
    // Number of values, e.g. 2 for B3:0,2
    val arrayLen: Int,
) {

    // The length in bytes to read for the data for this address
    private val dataLengthInBytes =
        when (dataFile) {
            // adjustments for INPUT and OUTPUT data types
            DataFileType.INPUT, DataFileType.OUTPUT -> {
                val len = (((addressOffset?.bitOffset ?: 0) + arrayLen).toDouble().div(8) + 0.99999).toInt()
                len + (len % 2)
            }

            else ->
                arrayLen * dataFile.sizeOfSingleItemInBytes
        }

    // Builds a buffer containing the bytes of address information in the PCCC read packet
    val addressBytes: ByteArray by lazy {
        buildAddressBytes(dataFile, dataFileNumber, index, dataOffsetInItem, dataLengthInBytes.toByte())
    }

    // Offset of the data in the item
    val dataOffsetInItem =
        (when (dataFile) {
            DataFileType.OUTPUT, DataFileType.INPUT ->
                if (addressOffset?.bitOffset != null && addressOffset.bitOffset!! > 16)
                    (addressOffset.bitOffset!! / 16) else addressOffset?.element ?: 0

            else -> 0
        }).toShort()


    override fun toString(): String {
        return label
    }

    companion object {

        private fun create(
            str: String,
            dataFile: DataFile,
            dataTypeNumber: Short,
            element: Short,
            addressSubElement: AddressSubElement?,
            arrayLen: Int
        ): Address {
            return Address(str, dataFile, dataTypeNumber, element, addressSubElement, arrayLen)
        }

        // These types support data offset
        private val supportsOffset = setOf(
            DataFileType.OUTPUT,
            DataFileType.INPUT,
            DataFileType.BINARY,
            DataFileType.COUNTER,
            DataFileType.TIMER,
            DataFileType.CONTROL,
            DataFileType.ASCII
        )

        // Multiple values of these types can be read as arrays
        private val supportsArrays = setOf(
            DataFileType.OUTPUT,
            DataFileType.INPUT,
            DataFileType.BINARY,
            DataFileType.INT16,
            DataFileType.INT32,
            DataFileType.FLOAT,
            DataFileType.ASCII
        )

        // Helper method for specific encoding of short values in packet data
        private fun shortAsBytes(value: Short): ByteArray =
            if (value <= 254) {
                byteArrayOf(value.toByte())
            } else {
                byteArrayOf(0xFF.toByte(), (value.toInt() and 0xFF).toByte(), (value.toInt() shr 8 and 0xFF).toByte())
            }

        // Parsers an address from a string
        fun parse(s: String): Address {

            val (dataFileStr, elementStr) = splitAddressParts(s)
            val (dataFile, dataFileNUmber) = parseDatafile(dataFileStr)
            val index = parseIndex(elementStr)
            val offset = parseAddressElement(dataFile, elementStr)

            if (offset != null && dataFile !in supportsOffset)
                throw AddressException("Data type $dataFile does not support offsets")

            if (dataFile == DataFileType.ASCII &&
                (((offset?.element ?: 0) !in 0..1) || ((offset?.bitOffset ?: 0) !in 0..1))
            ) throw AddressException("Element number or bit index for data type $dataFile must be 0 or 1")

            val arrayLen = parseArrayLen(elementStr)
            if (arrayLen > 1) {
                if (dataFile !in supportsArrays) throw AddressException("Arrays not supported for data type $dataFile")
                if (offset != null) throw AddressException("Arrays not supported for addresses with offsets")
            }
            return create(s, dataFile, dataFileNUmber, index, offset, arrayLen)
        }


        // Split address parts, for "B3:0/1" the result would be ["B3", "0/1"]
        private fun splitAddressParts(s: String): Pair<String, String> {
            val parts = s.split(":")
            if (parts.size != 2) throw AddressException("\":\" missing in address \"$s\" ")

            if (parts[1].replace("/", ".")
                    .split(".").size > 2
            ) throw AddressException("Only one '/'  or '.' allowed in address, not both")

            return Pair(parts[0].uppercase(), parts[1].uppercase())
        }

        // Parses index, for "0/1" or "0.ACC" this would be 0
        private fun parseIndex(s: String): Short {
            val parts = s.split(",")[0].takeWhile { it.isDigit() }
            return try {
                parts.toShort()
            } catch (_: NumberFormatException) {
                throw AddressException("Invalid element number")
            }
        }

        // Parses a sub element by its known name, for the "0.ACC" part in "C5:0.ACC" this would be AddressSubElement.ACC
        private fun parseSubElementByName(dataFile: DataFile, s: String): AddressSubElement? {
            val subElementName = s.replace(Regex("[0-9]"), "").uppercase()
            if (subElementName.isNotEmpty()) {
                val e = AddressNamedSubElement.parse(subElementName)
                if (!dataFile.knownFields.contains(e)) throw AddressException(
                    "\"$subElementName\" is not a valid fields for data file type ${dataFile.prefix}, " +
                            if (dataFile.knownFields.isEmpty())
                                "as it supports no fields"
                            else
                                "valid fields are ${dataFile.knownFields.joinToString { it.name }}"
                )
                return e
            }
            return null
        }

        // Parses bit index of addresses and validates range
        private fun parseNumericBitOffset(s: String): AddressConfiguredSubElement {
            return try {
                val i = s.uppercase().replace(Regex("[A-Z]"), "").toInt()
                if (i < 0 || i > 15) throw AddressException("BitOffset must be in range 0..15")
                AddressConfiguredSubElement(bitOffset = i)
            } catch (_: NumberFormatException) {
                throw AddressException("Invalid bit index")
            }
        }

        // Parses the element index of an address,
        private fun parseElementIndex(s: String): AddressConfiguredSubElement {
            return try {
                val i = s.uppercase().replace(Regex("[A-Z]"), "").toInt()
                AddressConfiguredSubElement(element = i)
            } catch (_: NumberFormatException) {
                throw AddressException("Invalid index")
            }
        }

        // Gets the sub element, which could be a numeric index or by name or o sub element
        private fun parseAddressElement(dataFile: DataFile, s: String): AddressSubElement? {

            // either a . or / can be used in an address
            if (s.count { it == '/' } > 0 && s.count { it == '.' } > 0) throw AddressException("Address can only contain a '.' or '/'")

            // splits in parts separated by the "/", th last part is the bit index (or char index for ASCII)
            var parts = s.split(",")[0].split("/")
            when (parts.size) {

                1 -> {/* no index */
                }

                2 -> {
                    // Known indexes like "EN" in T4:0/EN -> AddressNamedSubElement.EN
                    val knownField = parseSubElementByName(dataFile, parts[1])
                    if (knownField != null) return knownField
                    // Numeric index like 1 in "B3:0/1"
                    return parseNumericBitOffset(parts[1])
                }

                else ->
                    throw AddressException("Only one '/' allowed in address")
            }

            parts = s.split(",")[0].split(".")

            when (parts.size) {
                1 -> return null
                2 -> {
                    // Know element like "ACC" in "T4:0.ACC" -> AddressNamedSubElement.ACC
                    val knownField = parseSubElementByName(dataFile, parts[1])
                    if (knownField != null) return knownField
                    // This is for reading a specific character in ASCII values, e.g. "A12:4.0" which stands for the first character in A12:4 or
                    // reading INPUT and OUTPUT "rows" like O0:0.1 which reads O0:0/16 to O0:0/31
                    return parseElementIndex(parts[1])
                }

                else ->
                    throw AddressException("Only one '.' allowed in address")


            }
        }

        // Get the array length this for B3:0,2 this would be 2
        private fun parseArrayLen(s: String): Int {
            val parts = s.split(",")
            val len = when (parts.size) {
                1 -> 1
                2 -> {
                    try {
                        parts[1].toInt()
                    } catch (_: NumberFormatException) {
                        throw Exception("\"$parts[1]\" is not a valid array length")
                    }
                }

                else -> throw Exception("only one ',' in address allowed")
            }
            return if (len > 0) len else throw AddressException("Array length must be greater than 0")
        }

        private fun parseDatafile(s: String): Pair<DataFile, Short> {
            val parts = s.split(":")

            val dataFileStr = parts[0]
            return Pair(
                DataFileType.parseDataFileType(dataFileStr),
                DataFileType.parseDataFileNumber(dataFileStr)
            )
        }

        private fun buildAddressBytes(
            dataFile: DataFile,
            dataFileNumber: Short,
            index: Short,
            dataOffset: Short,
            len: Byte
        ): ByteArray {

            val buffer = ByteArrayOutputStream()

            // header
            buffer.write(byteArrayOf(0xa2.toByte(), len))
            // FileNumber
            buffer.write(shortAsBytes(dataFileNumber))
            //File type
            buffer.write(byteArrayOf(dataFile.area))
            //Offset
            buffer.write(shortAsBytes(index))
            buffer.write(shortAsBytes(dataOffset))

            return buffer.toByteArray()

        }

        val List<Address>.addressBytes: ByteArray
            get() {
                return if (this.size == 1) {
                    first().addressBytes
                } else {
                    val length = this.totalLength
                    with(first()) {
                        buildAddressBytes(
                            dataFile,
                            dataFileNumber,
                            index,
                            dataOffsetInItem,
                            length.toByte()
                        )
                    }
                }

            }

        val List<Address>.totalLength: Int
            get() {
                val firstAddress = this.first()
                val dataLenPerItem = firstAddress.dataFile.sizeOfSingleItemInBytes
                val startPositionOfFirstAddress = firstAddress.dataOffsetInItem + (firstAddress.index * dataLenPerItem)
                val endPositionOfLastAddress = maxOf { (it.index * dataLenPerItem) + (it.arrayLen * dataLenPerItem) }
                return (endPositionOfLastAddress - startPositionOfFirstAddress)
            }


    }

}


