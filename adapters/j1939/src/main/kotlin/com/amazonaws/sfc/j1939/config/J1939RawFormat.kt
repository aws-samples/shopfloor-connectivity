// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.j1939.config

import com.google.gson.annotations.SerializedName

enum class J1939RawFormat {

    @SerializedName("Bytes")
    BYTES,

    @SerializedName("BytesMasked")
    BYTES_MASKED,

    @SerializedName("LittleEndian")
    LITTLE_ENDIAN,

    @SerializedName("LittleEndianMasked")
    LITTLE_ENDIAN_MASKED,

    @SerializedName("BigEndian")
    BIG_ENDIAN,

    @SerializedName("BigEndianMasked")
    BIG_ENDIAN_MASKED;

    val isMasked: Boolean
        get() = this in listOf<J1939RawFormat>(BYTES_MASKED, LITTLE_ENDIAN_MASKED, BIG_ENDIAN_MASKED)
}


