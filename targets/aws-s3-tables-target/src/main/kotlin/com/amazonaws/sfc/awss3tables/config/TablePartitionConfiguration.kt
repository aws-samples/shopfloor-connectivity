// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.awss3tables.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject


@ConfigurationClass
class TablePartitionConfiguration(transforms: List<PartitionTransform>) {

    private var _transforms: List<PartitionTransform>? = transforms
    val transforms: List<PartitionTransform>
        get() = _transforms ?: emptyList()


    companion object {

        fun fromJson(jsonObject: JsonObject): TablePartitionConfiguration =

            TablePartitionConfiguration(
                jsonObject.keySet().map { key ->
                    PartitionTransform.of(key, jsonObject.get(key).asString)
                })
    }


}

