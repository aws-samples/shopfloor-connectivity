// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuatarget.config

import com.google.gson.annotations.SerializedName

class SourceMapping : DataModelMappingBase(){

    @SerializedName("Channels")
    private val _channels: Map<String, DataModelMappingBase> = emptyMap()
    val channels:  Map<String, DataModelMappingBase>
        get() = _channels

}