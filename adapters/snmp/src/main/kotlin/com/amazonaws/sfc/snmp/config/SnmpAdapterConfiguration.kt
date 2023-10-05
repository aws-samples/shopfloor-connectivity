/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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


