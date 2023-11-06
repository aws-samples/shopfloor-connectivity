
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data

import com.amazonaws.sfc.config.ElementNamesConfiguration
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

/**
 * Custom JSON serializer for ChannelOutputData
 * @property targetData TargetData The data to serialize
 * @property elementNames ElementNamesConfiguration Configurable element names
 * @constructor
 */
internal class ChannelDataOutputSerializer(private val targetData: TargetData,
                                           private val elementNames: ElementNamesConfiguration) : JsonSerializer<ChannelOutputData> {
    override fun serialize(channelData: ChannelOutputData, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        val channelNode = JsonObject()

        if (channelData.value != null) {

            // The Value
            when (channelData.value) {
                // Most efficient way for types that have overloaded method for addProperty
                is Number -> channelNode.addProperty(elementNames.value, channelData.value)
                is String -> channelNode.addProperty(elementNames.value, channelData.value)
                is Boolean -> channelNode.addProperty(elementNames.value, channelData.value)
                is Char -> channelNode.addProperty(elementNames.value, channelData.value)
                // Use the custom serializer to serialize the value and add it as a JSON element
                else -> channelNode.add(elementNames.value, targetData.gson(elementNames).toJsonTree(channelData.value))
            }

            // Metadata
            addMetaDataNode(channelData.metadata, elementNames.metadata, channelNode)

            //Timestamp
            addTimestampNode(channelData.timestamp, elementNames.timestamp, channelNode)
        }
        return channelNode

    }
}