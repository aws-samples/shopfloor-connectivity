
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.filters

import com.google.gson.annotations.SerializedName

enum class ChangeFilterType {

    // Filter value is absolute
    @SerializedName("Absolute")
    ABSOLUTE,

    // Filter value is percentage of last value
    @SerializedName("Percent")
    PERCENT,

    // Always pass if value has changed
    @SerializedName("Always")
    ALWAYS

}