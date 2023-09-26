/*
 Copyright (c) 2022. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.aggregations

import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_TRANSFORMATIONS
import com.amazonaws.sfc.config.BaseConfiguration.Companion.WILD_CARD
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName

typealias AggregationChannelOutputTransformation = Map<String, String>
typealias AggregationSourceOutputTransformation = Map<String, AggregationChannelOutputTransformation>
typealias AggregationOutputTransformations = Map<String, AggregationSourceOutputTransformation>

typealias AggregationOutputValues = Map<String, Map<String, ArrayList<String>>>

/**
 * Data aggregation configuration
 */

@ConfigurationClass
class AggregationConfiguration : Validate {
    @SerializedName(CONFIG_AGGREGATION_SIZE)
    private var _size = DEFAULT_AGGREGATION_SIZE

    /**
     * Size of configuration.
     */
    val size: Int
        get() = _size

    @SerializedName(CONFIG_AGGREGATION_OUTPUT)
    private var _output: AggregationOutputValues = emptyMap()

    /**
     * Aggregations to be applied to values
     */
    val output by lazy {
        // build new nested maps there the keys will be converted to a list of strings
        _output.map { o ->
            o.key.split(",").map { it.trim() } to
                    o.value.map { c ->
                        c.key.split(",").map { it.trim() } to c.value
                    }.toMap()
        }.toMap()
    }

    @SerializedName(CONFIG_TRANSFORMATIONS)
    private var _transformations: AggregationOutputTransformations = emptyMap()


    /**
     * TransformationsDeserializer for aggregated outputs. Keys of this 3 level maps are lists of strings for sourceIDs, channelIDs or output names
     */
    val transformations by lazy {
        _transformations.map { source ->
            source.key.split(",").map { it.trim() } to
                    source.value.map { channel ->
                        channel.key.split(",").map { it.trim() } to
                                channel.value.map { output ->
                                    output.key.split(",").map { it.trim() } to output.value
                                }.toMap()
                    }.toMap()
        }.toMap()
    }


    /**
     * Returns names of aggregation outputs configured for a source/channel combination
     * @param sourceID String ID of the source
     * @param channelID String ID of the channel
     * @return Set<String> Set of aggregations to output for the source/channel
     * <p>
     * The following order is applied to map the source/channel to an aggregated output
     * <p>
     * - match for both sourceID and channelID
     * - match for sourceID and wildcard for channel
     * - wildcard for source and match for channelID
     * - wildcard for both sourceID and channelID
     *
     */
    fun aggregationOutputs(sourceID: String, channelID: String): Pair<Set<String>, String?> {

        if (output.isEmpty()) {
            return emptySet<String>() to null
        }

        val matchPatterns = listOf(
            "$sourceID$sep$channelID",   // match for both sourceID and channelID
            "$sourceID$sep$WILD_CARD",   // match for sourceID and wildcard for channel
            "$WILD_CARD$sep$channelID",  // wildcard for source and match for channelID
            "$WILD_CARD$sep$WILD_CARD"   // wildcard for both sourceID and channelID
        )  // wildcard for both sourceID and channelID

        fun matches(s: List<String>, sourceID: String) = (s.first() == WILD_CARD) || ((sourceID in s))

        // loop through source outputs with match on sourceID or wildcard
        output.filter { matches(it.key, sourceID) }.forEach { source ->
            // loop through channels for the source that match or wildcard
            source.value.filter { matches(it.key, channelID) }.forEach { channel ->
                matchPatterns.forEach {
                    source.key.forEach { sourceKey ->
                        channel.key.forEach { channelKey ->
                            if ("$sourceKey$sep$channelKey" == it)
                                return channel.value.toSet() to it
                        }
                    }
                }
                return emptySet<String>() to null
            }
        }
        return emptySet<String>() to null
    }


    /**
     * Returns the names of an output transformation configured for a source/channel/aggregation combination
     * @param sourceID String ID of the source
     * @param channelID String ID of the channel
     * @param output Name of the aggregated output
     * @return Set<String> Set of aggregations to output for the source/channel/aggregated output
     * <p>
     * The following order is applied to map the source/channel to an aggregated output
     * <p>
     * - match on source and channel, all outputs
     * - match on source and output any channel
     * - match on channel and output, any source
     * - match on source, all outputs for any channel
     * - match on channel, oll outputs for all sources
     * - match on output, for all sources and channels
     * - any output for all sources and channels
     */
    fun aggregationTransformation(sourceID: String, channelID: String, output: String): Pair<String?, String?> {
        if (transformations.isEmpty()) {
            return null to null
        }

        val sep = "|"

        val transformationMatchPatterns = listOf(

            // The order of these entries define the order in which matching patterns for the transformation
            "$sourceID$sep$channelID$sep$output",                      // full match

            "$sourceID$sep$channelID$sep$WILD_CARD",                   // match on source and channel, all outputs
            "$sourceID$sep$WILD_CARD$sep${output.lowercase()}",      // match on source and output any channel
            "$WILD_CARD$sep$channelID$sep${output.lowercase()}",     // match on channel and output, any source

            "$sourceID$sep$WILD_CARD$sep$WILD_CARD",                   // match on source, all outputs for any channel
            "$WILD_CARD$sep$channelID$sep$WILD_CARD",                  // match on channel, oll outputs for all sources
            "$WILD_CARD$sep$WILD_CARD$sep${output.lowercase()}",     // match on output, for all sources and channels

            "$WILD_CARD$sep$WILD_CARD$sep$WILD_CARD"                   // any output for all sources and channels
        )


        fun matchesKeys(keys: List<String>, sourceID: String) = keys.first() == WILD_CARD || sourceID in keys

        // loop through source outputs with match on sourceID or wildcard
        transformations.filter { src -> matchesKeys(src.key, sourceID) }.forEach { (sourceKeys, channels) ->
            // loop through channels for the source that match or wildcard
            channels.filter { ch -> matchesKeys(ch.key, channelID) }.forEach { (channelKeys, outputs) ->
                // loop through all outputs that match or wildcard
                outputs.filter { out -> matchesKeys(out.key, output) }.forEach { (outputKeys, outputTransformations) ->
                    // for every pattern build the pattern from source, channel and output and test for a match
                    transformationMatchPatterns.forEach {
                        sourceKeys.forEach { s ->
                            channelKeys.forEach { c ->
                                outputKeys.forEach { o ->
                                    if ("$s$sep$c$sep$o" == it) return outputTransformations to it
                                }
                            }
                        }
                    }
                }
            }
        }
        return null to null
    }

    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    /**
     * Validates the configuration
     * @throws ConfigurationException
     */
    override fun validate() {
        if (validated) return

        validateAggregationMinimum()
        validateHasOutputs()

        output.forEach { (sourceID, sourceOutput) ->
            validateSourceOutput(sourceOutput, sourceID)
        }

        _transformations.forEach { (sourceID, sourceTransformation) ->
            validateSourceTransformation(sourceTransformation, sourceID)
        }

        validated = true

    }

