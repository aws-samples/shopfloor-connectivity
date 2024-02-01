// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.config

import com.google.gson.annotations.SerializedName

// Enum used to specify which node types must be discovered
enum class DiscoveredNodeTypes {
    @SerializedName(VARIABLES)
    Variables,

    @SerializedName(EVENTS)
    Events,

    @SerializedName(VARIABLES_AND_EVENTS)
    VariablesAndEvents;

    companion object {
        const val VARIABLES = "Variables"
        const val EVENTS = "Events"
        const val VARIABLES_AND_EVENTS = "VariablesAndEvents"
    }


}
