// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuatarget.config

import com.google.gson.annotations.SerializedName

abstract class DataModelMappingBase{
    @SerializedName(CONFIG_MAPPING_ID)
    protected val _mappingID: String? = null
    val mappingID: String?
        get() = _mappingID

    @SerializedName(CONFIG_METADATA)
    private val _metadata: Map<String, DataModelMappingBase> = emptyMap()
    val metadata: Map<String, DataModelMappingBase>
        get() = _metadata

    companion object{
       const val CONFIG_MAPPING_ID ="MappingId"
        private const val CONFIG_METADATA = "Metadata"
    }

}