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


// Contains a set of addresses of the same data file and number which can be read with a single read request
data class OptimizedAddressSet(val addresses: List<Address>, val readPacket: ByteArray, val sequenceNumber: Short) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OptimizedAddressSet

        if (addresses != other.addresses) return false
        if (!readPacket.contentEquals(other.readPacket)) return false
        if (sequenceNumber != other.sequenceNumber) return false

        return true
    }

    override fun hashCode(): Int {
        var result = addresses.hashCode()
        result = 31 * result + readPacket.contentHashCode()
        result = 31 * result + sequenceNumber
        return result
    }
}