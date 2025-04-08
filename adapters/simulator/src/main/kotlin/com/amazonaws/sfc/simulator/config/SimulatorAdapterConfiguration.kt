// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.simulator.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.ProtocolAdapterConfiguration
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration

import com.amazonaws.sfc.simulator.config.SimulatorConfiguration.Companion.SIMULATOR_ADAPTER

@ConfigurationClass
class SimulatorAdapterConfiguration : ProtocolAdapterConfiguration(), Validate {


    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return
        super.validate()
        validated = true
    }

    companion object {

        private val default = SimulatorAdapterConfiguration()

        fun create(description: String = default._description,
                   metrics: MetricsSourceConfiguration? = default._metrics,
                   adapterServer: String? = default._protocolAdapterServer): SimulatorAdapterConfiguration {

            val instance = createAdapterConfiguration<SimulatorAdapterConfiguration>(
                description = description,
                adapterType = SIMULATOR_ADAPTER,
                metrics = metrics,
                adapterServer = adapterServer)

            return instance
        }


    }

}


