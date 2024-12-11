// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.rest.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.ProtocolAdapterConfiguration
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.amazonaws.sfc.rest.config.RestConfiguration.Companion.REST_ADAPTER
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class RestAdapterConfiguration : ProtocolAdapterConfiguration(), Validate {

    @SerializedName(CONFIG_SERVERS)
    private var _servers = mapOf<String, RestServerConfiguration>()

    val servers: Map<String, RestServerConfiguration>
        get() = _servers

    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return
        super.validate()
        servers.values.forEach { it.validate() }
        validated = true
    }

    companion object {
        const val CONFIG_SERVERS = "RestServers"
        private val default = RestAdapterConfiguration()

        fun create(servers: Map<String, RestServerConfiguration> = default._servers,
                   description: String = default._description,
                   metrics: MetricsSourceConfiguration? = default._metrics,
                   adapterServer: String? = default._protocolAdapterServer): RestAdapterConfiguration {

            val instance = createAdapterConfiguration<RestAdapterConfiguration>(
                description = description,
                adapterType = REST_ADAPTER,
                metrics = metrics,
                adapterServer = adapterServer)

            with(instance) {
                _servers = servers
            }
            return instance
        }


    }

}


