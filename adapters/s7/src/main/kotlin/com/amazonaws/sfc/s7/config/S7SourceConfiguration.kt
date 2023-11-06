
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.s7.config

import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANNELS
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_DISABLED_COMMENT
import com.amazonaws.sfc.config.BaseSourceConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class S7SourceConfiguration : BaseSourceConfiguration() {

    @SerializedName(CONFIG_SOURCE_ADAPTER_CONTROLLER)
    private var _sourceAdapterControllerID: String = ""

    /**
     * ID of the S7 controller in the adapter device
     */
    val sourceAdapterControllerID: String
        get() = _sourceAdapterControllerID

    @SerializedName(CONFIG_CHANNELS)
    private var _channels = mapOf<String, S7FieldChannelConfiguration>()
    val channels: Map<String, S7FieldChannelConfiguration>
        get() = _channels.filter { !it.key.startsWith(CONFIG_DISABLED_COMMENT) }

    override fun validate() {
        if (validated) return
        super.validate()
        channels.values.forEach {
            it.validate()
        }
        validated = true

    }


    companion object {
        const val CONFIG_SOURCE_ADAPTER_CONTROLLER = "AdapterController"

        private val default = S7SourceConfiguration()

        fun create(adapterController: String = default._sourceAdapterControllerID,
                   channels: Map<String, S7FieldChannelConfiguration> = default._channels,
                   name: String = default._name,
                   description: String = default._description,
                   protocolAdapter: String? = default._protocolAdapterID): S7SourceConfiguration {

            val instance = createSourceConfiguration<S7SourceConfiguration>(
                name = name,
                description = description,
                protocolAdapter = protocolAdapter)

            with(instance) {
                _sourceAdapterControllerID = adapterController
                _channels = channels
            }
            return instance
        }

    }
}