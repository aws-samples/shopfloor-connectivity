package com.amazonaws.sfc.config

import com.google.gson.annotations.SerializedName

enum class Connection {

    @SerializedName("Plaintext")
    PLAINTEXT,

    @SerializedName("ServerSideTLS")
    SSL,

    @SerializedName("MutualTLS")
    MUTUAL
}
