
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.nats.config

import com.amazonaws.sfc.config.BaseConfiguration
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANNELS
import com.amazonaws.sfc.config.BaseSourceConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class NatsSourceConfiguration : BaseSourceConfiguration() {

    @SerializedName(CONFIG_SOURCE_ADAPTER_SERVER)
    private var _sourceAdapterServerID: String = ""

    val sourceAdapterServerID: String
        get() = _sourceAdapterServerID

    @SerializedName(CONFIG_CHANNELS)
    private var _channels = mapOf<String, NatsChannelConfiguration>()
    val channels: Map<String, NatsChannelConfiguration>
        get() = _channels.filter { !it.key.startsWith(BaseConfiguration.CONFIG_DISABLED_COMMENT) }


    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return

        super.validate()
        validateMustHaveAdapterBroker()
        validateAtLeastOneChannel()
        channels.values.forEach { channel ->
            channel.validate()
        }


        validated = true
    }

    // Device must be set
    private fun validateMustHaveAdapterBroker() =
        ConfigurationException.check(
            (sourceAdapterServerID.isNotEmpty()),
            "$CONFIG_SOURCE_ADAPTER_SERVER for NATS source must be set",
            CONFIG_SOURCE_ADAPTER_SERVER,
            this
        )

    private fun validateAtLeastOneChannel() =
        ConfigurationException.check(
            (channels.isNotEmpty()),
            "NATS source must have 1 or more channels",
            CONFIG_CHANNELS,
            this
        )

    companion object {
        private const val CONFIG_SOURCE_ADAPTER_SERVER = "AdapterServer"

        private val default = NatsSourceConfiguration()

        fun create(channels: Map<String, NatsChannelConfiguration> = default._channels,
                   adapterBroker: String = default._sourceAdapterServerID,
                   name: String = default._name,
                   description: String = default._description,
                   protocolAdapter: String? = default._protocolAdapterID): NatsSourceConfiguration {

            val instance = createSourceConfiguration<NatsSourceConfiguration>(
                name = name,
                description = description,
                protocolAdapter = protocolAdapter)

            with(instance) {
                _sourceAdapterServerID = adapterBroker
                _channels = channels
            }
            return instance
        }
    }


}