    // checks if a source or wildcard is available for the transformation
    private fun validateSourceTransformation(sourceTransformation: AggregationSourceOutputTransformation, sourceID: String) {

        ConfigurationException.check(
            sourceTransformation.isNotEmpty(),
            "TransformationsDeserializer for source $sourceID must have 1 or more channels or a wildcard",
            "Aggregation.Transformation",
            this
        )

        sourceTransformation.forEach { (channelID, channelTransformation) ->
            ConfigurationException.check(
                channelTransformation.isNotEmpty(),
                "TransformationsDeserializer for source ${sourceID}, channel $${channelID} must have 1 or outputs",
                "Aggregation.Output",
                this
            )
        }
    }

    // checks if a channel or wildcard is available for the transformation
    private fun validateSourceOutput(sourceOutput: Map<List<String>, ArrayList<String>>, sourceID: List<String>) {

        ConfigurationException.check(
            sourceOutput.isNotEmpty(), "Aggregation for source $sourceID must have 1 or more channels or a wildcard",
            "Aggregation.Output",
            this
        )

        sourceOutput.forEach { (channelID, channelOutput) ->
            ConfigurationException.check(
                channelOutput.isNotEmpty(),
                "Aggregation for source $sourceID, channel $channelID must have 1 or more aggregated values",
                "Aggregation.Output",
                this
            )


            channelOutput.forEach { output ->
                validateChannelWildcard(sourceID, channelID, channelOutput, output)
                validateOutputName(output, sourceID, channelID)
            }
        }
    }

    // checks is the output name is valid (or is a wildcard)
    private fun validateOutputName(output: String, sourceID: List<String>, channelID: List<String>) =
        ConfigurationException.check(
            ((output == WILD_CARD) || (Aggregator.aggregationOutputFunctionsNames.contains(output.lowercase()))),
            "Aggregation for source $sourceID, channel $channelID  \"$output\" is not a valid aggregated value",
            "Aggregation.Output",
            this
        )


    // wildcard output can not be used with explicit configured outputs
    private fun validateChannelWildcard(sourceID: List<String>, channelID: List<String>, channelOutput: ArrayList<String>, output: String) =
        ConfigurationException.check(
            ((output != WILD_CARD) || (channelOutput.count() == 1)),
            "Aggregation for source $sourceID, channel $channelID wildcard \"$output\" cannot be combined with other aggregated values",
            "Aggregation.Output",
            this
        )


    // validates if an aggregation has at least a single output
    private fun validateHasOutputs() =
        ConfigurationException.check(
            output.isNotEmpty(),
            "Aggregation must have 1 or more outputs",
            "Aggregation.Output",
            this
        )

    // validates aggregation size
    private fun validateAggregationMinimum() =
        ConfigurationException.check(
            (size >= 1),
            "Aggregation size must be greater than 1",
            "Aggregation.Size",
            this
        )

    /**
     * Aggregation as s string
     * @return String
     */
    override fun toString(): String {
        return "Aggregation($_output)"
    }

    companion object {
        // Available aggregations
        const val AVERAGE = "avg"
        const val COUNT = "count"
        const val FIRST = "first"
        const val LAST = "last"
        const val MAX = "max"
        const val MEDIAN = "median"
        const val MIN = "min"
        const val MODE = "mode"
        const val STDDEV = "stddev"
        const val SUM = "sum"
        const val VALUES = "values"

        const val sep = "|"

        const val CONFIG_AGGREGATION_SIZE = "Size"
        const val DEFAULT_AGGREGATION_SIZE = 1
        const val CONFIG_AGGREGATION_OUTPUT = "Output"

        private val default = AggregationConfiguration()

        fun create(size: Int = default._size,
                   output: AggregationOutputValues = default._output,
                   transformations: AggregationOutputTransformations = default._transformations): AggregationConfiguration {

            val instance = AggregationConfiguration()

            with(instance) {
                _size = size
                _output = output
                _transformations = transformations
            }
            return instance
        }


    }


}