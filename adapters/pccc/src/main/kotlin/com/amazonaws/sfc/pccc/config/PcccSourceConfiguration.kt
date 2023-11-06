
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.pccc.config

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANNELS
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class PcccSourceConfiguration : BaseSourceConfiguration() {

    @SerializedName(CONFIG_ADAPTER_CONTROLLER)
    private var _adapterControllerID: String = ""
    val adapterControllerID: String
        get() = _adapterControllerID

    @SerializedName(CONFIG_CHANNELS)
    private var _channels = mapOf<String, PcccChannelConfiguration>()
    val channels: Map<String, PcccChannelConfiguration>
        get() = _channels.filter { !it.key.startsWith(BaseConfiguration.CONFIG_DISABLED_COMMENT) }


    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return

        super.validate()
        validateMustHaveController()
        validateAtLeastOneChannel()
        channels.values.forEach { channel ->
            channel.validate()
        }

        validated = true
    }

    // Server must be set
    private fun validateMustHaveController() =
        ConfigurationException.check(
            (_adapterControllerID.isNotEmpty()),
            "$CONFIG_ADAPTER_CONTROLLER for PCCC source must be set",
            CONFIG_ADAPTER_CONTROLLER,
            this
        )

    private fun validateAtLeastOneChannel() =
        ConfigurationException.check(
            (channels.isNotEmpty()),
            "PCCC source must have 1 or more channels",
            CONFIG_CHANNELS,
            this
        )

    companion object {
        const val CONFIG_ADAPTER_CONTROLLER = "AdapterController"

        private val default = PcccSourceConfiguration()

        fun create(
            channels: Map<String, PcccChannelConfiguration> = default._channels,
            adapterControllerID: String = default._adapterControllerID,
            name: String = default._name,
            description: String = default._description,
            protocolAdapter: String? = default._protocolAdapterID
        ): PcccSourceConfiguration {

            val instance = createSourceConfiguration<PcccSourceConfiguration>(
                name = name,
                description = description,
                protocolAdapter = protocolAdapter
            )

            with(instance) {
                _adapterControllerID = adapterControllerID
                _channels = channels
            }
            return instance
        }
    }

}
