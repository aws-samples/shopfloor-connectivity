package com.amazonaws.sfc.config

import com.google.gson.annotations.SerializedName

enum class DiscoveryNodeTypes {
    @SerializedName("Variables")
    Variables,
    @SerializedName("Events")
    Events,
    @SerializedName("VariablesAndEvents")
    VariablesAndEvents
}
