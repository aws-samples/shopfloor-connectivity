
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.sql.config

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.amazonaws.sfc.sql.config.SqlConfiguration.Companion.SQL_ADAPTER
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class SqlAdapterConfiguration : ProtocolAdapterConfiguration(), Validate {

    @SerializedName(CONFIG_DB_SERVERS)
    private var _dbServers = mapOf<String, DbServerConfiguration>()

    val dbServers: Map<String, DbServerConfiguration>
        get() = _dbServers

    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return
        super.validate()
        dbServers.values.forEach { it.validate() }
        validated = true
    }

    companion object {
        const val CONFIG_DB_SERVERS = "DbServers"
        private val default = SqlAdapterConfiguration()

        fun create(dbServers: Map<String, DbServerConfiguration> = default._dbServers,
                   description: String = default._description,
                   metrics: MetricsSourceConfiguration? = default._metrics,
                   adapterServer: String? = default._protocolAdapterServer): SqlAdapterConfiguration {

            val instance = createAdapterConfiguration<SqlAdapterConfiguration>(
                description = description,
                adapterType = SQL_ADAPTER,
                metrics = metrics,
                adapterServer = adapterServer)

            with(instance) {
                _dbServers = dbServers
            }
            return instance
        }


    }

}


