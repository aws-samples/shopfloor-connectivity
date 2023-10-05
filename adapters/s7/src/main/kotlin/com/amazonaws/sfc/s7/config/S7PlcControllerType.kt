/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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

