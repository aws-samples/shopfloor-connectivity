
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data

import com.amazonaws.sfc.config.ElementNamesConfiguration
import com.amazonaws.sfc.system.DateTime
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.time.Instant

data class TargetData(val schedule: String,
                      val sources: Map<String, SourceOutputData>,
                      val metadata: Map<String, String>,
                      val serial: String,
                      var noBuffering: Boolean,
                      val timestamp: Instant = DateTime.systemDateTimeUTC()) {

    /**
     * Converts the value to a map, used for JMESPath querying
     * @param elementNames ElementNamesConfiguration
     * @return Mapping<String, Any>
     */
    fun toMap(elementNames: ElementNamesConfiguration, jmesPathCompatibleKeys: Boolean): Map<String, Any> {

        return mapOf(
            elementNames.schedule to schedule,
            elementNames.sources to sources.map {
                val name = if (jmesPathCompatibleKeys) JmesPathExtended.escapeJMesString(it.key) else it.key
                name to it.value.toMap(elementNames, jmesPathCompatibleKeys)
            }.toMap(),
            elementNames.metadata to metadata,
            elementNames.timestamp to timestamp,
            elementNames.serial to serial
        )
    }

    /**
     * Instance of gson serializer that with additional type handlers to support serializing all used data types.
     */
    private var _gson: Gson? = null

    /**
     * Gson serializer with configurable element fields. Exposed as a public method in order to use the same instance for
     * custom serialization of nested types.
     * @param elementNames ElementNamesConfiguration? Configurable element names.
     * @return Gson instance
     */
    internal fun gson(elementNames: ElementNamesConfiguration? = null): Gson {
        var names = elementNames
        if (_gson == null) {
            if (names == null) {
                names = ElementNamesConfiguration.DEFAULT_TAG_NAMES
            }
            _gson = gsonInstance(names)
        }
        return _gson as Gson
    }

    /**
     * Converts target data to JSON, taking in account all custom types and custom serialization.
     * @param elementNames ElementNamesConfiguration Configurable element names
     * @return String Data as JSON
     */
    fun toJson(elementNames: ElementNamesConfiguration): String {
        return gson(elementNames).toJson(this)
    }

    /**
     * Serializer with all custom types and customized serializations
     * @param elementNames ElementNamesConfiguration
     * @return Gson
     */
    private fun gsonInstance(elementNames: ElementNamesConfiguration): Gson =
        JsonHelper.gsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .registerTypeAdapter(TargetData::class.java, TargetDataSerializer(elementNames))
            .registerTypeAdapter(SourceOutputData::class.java, SourceOutputDataSerializer(this, elementNames))
            .registerTypeAdapter(ChannelOutputData::class.java, ChannelDataOutputSerializer(this, elementNames))
            .create()


}

/**
 * Helper to add metadata to a node if it is available at that level
 * @param metadata Mapping<String, String>? Mapping with metadata
 * @param metadataElementName String Name of te metadata element
 * @param node JsonObject
 */
internal fun addMetaDataNode(metadata: Map<String, String>?, metadataElementName: String, node: JsonObject) {

    if (metadata.isNullOrEmpty()) {
        return
    }
    val metadataNode = JsonObject()
    metadata.forEach {
        metadataNode.addProperty(it.key, it.value)
    }

    node.add(metadataElementName, metadataNode)

}

/**
 * Helper to add timestamp data to a node if it ia available
 * @param timestamp Instant? Timestamp
 * @param timestampElementName String Name of the timestamp element
 * @param node JsonObject
 */
internal fun addTimestampNode(timestamp: Instant?, timestampElementName: String, node: JsonObject) {
    if (timestamp != null) {
        node.addProperty(timestampElementName, timestamp.toString())
    }
}