
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.snmp.config

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.amazonaws.sfc.snmp.config.SnmpConfiguration.Companion.SNMP_ADAPTER
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class SnmpAdapterConfiguration : ProtocolAdapterConfiguration(), Validate {

    @SerializedName(CONFIG_SNMP_DEVICES)
    private var _devices = mapOf<String, SnmpDeviceConfiguration>()

    val devices: Map<String, SnmpDeviceConfiguration>
        get() = _devices

    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return
        super.validate()
        super.validate()
        devices.values.forEach { it.validate() }
        validated = true

    }

    companion object {
        const val CONFIG_SNMP_DEVICES = "Devices"

        private val default = SnmpAdapterConfiguration()

        fun create(devices: Map<String, SnmpDeviceConfiguration> = default._devices,
                   description: String = default._description,
                   metrics: MetricsSourceConfiguration? = default._metrics,
                   adapterServer: String? = default._protocolAdapterServer): SnmpAdapterConfiguration {

            val instance = createAdapterConfiguration<SnmpAdapterConfiguration>(
                description = description,
                adapterType = SNMP_ADAPTER,
                metrics = metrics,
                adapterServer = adapterServer
            )

            with(instance) {
                _devices = devices
            }

            return instance
        }
    }

}


