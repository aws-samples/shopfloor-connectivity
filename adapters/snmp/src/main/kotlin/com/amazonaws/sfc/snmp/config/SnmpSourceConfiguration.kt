
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.snmp.config

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANNELS
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class SnmpSourceConfiguration : BaseSourceConfiguration() {

    @SerializedName(CONFIG_SOURCE_ADAPTER_SNMP_DEVICE)
    private var _sourceAdapterDeviceID: String = ""

    /**
     * ID of the device for the adapter device
     */
    val sourceAdapterDeviceID: String
        get() = _sourceAdapterDeviceID

    @SerializedName(CONFIG_CHANNELS)
    private var _channels = mapOf<String, SnmpChannelConfiguration>()
    val channels: Map<String, SnmpChannelConfiguration>
        get() = _channels.filter { !it.key.startsWith(BaseConfiguration.CONFIG_DISABLED_COMMENT) }

    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return

        super.validate()
        validateMustHaveAdapterDevice()
        validateAtLeastOneChannel()
        channels.values.forEach { channel ->
            channel.validate()
        }

        validated = true
    }

    // Device must be set
    private fun validateMustHaveAdapterDevice() =
        ConfigurationException.check(
            (sourceAdapterDeviceID.isNotEmpty()),
            "$$CONFIG_SOURCE_ADAPTER_SNMP_DEVICE for SNMP source must be set",
            CONFIG_SOURCE_ADAPTER_SNMP_DEVICE,
            this
        )

    private fun validateAtLeastOneChannel() =
        ConfigurationException.check(
            (channels.isNotEmpty()),
            "SNMP source must have 1 or more channels",
            CONFIG_CHANNELS,
            this
        )

    companion object {
        private const val CONFIG_SOURCE_ADAPTER_SNMP_DEVICE = "AdapterDevice"

        private val default = SnmpSourceConfiguration()

        fun create(adapterDevice: String = default._sourceAdapterDeviceID,
                   channels: Map<String, SnmpChannelConfiguration> = default._channels,
                   name: String = default._name,
                   description: String = default._description,
                   protocolAdapter: String? = default._protocolAdapterID): SnmpSourceConfiguration {

            val instance = createSourceConfiguration<SnmpSourceConfiguration>(
                name = name,
                description = description,
                protocolAdapter = protocolAdapter)
            with(instance) {
                _sourceAdapterDeviceID = adapterDevice
                _channels = channels
            }
            return instance
        }


    }


}
