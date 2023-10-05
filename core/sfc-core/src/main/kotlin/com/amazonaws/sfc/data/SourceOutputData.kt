/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.data

import com.amazonaws.sfc.config.ElementNamesConfiguration
import com.amazonaws.sfc.data.JmesPathExtended.escapeJMesString
import java.time.Instant

/**
 * Class to pass output data for a source
 * @property channels Mapping<String, ChannelOutputData> Values for the source
 * @property metadata Mapping<String, String>? Metadata for a source
 * @constructor
 */
data class SourceOutputData(
    val channels: Map<String, ChannelOutputData>,
    val timestamp: Instant? = null,
    val metadata: Map<String, String>? = null,
    val isAggregated: Boolean) {
    fun toMap(elementNames: ElementNamesConfiguration, jmesPathCompatibleNames: Boolean): Map<String, Any> {

        val map = mutableMapOf<String, Any>(
            elementNames.values to channels.map {
                val name = if (jmesPathCompatibleNames) escapeJMesString(it.key) else it.key
                name to it.value.toMap(elementNames, isAggregated)
            }.toMap()
        )
        if (timestamp != null) {
            map[elementNames.timestamp] = timestamp.toString()
        }
        if (metadata != null) {
            map[elementNames.metadata] = metadata
        }
        return map
    }
}