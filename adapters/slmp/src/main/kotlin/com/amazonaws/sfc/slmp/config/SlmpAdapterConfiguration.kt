
/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 */


package com.amazonaws.sfc.slmp.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.ProtocolAdapterConfiguration
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.amazonaws.sfc.slmp.config.SlmpConfiguration.Companion.SLMP_ADAPTER
import com.amazonaws.sfc.slmp.protocol.SlmpStructureType
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class SlmpAdapterConfiguration : ProtocolAdapterConfiguration(), Validate {

    @SerializedName(CONFIG_CONTROLLERS)
    private var _controllers = mapOf<String, SlmpControllerConfiguration>()

    val controllers: Map<String, SlmpControllerConfiguration>
        get() = _controllers

    @SerializedName(CONFIG_STRUCTURES)
    private var _structures : Map<String, Map<String, String>> = emptyMap()

    val structures: Map<String, SlmpStructureType> by lazy {
        _structures.map { struct ->
            val structureType = SlmpStructureType.fromString(struct.value)
            SlmpStructureType.registerStructureType(struct.key, structureType )
            struct.key to structureType
        }.toMap()
    }

    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return
        super.validate()

        controllers.values.forEach { it.validate() }

        try{
            structures
        }catch( e : Exception){
            throw ConfigurationException("Error in $CONFIG_STRUCTURES configuration of adapter, $e", CONFIG_STRUCTURES, _structures)
        }


        validated = true
    }

    companion object {
        const val CONFIG_CONTROLLERS = "Controllers"
        const val CONFIG_STRUCTURES = "Structures"

        private val default = SlmpAdapterConfiguration()

        fun create(
            controllers: Map<String, SlmpControllerConfiguration> = default._controllers,
            structures: Map<String, Map<String, String>> = default._structures,
            description: String = default._description,
            metrics: MetricsSourceConfiguration? = default._metrics,
            adapterServer: String? = default._protocolAdapterServer
        ): SlmpAdapterConfiguration {

            val instance = createAdapterConfiguration<SlmpAdapterConfiguration>(
                description = description,
                adapterType = SLMP_ADAPTER,
                metrics = metrics,
                adapterServer = adapterServer
            )

            with(instance) {
                _controllers = controllers
                _structures = structures
            }
            return instance
        }

    }
}


