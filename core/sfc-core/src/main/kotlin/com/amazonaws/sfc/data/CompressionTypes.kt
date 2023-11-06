
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data

import com.google.gson.annotations.SerializedName

enum class CompressionType {
    @SerializedName("None")
    NONE {
        override val mime = "application/json"
        override val ext = ""
    },

    @SerializedName("Zip")
    ZIP {
        override val mime = "application/zip"
        override val ext = ".zip"
    },

    @SerializedName("GZip")
    GZIP {
        override val mime = "application/gzip"
        override val ext = ".gzip"
    };

    abstract val mime: String
    val mimeType
        get() = mime

    abstract val ext: String
    val extension
        get() = ext
}



