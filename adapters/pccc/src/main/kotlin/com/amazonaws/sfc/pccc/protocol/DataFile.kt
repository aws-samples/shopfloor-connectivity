/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.pccc.protocol

interface DataFile {
    // Prefix used in the address for this type, e.g. "B: for BINARY
    val prefix: String

    // Address area in controller for this type
    val area: Byte

    // The size in bytes for a value for this type
    val sizeOfSingleItemInBytes: Int

    // Default file number is none is specified O:0 -> O1:0
    val defaultFileNumber: Short? get() = null

    // Named fields for this data date
    val knownFields: List<AddressNamedSubElement> get() = emptyList()

    // decode methods for received data
    fun decodeValue(bytes: ByteArray, addressSubElement: AddressSubElement? = null): Any
    fun decodeArrayValue(bytes: ByteArray): ArrayList<Any>
}