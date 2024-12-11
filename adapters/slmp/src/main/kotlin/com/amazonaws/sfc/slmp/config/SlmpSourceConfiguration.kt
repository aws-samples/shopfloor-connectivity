/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 */


package com.amazonaws.sfc.slmp.config

import com.amazonaws.sfc.config.BaseConfiguration
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANNELS
import com.amazonaws.sfc.config.BaseSourceConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class SlmpSourceConfiguration : BaseSourceConfiguration() {

    @SerializedName(CONFIG_ADAPTER_CONTROLLER)
    private var _adapterControllerID: String = ""
    val adapterDeviceID: String
        get() = _adapterControllerID


    @SerializedName(CONFIG_CHANNELS)
    private var _channels = mapOf<String, SlmpChannelConfiguration>()
    val channels: Map<String, SlmpChannelConfiguration>
        get() = _channels.filter { !it.key.startsWith(BaseConfiguration.CONFIG_DISABLED_COMMENT) }


    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return

        super.validate()
        validateMustHaveDevice()

        validateAtLeastOneChannel()
        channels.values.forEach { channel ->
            channel.validate()
        }

        validated = true
    }


    private fun validateMustHaveDevice() =
        ConfigurationException.check(
            (_adapterControllerID.isNotEmpty()),
            "$CONFIG_ADAPTER_CONTROLLER for SLMP source must be set to a valid controller for the used SLMP adapter",
            CONFIG_ADAPTER_CONTROLLER,
            this
        )

    private fun validateAtLeastOneChannel() =
        ConfigurationException.check(
            (channels.isNotEmpty()),
            "SLMP source must have 1 or more channels",
            CONFIG_CHANNELS,
            this
        )


    companion object {
        const val CONFIG_ADAPTER_CONTROLLER = "AdapterController"

        private val default = SlmpSourceConfiguration()

        fun create(
            channels: Map<String, SlmpChannelConfiguration> = default._channels,
            adapterControllerID: String = default._adapterControllerID,
            name: String = default._name,
            description: String = default._description,
            protocolAdapter: String? = default._protocolAdapterID
        ): SlmpSourceConfiguration {


            val instance = createSourceConfiguration<SlmpSourceConfiguration>(
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
