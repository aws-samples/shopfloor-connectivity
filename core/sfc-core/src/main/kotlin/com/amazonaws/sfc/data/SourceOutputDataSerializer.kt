/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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