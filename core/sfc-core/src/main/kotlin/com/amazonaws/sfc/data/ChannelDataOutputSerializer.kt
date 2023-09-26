/*
 Copyright (c) 2022. Amazon.com, Inc. or its affiliates. All Rights Reserved.
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