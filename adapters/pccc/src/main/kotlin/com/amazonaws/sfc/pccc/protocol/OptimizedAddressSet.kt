/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
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