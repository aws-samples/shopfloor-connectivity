/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.opcua.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.ProtocolAdapterConfiguration
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.amazonaws.sfc.opcua.config.OpcuaConfiguration.Companion.OPC_UA_ADAPTER
import com.amazonaws.sfc.opcua.config.OpcuaServerConfiguration.Companion.CONFIG_SERVER_PROFILE
import com.google.gson.annotations.SerializedName


@ConfigurationClass
class OpcuaAdapterConfiguration : ProtocolAdapterConfiguration() {

    @SerializedName(CONFIG_OPC_UA_SERVERS)
    private var _opcuaServers = mapOf<String, OpcuaServerConfiguration>()
    val opcuaServers: Map<String, OpcuaServerConfiguration>
        get() = _opcuaServers

    @SerializedName(CONFIG_SERVER_PROFILES)
    private var _serverProfiles = emptyMap<String, OpcuaServerProfileConfiguration>()
    val serverProfiles: Map<String, OpcuaServerProfileConfiguration>
        get() = _serverProfiles


    /**
     * Validates the configuration
     * @throws ConfigurationException
     */
    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return
        super.validate()

        ConfigurationException.check(
            opcuaServers.isNotEmpty(),
            "No $CONFIG_OPC_UA_SERVERS configured for adapter",
            CONFIG_OPC_UA_SERVERS,
            this
        )

        opcuaServers.values.forEach { it.validate() }
        serverProfiles.values.forEach { it.validate() }

        validateTypeOfServers()

        validated = true

    }

    private fun validateTypeOfServers() {
        opcuaServers
            .filter { server -> !server.value.serverProfile.isNullOrEmpty() }
            .forEach { (serverID, server) ->
                ConfigurationException.check(
                    serverProfiles.containsKey(server.serverProfile),
                    "Server profile \"${server.serverProfile}\" of OPCUA server \"$serverID\" is not configured for its OPCUA adapter, available profiles are ${serverProfiles.keys}",
                    CONFIG_SERVER_PROFILE,
                    this)
            }
    }

    companion object {
        const val CONFIG_OPC_UA_SERVERS = "OpcuaServers"
        const val CONFIG_SERVER_PROFILES = "ServerProfiles"

        private val default = OpcuaAdapterConfiguration()

        fun create(opcuaServers: Map<String, OpcuaServerConfiguration> = default._opcuaServers,
                   serverProfiles: Map<String, OpcuaServerProfileConfiguration> = default._serverProfiles,
                   description: String = default._description,
                   metrics: MetricsSourceConfiguration? = default._metrics,
                   adapterServer: String? = default._protocolAdapterServer): OpcuaAdapterConfiguration {

            val instance = createAdapterConfiguration<OpcuaAdapterConfiguration>(
                description = description,
                adapterType = OPC_UA_ADAPTER,
                metrics = metrics,
                adapterServer = adapterServer
            )

            with(instance) {
                _opcuaServers = opcuaServers
                _serverProfiles = serverProfiles
            }

            return instance
        }
    }
}

