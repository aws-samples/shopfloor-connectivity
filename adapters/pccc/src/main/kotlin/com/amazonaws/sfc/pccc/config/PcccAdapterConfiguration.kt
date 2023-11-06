
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.pccc.config

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.amazonaws.sfc.pccc.config.PcccConfiguration.Companion.PCCC_ADAPTER
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class PcccAdapterConfiguration : ProtocolAdapterConfiguration(), Validate {

    @SerializedName(CONFIG_CONTROLLERS)
    private var _controllers = mapOf<String, PcccControllerConfiguration>()

    val controllers: Map<String, PcccControllerConfiguration>
        get() = _controllers

    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return
        super.validate()
        controllers.values.forEach { it.validate() }
        validated = true

    }

    companion object {
        const val CONFIG_CONTROLLERS = "Controllers"

        private val default = PcccAdapterConfiguration()

        fun create(
            controllers: Map<String, PcccControllerConfiguration> = default._controllers,
            description: String = default._description,
            metrics: MetricsSourceConfiguration? = default._metrics,
            adapterServer: String? = default._protocolAdapterServer
        ): PcccAdapterConfiguration {

            val instance = createAdapterConfiguration<PcccAdapterConfiguration>(
                description = description,
                adapterType = PCCC_ADAPTER,
                metrics = metrics,
                adapterServer = adapterServer
            )

            with(instance) {
                _controllers = controllers
            }
            return instance
        }

    }
}


