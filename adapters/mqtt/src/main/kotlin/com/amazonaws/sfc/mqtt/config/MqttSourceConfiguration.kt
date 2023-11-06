
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.mqtt.config

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANNELS
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class MqttSourceConfiguration : BaseSourceConfiguration() {

    @SerializedName(CONFIG_SOURCE_ADAPTER_BROKER)
    private var _sourceAdapterbrokerID: String = ""

    /**
     * ID of the opcua server in the adapter device
     */
    val sourceAdapterbrokerID: String
        get() = _sourceAdapterbrokerID

    @SerializedName(CONFIG_CHANNELS)
    private var _channels = mapOf<String, MqttChannelConfiguration>()
    val channels: Map<String, MqttChannelConfiguration>
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
            (sourceAdapterbrokerID.isNotEmpty()),
            "$CONFIG_SOURCE_ADAPTER_BROKER for MQTT source must be set",
            CONFIG_SOURCE_ADAPTER_BROKER,
            this
        )

    private fun validateAtLeastOneChannel() =
        ConfigurationException.check(
            (channels.isNotEmpty()),
            "MQTT source must have 1 or more channels",
            CONFIG_CHANNELS,
            this
        )

    companion object {
        private const val CONFIG_SOURCE_ADAPTER_BROKER = "AdapterBroker"

        private val default = MqttSourceConfiguration()

        fun create(channels: Map<String, MqttChannelConfiguration> = default._channels,
                   adapterBroker: String = default._sourceAdapterbrokerID,
                   name: String = default._name,
                   description: String = default._description,
                   protocolAdapter: String? = default._protocolAdapterID): MqttSourceConfiguration {

            val instance = createSourceConfiguration<MqttSourceConfiguration>(
                name = name,
                description = description,
                protocolAdapter = protocolAdapter)

            with(instance) {
                _sourceAdapterbrokerID = adapterBroker
                _channels = channels
            }
            return instance
        }
    }


}
