package com.amazonaws.sfc.awsmsk.config

import com.google.gson.annotations.SerializedName

enum class Compression {

    @SerializedName("none")
    NONE {
        override val value = "none"
    },

    @SerializedName("snappy")
    SNAPPY {
        override val value = "snappy"
    },

    @SerializedName("lz4")
    LZ4 {
        override val value = "lz4"
    },

    @SerializedName("gzip")
    GZIP {
        override val value = "gzip"
    },

    @SerializedName("Gzip")
    GZIP_ {
        override val value = "gzip"
    },

    @SerializedName("zstd")
    ZSTD {
        override val value = "zstd"
    };

    abstract val value: String


    companion object {
        fun fromString(s: String): Compression {
            return entries.find { i: Compression -> i.value == s } ?: NONE
        }

    }


    override fun toString(): String {
        return value
    }




}