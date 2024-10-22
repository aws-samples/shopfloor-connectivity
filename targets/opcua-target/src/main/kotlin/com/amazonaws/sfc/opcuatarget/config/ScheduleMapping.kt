// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuatarget.config

import com.google.gson.annotations.SerializedName

class ScheduleMapping : DataModelMappingBase() {

    @SerializedName("Sources")
    private val _sources: Map<String, SourceMapping> = emptyMap()
    val sources: Map<String, SourceMapping>
        get() = _sources


}