/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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


