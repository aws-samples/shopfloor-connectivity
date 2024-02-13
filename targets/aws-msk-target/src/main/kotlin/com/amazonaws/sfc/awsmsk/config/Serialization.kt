package com.amazonaws.sfc.awsmsk.config

import com.google.gson.annotations.SerializedName

enum class Serialization {

        @SerializedName("json")
        JSON,
        @SerializedName("protobuf")
        PROTOBUF,


}