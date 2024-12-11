// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.rest.config

import com.amazonaws.sfc.config.BaseConfiguration
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANNELS
import com.amazonaws.sfc.config.BaseSourceConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class RestSourceConfiguration : BaseSourceConfiguration() {

    @SerializedName(CONFIG_ADAPTER_SERVER)
    private var _adapterServerID: String = ""
    val adapterServerID: String
        get() = _adapterServerID

    @SerializedName(CONFIG_CHANNELS)
    private var _channels = mapOf<String, RestChannelConfiguration>()
    val channels: Map<String, RestChannelConfiguration>
        get() = _channels.filter { !it.key.startsWith(BaseConfiguration.CONFIG_DISABLED_COMMENT) }

    @SerializedName(CONFIG_REST_REQUEST)
    private val _restRequest: String = ""
    val restRequest: String
        get() = _restRequest


    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return
        super.validate()
        validateMustHaveServer()
        validateAtLeastOneChannel()
        channels.values.forEach { channel ->
            channel.validate()
        }

        validateMustRestRequest()

        validated = true
    }

    // Server must be set
    private fun validateMustHaveServer() =
        ConfigurationException.check(
            (_adapterServerID.isNotEmpty()),
            "$CONFIG_ADAPTER_SERVER for Rest source must be set",
            CONFIG_ADAPTER_SERVER,
            this
        )

    private fun validateMustRestRequest() =
        ConfigurationException.check(
            (_restRequest.isNotEmpty()),
            "$CONFIG_REST_REQUEST for REST source must be set",
            CONFIG_REST_REQUEST,
            this
        )

    private fun validateAtLeastOneChannel() =
        ConfigurationException.check(
            (channels.isNotEmpty()),
            "Rest source must have 1 or more channels",
            CONFIG_CHANNELS,
            this
        )


    companion object {
        const val CONFIG_ADAPTER_SERVER = "RestServer"
        private const val CONFIG_REST_REQUEST = "Request"

        private val default = RestSourceConfiguration()

        fun create(channels: Map<String, RestChannelConfiguration> = default._channels,
                   adapterServerID: String = default._adapterServerID,
                   name: String = default._name,
                   description: String = default._description,
                   protocolAdapter: String? = default._protocolAdapterID): RestSourceConfiguration {

            val instance = createSourceConfiguration<RestSourceConfiguration>(
                name = name,
                description = description,
                protocolAdapter = protocolAdapter)

            with(instance) {
                _adapterServerID = adapterServerID
                _channels = channels
            }
            return instance
        }
    }


}
