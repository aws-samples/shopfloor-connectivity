
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data

import com.amazonaws.sfc.config.ElementNamesConfiguration
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.util.TemplateRenderer.containsPlaceHolders
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.time.Instant

/**
 * Represents target data containing schedule, sources, metadata, and timing information.
 *
 * @property schedule The schedule information for the target data
 * @property sources Map of source names to their corresponding output data
 * @property metadata Additional metadata key-value pairs
 * @property serial Serial identifier for the target data
 * @property noBuffering Flag indicating if buffering should be disabled
 * @property timestamp The timestamp when this target data was created (defaults to current UTC time)
 */
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
    fun  toMap(elementNames: ElementNamesConfiguration, jmesPathCompatibleKeys: Boolean): Map<String, Any> {

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
    internal fun gson(elementNames: ElementNamesConfiguration? = null, pretty : Boolean = true): Gson {
        var names = elementNames
        if (_gson == null) {
            if (names == null) {
                names = ElementNamesConfiguration.DEFAULT_TAG_NAMES
            }
            _gson = gsonInstance(names, pretty)
        }
        return _gson as Gson
    }

    /**
     * Converts target data to JSON, taking in account all custom types and custom serialization.
     * @param elementNames ElementNamesConfiguration Configurable element names
     * @return String Data as JSON
     */
    fun toJson(elementNames: ElementNamesConfiguration, trimQuotesForNumericValues : Boolean, pretty: Boolean = true): String {

        val s =  gson(elementNames, pretty).toJson(this)
        return if (trimQuotesForNumericValues) s.replace(numericValueRegex){m ->
            m.groups[1]?.value.toString().trim('"')
        } else s

    }

    /**
     * Serializer with all custom types and customized serializations
     * @param elementNames ElementNamesConfiguration
     * @return Gson
     */
    private fun gsonInstance(elementNames: ElementNamesConfiguration, pretty : Boolean = true): Gson {
        val builder = JsonHelper.gsonBuilder()
            .disableHtmlEscaping()

            .registerTypeAdapter(TargetData::class.java, TargetDataSerializer(elementNames))
            .registerTypeAdapter(SourceOutputData::class.java, SourceOutputDataSerializer(this, elementNames))
            .registerTypeAdapter(ChannelOutputData::class.java, ChannelDataOutputSerializer(this, elementNames))

        if (pretty) builder.setPrettyPrinting()

        return builder.create()
    }

    fun metaDataAtSourceLevel(source: String) = metadata + (sources[source]?.metadata ?: emptyMap())
    fun metaDataAtChannelLevel( source: String, channel: String) = (sources[source]?.channels?.get(channel)?.metadata ?: emptyMap())


    companion object{
       private val numericValueRegex = "\"(\\d+(\\.\\d*)?)\"".toRegex()
    }
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

typealias NameBuilderFunction = (TargetData, String, String, Map<String, String>) -> String

/**
 * Splits data by name, using a template to render the name
 * @param template String Template to render the name
 * @param fn NameBuilderFunction Function that will be used to render the name
 * @return Map<String, TargetData>
 */
fun TargetData.splitDataByName(template : String, fn : NameBuilderFunction): Map<String, TargetData> {

    val targetData = this@splitDataByName
    if (!containsPlaceHolders(template)) return mapOf(template to targetData)

    val namesMap = sequence {
        targetData.sources.forEach { (sourceName, sourceData) ->
            val sourceMetadata = targetData.metaDataAtSourceLevel(sourceName)
            sourceData.channels.map { channel ->
                val channelMetadata = targetData.metaDataAtChannelLevel(sourceName, channel.key) + sourceMetadata
                var subjectName = fn(targetData, sourceName, channel.key, channelMetadata)
                if (subjectName.isNotEmpty()) {
                    yield(subjectName to Pair(sourceName, channel))
                }
            }
        }
    }.toList().groupBy { subject -> subject.first }    // group by rendered subject name
        .map { (subjectName, subjectChannels) ->       // create map for subject names
            subjectName to subjectChannels.map { (_, subjectSources) ->
                subjectSources
            }.groupBy { source -> source.first }.map { (sourceName, channels) ->     // group by source
                sourceName to channels.map { (_, sourceChannels) -> sourceChannels } // creat map with channel in source
            }.toMap()
        }.toMap()


    val mappedTargetData = namesMap.map { (subject, subjectSources) ->
        subject to TargetData(
            targetData.schedule, sources =
                subjectSources.map { sourceChannels ->
                    val source = targetData.sources[sourceChannels.key]!!
                    sourceChannels.key to SourceOutputData(
                        channels = sourceChannels.value.associate { it.key to it.value },
                        timestamp = source.timestamp,
                        metadata = source.metadata,
                        isAggregated = source.isAggregated

                    )
                }.toMap(),
            metadata = targetData.metadata,
            serial = targetData.serial,
            noBuffering = targetData.noBuffering,
            timestamp = targetData.timestamp)

    }.toMap()

    return mappedTargetData
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