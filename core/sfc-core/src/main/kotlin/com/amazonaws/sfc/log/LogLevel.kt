
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.log

import com.google.gson.annotations.SerializedName

/**
 * Log levels
 */
enum class LogLevel {

    /**
     * Always on for error messages
     */
    @SerializedName("Error")
    ERROR,

    /**
     * Warning + Error messages
     */
    @SerializedName("Warning")
    WARNING,

    /**
     * Warning + Error + Informational messages
     */
    @SerializedName("Info")
    INFO,

    /**
     * TRace + Warning + Error + Informational messages
     */
    @SerializedName("Trace")
    TRACE
}