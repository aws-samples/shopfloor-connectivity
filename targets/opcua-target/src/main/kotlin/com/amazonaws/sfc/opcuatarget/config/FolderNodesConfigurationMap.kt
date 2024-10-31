// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuatarget.config

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

class FolderNodesConfigurationMap : NodeConfigurationMap<FolderNodeConfiguration>() {

    class FolderNodesDeserializer : JsonDeserializer<Map<String, FolderNodeConfiguration>> {
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): NodeConfigurationMap<FolderNodeConfiguration> {

            val jsonObject = json?.asJsonObject

            val nodes = jsonObject?.keySet()?.associate { key ->
                key to FolderNodeConfiguration.fromJson(key, jsonObject.getAsJsonObject()[key] as JsonObject, context)
            }

            val folderNode = FolderNodesConfigurationMap()
            folderNode.nodes = nodes ?: emptyMap()
            return folderNode
        }


    }
}