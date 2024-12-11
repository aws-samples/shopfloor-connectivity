
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.nats.config

import com.amazonaws.sfc.config.ChannelConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.data.JmesPathExtended
import com.google.gson.annotations.SerializedName
import io.burt.jmespath.Expression


@ConfigurationClass
class NatsChannelConfiguration : ChannelConfiguration() {

    @SerializedName(CONFIG_SUBJECTS)
    private var _subjects: Array<String> = emptyArray()

    val subjects: Array<String>
        get() = _subjects

    @SerializedName(CONFIG_JSON)
    private var _json: Boolean = true

    val json: Boolean
        get() = _json

    @SerializedName(CONFIG_SUBJECT_NAME_MAPPING)
    private var _subjectNameMapping: SubjectNameMappingConfiguration? = null

    val subjectNameMapping: SubjectNameMappingConfiguration?
        get() = _subjectNameMapping


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
        validateHasSubjects()
        validateSelector()
        validateNameOrMapping()
        validateNameOrSubjectWildcards()
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

    private fun validateHasSubjects() {
        ConfigurationException.check(
            (subjects.isNotEmpty() && subjects.any { it.isNotBlank() }),
            "NATS channel must have 1 or more subjects",
            CONFIG_SUBJECTS,
            this
        )
    }

    private fun validateNameOrSubjectWildcards() {
        val subjectsHaveWildcards = subjects.any { "*" in it || ">" in it }
        ConfigurationException.check(
            !(name != null && subjectsHaveWildcards),
            "Name can not be used if any of the subjects contains a * or > wildcard",
            CONFIG_SUBJECTS,
            this
        )
    }

    private fun validateNameOrMapping() {
        ConfigurationException.check(
            (name == null && subjectNameMapping != null) || (name != null && subjectNameMapping == null),
            "Only Name OR SubjectNameMapping can be used for a NATS channel configuration",
            "Name|SubjectNameMappingConfiguration",
            this
        )
    }

    companion object {

        private const val CONFIG_SUBJECTS = "Subjects"
        private const val CONFIG_JSON = "Json"
        private const val CONFIG_SUBJECT_NAME_MAPPING = "SubjectNameMappingConfiguration"
        private const val CONFIG_SELECTOR = "Selector"

        val jmesPath by lazy {
            JmesPathExtended.create()
        }
        private val default = NatsChannelConfiguration()

        fun create(subjects: Array<String> = default._subjects,
                   json: Boolean = default._json,
                   subjectNameMapping: SubjectNameMappingConfiguration? = default._subjectNameMapping,
                   selector: String? = default._selector,
                   name: String? = default._name,
                   description: String = default._description,
                   transformation: String? = default._transformationID,
                   metadata: Map<String, String> = default._metadata,
                   changeFilter: String? = default._changeFilterID,
                   valueFilter: String? = default._valueFilterID,
                   conditionFilter : String? = default._conditionFilterID): NatsChannelConfiguration {

            val instance = createChannelConfiguration<NatsChannelConfiguration>(
                name = name,
                description = description,
                transformation = transformation,
                metadata = metadata,
                changeFilter = changeFilter,
                valueFilter = valueFilter,
                conditionFilter = conditionFilter
            )

            with(instance) {
                _subjects = subjects
                _json = json
                _subjectNameMapping = subjectNameMapping
                _selector = selector
            }
            return instance
        }

    }
}


fun NatsChannelConfiguration.mapSubjectName(subjectName: String): String? {
    if (name != null) {
        return name
    }
    return if (subjectNameMapping == null) {
        subjectName
    } else {
        val mappedName = subjectNameMapping!!.map(subjectName)
        return mappedName ?: if (subjectNameMapping!!.includeUnmappedSubjects) subjectName else null
    }
}







