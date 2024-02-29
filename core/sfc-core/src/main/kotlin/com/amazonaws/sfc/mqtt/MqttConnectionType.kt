package com.amazonaws.sfc.mqtt

import com.google.gson.annotations.SerializedName

enum class MqttConnectionType {

    @SerializedName("Plaintext")
    PLAINTEXT,

    @SerializedName("ServerSideTLS")
    SSL,

    @SerializedName("MutualTLS")
    MUTUAL
}
