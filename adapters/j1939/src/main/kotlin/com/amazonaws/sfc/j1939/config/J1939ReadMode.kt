// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.j1939.config

import com.google.gson.annotations.SerializedName

enum class J1939ReadMode {

    @SerializedName("KeepLast")
    KEEP_LAST,

    @SerializedName("KeepAll")
    KEEP_ALL
}


