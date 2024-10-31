// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuatarget.config

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class VariableNodeMap: NodeConfigurationMap<VariableNodeConfiguration>(){

    class VariableNodesDeserializer : JsonDeserializer<Map<String, VariableNodeConfiguration>>{
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): NodeConfigurationMap<VariableNodeConfiguration> {

            val jsonObject = json?.asJsonObject
            val variables = jsonObject?.keySet()?.associate { key ->
                key to VariableNodeConfiguration.fromJson(key, jsonObject)
            }

            val f = VariableNodeMap()
            f.nodes = variables
            return f
        }
    }


}