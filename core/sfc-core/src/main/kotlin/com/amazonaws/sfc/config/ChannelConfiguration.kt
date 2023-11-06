/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.config

import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANGE_FILTER
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_DESCRIPTION
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_NAME
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_VALUE_FILTER
import com.google.gson.annotations.SerializedName

/**
 * Base class with minimum set of attributes for a channel of a source.
 * A channel is an abstraction of an input address for a value in a source
 */
@Suppress("unused")
@ConfigurationClass
open class ChannelConfiguration : Validate {

    @SerializedName(CONFIG_NAME)
    @Suppress("PropertyName")
    protected var _name: String? = null

    /**
     * Name of the channel
     */
    val name: String?
        get() = _name

    @SerializedName(CONFIG_DESCRIPTION)
    @Suppress("PropertyName")
    protected var _description = ""

    /**
     *  Description of the channel
     */
    val description: String
        get() = _description

    @SerializedName(CONFIG_TRANSFORMATION)
    @Suppress("PropertyName")
    protected var _transformationID: String? = null

    /**
     * ID of the transformation to be applied to the channel value
     */
    val transformationID: String?
        get() = _transformationID

    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    @SerializedName(BaseConfiguration.CONFIG_META_DATA)
    @Suppress("PropertyName")
    protected var _metadata = emptyMap<String, String>()

    /**
     * Metadata, which are constant values, that will be added as constant values
     */
    val metadata: Map<String, String>
        get() = _metadata

    @SerializedName(CONFIG_CHANGE_FILTER)
    @Suppress("PropertyName")
    protected var _changeFilterID: String? = null
    val changeFilterID: String?
        get() = _changeFilterID

    @SerializedName(CONFIG_VALUE_FILTER)
    @Suppress("PropertyName")
    protected var _valueFilterID: String? = null
    val valueFilterID: String?
        get() = _valueFilterID


    /**
     * Validates the configuration
     * @throws ConfigurationException
     */
    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return
        // Add validations here, note that channels are checked in the context of the ControllerServiceConfiguration as well.
        validated = true
    }

    companion object {
        const val CHANNEL_SEPARATOR = '/'
        const val CONFIG_TRANSFORMATION = "Transformation"

        private val default = ChannelConfiguration()

        fun create(name: String? = default._name,
                   description: String = default._description,
                   transformation: String? = default._transformationID,
                   metadata: Map<String, String> = default._metadata,
                   changeFilter: String? = default._changeFilterID,
                   valueFilter: String? = default._valueFilterID): ChannelConfiguration = createChannelConfiguration(name = name,
            description = description,
            transformation = transformation,
            metadata = metadata,
            changeFilter = changeFilter,
            valueFilter = valueFilter)


        @JvmStatic
        protected inline fun <reified T : ChannelConfiguration> createChannelConfiguration(name: String?,
                                                                                           description: String,
                                                                                           transformation: String?,
                                                                                           metadata: Map<String, String>,
                                                                                           changeFilter: String?,
                                                                                           valueFilter: String?): T {

            val parameterLessConstructor = T::class.java.constructors.firstOrNull { it.parameters.isEmpty() }
            assert(parameterLessConstructor != null)
            val instance = parameterLessConstructor!!.newInstance() as T

            with(instance) {
                _name = name
                _description = description
                _transformationID = transformation
                _metadata = metadata
                _changeFilterID = changeFilter
                _valueFilterID = valueFilter
            }
            return instance

        }

    }

}

