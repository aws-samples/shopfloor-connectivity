
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.config

import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANGE_FILTER
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANNELS
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_META_DATA
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_NAME
import com.google.gson.annotations.SerializedName

/**
 * Source configuration with attributes for a source as used by the SFC core controller
 */
@ConfigurationClass
class SourceConfiguration : BaseSourceConfiguration() {

    @SerializedName(CONFIG_CHANGE_FILTER)
    private var _changeFilterID: String? = null
    val changeFilterID: String?
        get() = _changeFilterID

    @SerializedName(CONFIG_CHANNELS)
    private var _channels = mapOf<String, ChannelConfiguration>()

    /**
     * Channels to read from a source
     * @see ChannelConfiguration
     */
    val channels: Map<String, ChannelConfiguration> by lazy {
        _channels.filter {
            !it.key.startsWith(BaseConfiguration.CONFIG_DISABLED_COMMENT)
        }
    }

    @SerializedName(CONFIG_META_DATA)
    private var _metadata = emptyMap<String, String>()

    /**
     * Metadata, which are constant values, that will be added as constant values for a source
     */
    val metadata: Map<String, String>
        get() = _metadata


    /**
     * Validates configuration
     * @throws ConfigurationException
     */
    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return
        super.validate()
        validateMustHaveChannels()
        validateUniqueChannelNames()
        validated = true

    }

    private fun validateUniqueChannelNames() {

        if (channels.any { !it.value.name.isNullOrBlank() }) {

            val duplicateChannelNames = channels.map {
                val name = it.value.name
                if (name.isNullOrBlank()) it.key else name
            }.groupingBy { it }.eachCount().filter { it.value > 1 }

            ConfigurationException.check(
                duplicateChannelNames.isEmpty(),
                "Duplicate channel names found, ${duplicateChannelNames.keys}",
                CONFIG_NAME,
                channels.filter { it.value.name in duplicateChannelNames.keys }
            )
        }

    }

    // Check if a source has at least 1 channel
    private fun validateMustHaveChannels() =
        ConfigurationException.check(
            channels.isNotEmpty(),
            "Source must have 1 or more channels",
            CONFIG_CHANNELS,
            this
        )

    companion object {
        private val default = SourceConfiguration()

        fun create(name: String = default._name,
                   description: String = default._description,
                   protocolAdapter: String? = default._protocolAdapterID): SourceConfiguration {

            val instance = SourceConfiguration()

            with(instance) {
                _name = name
                _description = description
                _protocolAdapterID = protocolAdapter
            }
            return instance
        }

    }


}