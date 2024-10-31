// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuatarget.config


import com.amazonaws.sfc.opcuatarget.config.DataModelConfiguration.Companion.DEFAULT_SFC_DATA_MODEL
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

class DataModelConfigurationMap : NodeConfigurationMap<DataModelConfiguration>() {

    class ModelConfigurationMapDeserializer : JsonDeserializer<Map<String, DataModelConfiguration>> {
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): NodeConfigurationMap<DataModelConfiguration> {

            val jsonObject = json?.asJsonObject

            val dataModels = jsonObject?.keySet()?.associate { key ->
                key to DataModelConfiguration.fromJson(key, jsonObject[key] as JsonObject, context)
            }

            val dataModelNode = DataModelConfigurationMap()
            dataModelNode.nodes = dataModels
            return dataModelNode
        }

    }

    companion object{
        val DEFAULT_DATA_MODELS = DataModelConfigurationMap().apply {
            nodes = mapOf(DEFAULT_SFC_DATA_MODEL.id to DEFAULT_SFC_DATA_MODEL)
        }
    }

}

