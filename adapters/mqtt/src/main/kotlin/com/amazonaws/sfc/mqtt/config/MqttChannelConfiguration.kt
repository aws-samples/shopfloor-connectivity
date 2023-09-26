/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.mqtt.config

import com.amazonaws.sfc.config.ChannelConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.data.JmesPathExtended
import com.google.gson.annotations.SerializedName
import io.burt.jmespath.Expression


@ConfigurationClass
class MqttChannelConfiguration : ChannelConfiguration() {

    @SerializedName(CONFIG_TOPICS)
    private var _topics: Array<String> = emptyArray()

    /**
     * MQTT topics to subscribe to
     */
    val topics: Array<String>
        get() = _topics

    @SerializedName(CONFIG_JSON)
    private var _json: Boolean = true

    val json: Boolean
        get() = _json

    @SerializedName(CONFIG_TOPIC_NAME_MAPPING)
    private var _topicNameMapping: TopicNameMappingConfiguration? = null

    val topicNameMapping: TopicNameMappingConfiguration?
        get() = _topicNameMapping


    @SerializedName(CONFIG_SELECTOR)
    private var _selector: String? = null

    private var _expression: Expression<Any>? = null
    private var error: String? = null
    val selector: Expression<Any>?
        get() {

            return when {

                (_expression != null) -> _expression

                (_selector.isNullOrEmpty()) -> null

                (error != null) -> null

                (_expression == null) -> try {
                    _expression = jmesPath.compile(_selector)
                    _expression
                } catch (e: Throwable) {
                    error = e.message
                    null
                }

                else -> null
            }
        }

    val selectorStr
        get() = _selector

    override fun validate() {
        if (validated) return
        super.validate()
        validateHasTopics()
        validateSelector()
        validateNameOrMapping()
        validateNameOrTopicWildcards()
        validated = true
    }

    private fun validateSelector() {

        if (_selector.isNullOrBlank() && !json) {
            throw ConfigurationException(
                "$CONFIG_SELECTOR for a channel can only be used in if $CONFIG_JSON option is set to true",
                "Selector|Json",
                this
            )
        }

        if (!_selector.isNullOrEmpty()) {
            selector
            ConfigurationException.check(
                (error == null),
                "$CONFIG_SELECTOR \"$_selector\" for is not a valid JMESPath selector, $error",
                CONFIG_SELECTOR,
                this
            )
        }
    }

    private fun validateHasTopics() {
        ConfigurationException.check(
            (topics.any { it.isNotBlank() }),
            "MQTT channel must have 1 or more topics",
            CONFIG_TOPICS,
            this
        )
    }

    private fun validateNameOrTopicWildcards() {
        val topicsHaveWildcards = topics.any { "#" in it || "+" in it }
        ConfigurationException.check(
            !(name != null && topicsHaveWildcards),
            "Name can not be used if any of the topics contains a # or + wildcard",
            CONFIG_TOPICS,
            this
        )
    }

    private fun validateNameOrMapping() {
        ConfigurationException.check(
            (name == null && topicNameMapping != null) || (name != null && topicNameMapping == null),
            "Only Name and TopicNameMappingConfiguration can be used for a MQTT channel configuration",
            "Name|TopicNameMappingConfiguration",
            this
        )
    }

    fun mapTopicName(topicName: String): String? {
        if (name != null) {
            return name
        }
        return if (topicNameMapping == null) {
            topicName
        } else {
            val mappedName = topicNameMapping!!.map(topicName)
            return mappedName ?: if (topicNameMapping!!.includeUnmappedTopics) topicName else null
        }
    }

    companion object {

        private const val CONFIG_TOPICS = "Topics"
        private const val CONFIG_JSON = "Json"
        private const val CONFIG_TOPIC_NAME_MAPPING = "TopicNameMappingConfiguration"
        private const val CONFIG_SELECTOR = "Selector"

        val jmesPath by lazy {
            JmesPathExtended.create()
        }
        private val default = MqttChannelConfiguration()

        fun create(topics: Array<String> = default._topics,
                   json: Boolean = default._json,
                   topicNameMapping: TopicNameMappingConfiguration? = default._topicNameMapping,
                   selector: String? = default._selector,
                   name: String? = default._name,
                   description: String = default._description,
                   transformation: String? = default._transformationID,
                   metadata: Map<String, String> = default._metadata,
                   changeFilter: String? = default._changeFilterID,
                   valueFilter: String? = default._valueFilterID): MqttChannelConfiguration {

            val instance = createChannelConfiguration<MqttChannelConfiguration>(
                name = name,
                description = description,
                transformation = transformation,
                metadata = metadata,
                changeFilter = changeFilter,
                valueFilter = valueFilter
            )

            with(instance) {
                _topics = topics
                _json = json
                _topicNameMapping = topicNameMapping
                _selector = selector
            }
            return instance
        }

    }


}








