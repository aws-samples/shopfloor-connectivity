
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.tcp.config

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.amazonaws.sfc.modbus.tcp.config.ModbusTcpConfiguration.Companion.MODBUS_TCP_ADAPTER
import com.google.gson.annotations.SerializedName


@ConfigurationClass
class ModbusTcpAdapterConfiguration : ProtocolAdapterConfiguration(), Validate {

    @SerializedName(CONFIG_DEVICES)
    private var _devices = mapOf<String, ModbusTcpDeviceConfiguration>()

    val devices: Map<String, ModbusTcpDeviceConfiguration>
        get() = _devices

    /**
     * Validates the configuration
     * @throws ConfigurationException
     */
    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return

        devices.values.forEach { it.validate() }
        validated = true

    }

    companion object {
        const val CONFIG_DEVICES = "Devices"

        private val default = ModbusTcpAdapterConfiguration()

        fun create(devices: Map<String, ModbusTcpDeviceConfiguration> = default._devices,
                   description: String = default._description,
                   metrics: MetricsSourceConfiguration? = default._metrics,
                   adapterServer: String? = default._protocolAdapterServer): ModbusTcpAdapterConfiguration {

            val instance = createAdapterConfiguration<ModbusTcpAdapterConfiguration>(
                description = description,
                adapterType = MODBUS_TCP_ADAPTER,
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
