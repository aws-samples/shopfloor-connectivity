package com.amazonaws.sfc.awsmsk.config

import com.google.gson.annotations.SerializedName

enum class Acknowledgements {

    @SerializedName("none")
    NONE {
        override val value
            get() = "0"
    },

    @SerializedName("leader")
    LEADER {
        override val value
            get() = "1"
    },

    @SerializedName("all")
    ALL {
        override val value
            get() = "all"
    };

    abstract val value: String


    override fun toString(): String {
        return value
    }
}

