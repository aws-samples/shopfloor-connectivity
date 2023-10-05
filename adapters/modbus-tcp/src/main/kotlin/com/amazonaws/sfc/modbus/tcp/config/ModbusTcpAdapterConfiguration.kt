/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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
