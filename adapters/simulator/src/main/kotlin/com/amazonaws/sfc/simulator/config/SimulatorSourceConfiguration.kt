// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.simulator.config

import com.amazonaws.sfc.config.BaseConfiguration
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANNELS
import com.amazonaws.sfc.config.BaseSourceConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class SimulatorSourceConfiguration : BaseSourceConfiguration() {

    @SerializedName(CONFIG_CHANNELS)
    private var _channels = mapOf<String, SimulatorChannelConfiguration>()
    val channels: Map<String, SimulatorChannelConfiguration>
        get() = _channels.filter { !it.key.startsWith(BaseConfiguration.CONFIG_DISABLED_COMMENT) }


    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return

        super.validate()
        validateAtLeastOneChannel()
        channels.values.forEach { channel ->
            channel.validate()
        }

        validated = true
    }

    private fun validateAtLeastOneChannel() =
        ConfigurationException.check(
            (channels.isNotEmpty()),
            "Simulator source must have 1 or more channels",
            CONFIG_CHANNELS,
            this
        )

    companion object {


        private val default = SimulatorSourceConfiguration()

        fun create(channels: Map<String, SimulatorChannelConfiguration> = default._channels,
                   name: String = default._name,
                   description: String = default._description,
                   protocolAdapter: String? = default._protocolAdapterID): SimulatorSourceConfiguration {

            val instance = createSourceConfiguration<SimulatorSourceConfiguration>(
                name = name,
                description = description,
                protocolAdapter = protocolAdapter)

            with(instance) {
                _channels = channels
            }
            return instance
        }
    }


}
