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
 * Custom JSON serializer for TargetData type
 * @property elementNames ElementNamesConfiguration Configurable element names
 * @constructor
 */
internal class TargetDataSerializer(private val elementNames: ElementNamesConfiguration) : JsonSerializer<TargetData> {

    /**
     * Serializes target data
     * @param targetData TargetData
     * @param typeOfSrc Type
     * @param context JsonSerializationContext
     * @return JsonElement
     */
    override fun serialize(targetData: TargetData, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {

        val targetDataNode = JsonObject()
        // Schedule name
        targetDataNode.addProperty(elementNames.schedule, targetData.schedule)

        // Serial
        targetDataNode.addProperty(elementNames.serial, targetData.serial)

        // Processing Timestamp
        addTimestampNode(targetData.timestamp, elementNames.timestamp, targetDataNode)

        //Data for all sources
        addDataForSources(targetData, targetDataNode)

        // Metadata at target data top level
        addMetaDataNode(targetData.metadata, elementNames.metadata, targetDataNode)

        return targetDataNode
    }

    /**
     * Creates the "sources" element with a sub element for each source with the target data for that source
     * @param targetData TargetData
     * @param targetDataNode JsonObject
     */
    private fun addDataForSources(targetData: TargetData, targetDataNode: JsonObject) {
        if (targetData.sources.isNotEmpty()) {
            val sourceNode = JsonObject()
            targetData.sources.forEach {
                sourceNode.add(it.key, targetData.gson(elementNames).toJsonTree(it.value))
            }
            targetDataNode.add(elementNames.sources, sourceNode)
        }
    }

}