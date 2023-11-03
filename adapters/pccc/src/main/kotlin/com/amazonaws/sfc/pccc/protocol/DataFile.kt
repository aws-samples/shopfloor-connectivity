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