
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
 * Custom serializer for target data for a single source
 * @property sourceTargetData TargetData Target data for the source
 * @property elementNames ElementNamesConfiguration Configurable element names
 * @constructor
 */
internal class SourceOutputDataSerializer(private val sourceTargetData: TargetData,
                                          private val elementNames: ElementNamesConfiguration) : JsonSerializer<SourceOutputData> {

    /**
     * Serializes the data to JSON
     * @param source SourceOutputData
     * @param typeOfSrc Type
     * @param context JsonSerializationContext
     * @return JsonElement
     */
    override fun serialize(source: SourceOutputData, typeOfSrc: Type, context: JsonSerializationContext?): JsonElement {

        val sourceNode = JsonObject()

        if (source.channels.isNotEmpty()) {
            addSourceChannelData(source.channels, sourceNode)

            // Timestamp and metadata only if there are any channels with data
            addMetaDataNode(source.metadata, elementNames.metadata, sourceNode)
            addTimestampNode(source.timestamp, elementNames.timestamp, sourceNode)
        }
        return sourceNode
    }

    /**
     * Creates the "values" node containing the elements with the target data for the channels of a source
     */
    private fun addSourceChannelData(channels: Map<String, ChannelOutputData>, sourceNode: JsonObject) {
        val valuesNode = JsonObject()
        channels.forEach { channel ->
            valuesNode.add(channel.key, sourceTargetData.gson(elementNames).toJsonTree(channel.value))
        }
        sourceNode.add(elementNames.values, valuesNode)
    }


}