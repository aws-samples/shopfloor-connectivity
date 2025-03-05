
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data

import com.google.gson.annotations.SerializedName

enum class DataOutputType {
    @SerializedName("Json")
    JSON,
    @SerializedName("Protobuf")
    PROTOBUF ;
}



