
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.s7.config

import com.google.gson.annotations.SerializedName
import org.apache.plc4x.java.s7.readwrite.types.S7ControllerType

enum class S7PlcControllerType {
    @SerializedName("S7-300")
    S7_300 {
        override val plx4jType: S7ControllerType
            get() = S7ControllerType.S7_300
    },

    @SerializedName("S7-1200")
    S7_1200 {
        override val plx4jType: S7ControllerType
            get() = S7ControllerType.S7_1200
    },

    @SerializedName("S7-1500")
    S7_1500 {
        override val plx4jType: S7ControllerType
            get() = S7ControllerType.S7_1500
    },

    @SerializedName(UNKNOWN)
    UNKNOWN_CONTROLLER_TYPE {
        override val plx4jType: S7ControllerType
            get() = S7ControllerType.ANY
    };

    abstract val plx4jType: S7ControllerType
    val controllerType: S7ControllerType
        get() = plx4jType

    override fun toString() = plx4jType.toString()

    companion object {
        const val UNKNOWN = "UNKNOWN"
    }
}

