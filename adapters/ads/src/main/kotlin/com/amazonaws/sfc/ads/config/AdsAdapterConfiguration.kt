
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.ads.config

import com.amazonaws.sfc.ads.config.AdsConfiguration.Companion.ADS_ADAPTER
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class AdsAdapterConfiguration : ProtocolAdapterConfiguration(), Validate {

    @SerializedName(CONFIG_DEVICES)
    private var _devices = mapOf<String, AdsDeviceConfiguration>()

    val devices: Map<String, AdsDeviceConfiguration>
        get() = _devices

    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return
        super.validate()
        devices.values.forEach { it.validate() }
        validated = true

    }

    companion object {
        const val CONFIG_DEVICES = "Devices"

        private val default = AdsAdapterConfiguration()

        fun create(
            devices: Map<String, AdsDeviceConfiguration> = default._devices,
            description: String = default._description,
            metrics: MetricsSourceConfiguration? = default._metrics,
            adapterServer: String? = default._protocolAdapterServer
        ): AdsAdapterConfiguration {

            val instance = createAdapterConfiguration<AdsAdapterConfiguration>(
                description = description,
                adapterType = ADS_ADAPTER,
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


