
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awssitewise

import com.google.gson.annotations.SerializedName

/**
 * Data types supported by SieWise
 */
enum class SiteWiseDataType {
    @SerializedName("string")
    STRING,

    @SerializedName("integer")
    INTEGER,

    @SerializedName("double")
    DOUBLE,

    @SerializedName("boolean")
    BOOLEAN,
    UNSPECIFIED
}