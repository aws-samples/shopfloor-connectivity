
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awssitewise

import com.google.gson.annotations.SerializedName
import software.amazon.awssdk.services.iotsitewise.model.PropertyDataType

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
    UNSPECIFIED;

    companion object {


        fun from(p : PropertyDataType): SiteWiseDataType {
            return when (p) {
                PropertyDataType.STRING -> STRING
                PropertyDataType.INTEGER -> INTEGER
                PropertyDataType.DOUBLE -> DOUBLE
                PropertyDataType.BOOLEAN -> BOOLEAN
                else -> {
                    STRING
                }
            }
        }

        fun fromValue(value: Any): SiteWiseDataType =
            when (value) {
                is String -> STRING
                is Boolean -> BOOLEAN
                is Byte -> INTEGER
                is Short -> INTEGER
                is Int -> INTEGER
                is Long -> INTEGER
                is UByte -> INTEGER
                is UShort -> INTEGER
                is UInt -> INTEGER
                is ULong -> INTEGER
                is Double -> DOUBLE
                is Float -> DOUBLE
                else -> STRING
            }
    }


}