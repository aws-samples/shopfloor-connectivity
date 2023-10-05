/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.mqtt.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

@ConfigurationClass
class TopicNameMappingConfiguration : Validate {

    inner class Mapping(private val patternStr: String, private val replace: String) : Validate {
        private val pattern: Pattern by lazy {
            Pattern.compile(patternStr)
        }

        private val expression by lazy {
            pattern.toRegex()
        }

        fun matches(s: String) = pattern.matcher(s).find()

        fun replace(s: String) = expression.replace(s, replace)

        private var _validated = false
        override var validated
            get() = _validated
            set(value) {
                _validated = value
            }

        override fun validate() {
            if (validated) return

            try {
                pattern
                expression
            } catch (e: PatternSyntaxException) {
                throw ConfigurationException("Invalid pattern \"$patternStr\", ${e.description}", "Mappings", this)
            }
            validated = true
        }

    }

    @SerializedName(CONFIG_INCLUDE_UNMAPPED_TOPICS)
    private var _includeUnmappedTopics: Boolean = false

    val includeUnmappedTopics: Boolean
        get() = _includeUnmappedTopics

    @SerializedName(CONFIG_MAPPINGS)
    private var _mappings: Map<String, String> = emptyMap()


    private val mappings: List<Mapping> by lazy {
        _mappings.map { (pattern, replace) ->
            Mapping(pattern, replace)
        }.toList()
    }

    fun map(topicName: String): String? {
        val mapping = mappings.firstOrNull {
            it.matches(topicName)
        } ?: return if (includeUnmappedTopics)
            topicName
        else null
        return mapping.replace(topicName)
    }

    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    override fun validate() {

        if (validated) return
        mappings.forEach {
            it.validate()
        }
        validated = true
    }

    companion object {
        private const val CONFIG_INCLUDE_UNMAPPED_TOPICS = "IncludeUnmappedTopics"
        private const val CONFIG_MAPPINGS = "Mappings"

        private val default = TopicNameMappingConfiguration()

        fun create(includeUnmappedTopics: Boolean = default._includeUnmappedTopics,
                   mappings: Map<String, String> = default._mappings): TopicNameMappingConfiguration {

            val instance = TopicNameMappingConfiguration()
            with(instance) {
                _includeUnmappedTopics = includeUnmappedTopics
                _mappings = mappings
            }
            return instance
        }

    }
}


