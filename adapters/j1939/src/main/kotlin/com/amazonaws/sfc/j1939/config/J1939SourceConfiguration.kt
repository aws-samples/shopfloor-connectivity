// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.j1939.config

import com.amazonaws.sfc.config.BaseConfiguration
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANNELS
import com.amazonaws.sfc.config.BaseSourceConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.j1939.config.J1939ChannelConfiguration.Companion.CONFIG_RAW_FORMAT
import com.amazonaws.sfc.j1939.protocol.getUByte
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class J1939SourceConfiguration : BaseSourceConfiguration() {

    @SerializedName(CONFIG_SOURCE_ADDRESS)
    private var _sourceAddress: Any? = null
    val sourceAddress : UByte? by lazy{
        if (_sourceAddress == null) return@lazy null
        getUByte(_sourceAddress.toString())
    }

    @SerializedName(CONFIG_ADAPTER_CAN_SOCKET)
    private var _adapterCanSocket: String? = null
    val adapterCanSocket: String?
        get() = _adapterCanSocket

    @SerializedName(CONFIG_CHANNELS)
    private var _channels = mapOf<String, J1939ChannelConfiguration>()
    val channels: Map<String, J1939ChannelConfiguration>
        get() = _channels.filter { !it.key.startsWith(BaseConfiguration.CONFIG_DISABLED_COMMENT) }


    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return

        super.validate()
        validateAtLeastOneChannel()
        channels.values.forEach { channel ->
            channel.validate()
        }

        if (_sourceAddress != null){
            try{
                _sourceAddress.toString()
            }catch ( _ : Exception){
                ConfigurationException( "$CONFIG_SOURCE_ADDRESS is not a valid value", CONFIG_SOURCE_ADDRESS, this)
            }
        }

        validated = true
    }

    @SerializedName(CONFIG_RAW_FORMAT)
    private var _rawFormat : J1939RawFormat? = null
    val rawFormat: J1939RawFormat?
        get() = _rawFormat



    private fun validateAtLeastOneChannel() =
        ConfigurationException.check(
            (channels.isNotEmpty()),
            "J1939 source must have 1 or more channels",
            CONFIG_CHANNELS,
            this
        )

    companion object {

        private val default = J1939SourceConfiguration()

        private const val CONFIG_SOURCE_ADDRESS = "SourceAddress"
        const val CONFIG_ADAPTER_CAN_SOCKET = "AdapterCanSocket"

        fun create(sourceAddress : Any? = default._sourceAddress,
                   channels: Map<String, J1939ChannelConfiguration> = default._channels,
                   rawFormat: J1939RawFormat? = default._rawFormat,
                   name: String = default._name,
                   description: String = default._description,
                   protocolAdapter: String? = default._protocolAdapterID): J1939SourceConfiguration {

            val instance = createSourceConfiguration<J1939SourceConfiguration>(
                name = name,
                description = description,
                protocolAdapter = protocolAdapter)

            with(instance) {
                _sourceAddress = sourceAddress
                _channels = channels
                _rawFormat = rawFormat
            }
            return instance
        }
    }


}
