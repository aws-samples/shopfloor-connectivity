// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.awss3tables.config

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class TablePartitionDeserializer(): JsonDeserializer<TablePartitionConfiguration> {

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): TablePartitionConfiguration? {

        return if (json == null || json.isJsonNull || !json.isJsonObject)
            null
        else
            TablePartitionConfiguration.fromJson(json.asJsonObject)
    }
}