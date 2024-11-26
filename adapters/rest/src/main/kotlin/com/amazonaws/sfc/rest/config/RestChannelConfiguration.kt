// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.rest.config

import com.amazonaws.sfc.config.ChannelConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.data.JmesPathExtended
import com.google.gson.annotations.SerializedName
import io.burt.jmespath.Expression


@ConfigurationClass
class RestChannelConfiguration : ChannelConfiguration() {

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

    @SerializedName(CONFIG_JSON)
    private var _isJson: Boolean = true
    val isJson: Boolean
        get() = _isJson

    override fun validate() {
        validateSelector()
        validated = true
    }

    private fun validateSelector() {
        if (!_selector.isNullOrEmpty()) {
            selector
            ConfigurationException.check(
                (error == null),
                "$CONFIG_SELECTOR \"$selectorStr\" for channel is not a valid JMESPath selector, $error",
                CONFIG_SELECTOR,
                this
            )
        }

        if (selector != null && !isJson) throw ConfigurationException(
            "$CONFIG_SELECTOR for channel can only be used for json data",
            CONFIG_SELECTOR,
            this
        )
    }


    companion object {

        const val CONFIG_SELECTOR = "Selector"
        const val CONFIG_JSON = "Json"

        val jmesPath by lazy {
            JmesPathExtended.create()
        }


        private val default = RestChannelConfiguration()

        fun create(name: String? = default._name,
                   description: String = default._description,
                   selector: String? = default._selector,
                   json: Boolean = default._isJson,
                   transformation: String? = default._transformationID,
                   metadata: Map<String, String> = default._metadata,
                   changeFilter: String? = default._changeFilterID,
                   valueFilter: String? = default._valueFilterID,
                   conditionFilter: String? = default._conditionFilterID): RestChannelConfiguration {

            val instance = createChannelConfiguration<RestChannelConfiguration>(
                name = name,
                description = description,
                transformation = transformation,
                metadata = metadata,
                changeFilter = changeFilter,
                valueFilter = valueFilter,
                conditionFilter = conditionFilter)

            with(instance) {
                _selector = selector
                _isJson = json
            }
            return instance
        }

    }


}








