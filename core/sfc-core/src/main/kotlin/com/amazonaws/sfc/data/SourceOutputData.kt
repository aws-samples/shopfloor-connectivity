
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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