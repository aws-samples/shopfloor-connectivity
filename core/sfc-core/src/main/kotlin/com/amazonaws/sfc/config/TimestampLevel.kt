
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.config

import com.google.gson.annotations.SerializedName

/**
 * Timestamp level in output data
 */
enum class TimestampLevel {

    // No timestamps
    @SerializedName("None")
    NONE,

    // Time stamp per channel value -> stored under timestamp tag, value under value tag
    @SerializedName("Channel")
    CHANNEL,

    // Timestamp per source
    @SerializedName("Source")
    SOURCE,

    // Time stamp per source and per channel value -> stored under timestamp tag, value under value tag
    @SerializedName("Both")
    BOTH
}