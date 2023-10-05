/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.opcua.config

import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.ScheduleConfiguration.Companion.CONFIG_SCHEDULE_SOURCES
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.opcua.config.OpcuaSourceConfiguration.Companion.CONFIG_SOURCE_ADAPTER_OPCUA_SERVER
import com.google.gson.annotations.SerializedName


@ConfigurationClass
class OpcuaConfiguration : SourceAdapterBaseConfiguration() {

    val protocolAdapters: Map<String, OpcuaAdapterConfiguration>
        get() = _protocolAdapters.filter { it.value.protocolAdapterType == OPC_UA_ADAPTER }

    @SerializedName(CONFIG_SOURCES)
    private var _sources = mapOf<String, OpcuaSourceConfiguration>()

    val sources: Map<String, OpcuaSourceConfiguration>
        get() = _sources.filter { it.value.protocolAdapterID in protocolAdapters.keys }


    @SerializedName(CONFIG_PROTOCOL_ADAPTERS)
    private var _protocolAdapters = mapOf<String, OpcuaAdapterConfiguration>()

    val opcuaProtocolAdapters
        get() = protocolAdapters.filter { it.value.protocolAdapterType == OPC_UA_ADAPTER }


    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return

        super.validate()
        validateAtLeastOneSource()
        schedules.forEach { schedule ->
            validateSchedule(schedule)
        }

        protocolAdapters.values.forEach { it.validate() }

        validated = true
    }

    private fun validateAtLeastOneSource() {
        ConfigurationException.check(
            sources.isNotEmpty(),
            "Configuration must have 1 or more sources",
            CONFIG_SCHEDULE_SOURCES
        )
    }

    private fun validateSchedule(schedule: ScheduleConfiguration) {
        schedule.sources.forEach { source ->
            validateScheduleSources(source, schedule)
        }
    }

    private fun validateScheduleSources(source: Map.Entry<String, ArrayList<String>>, schedule: ScheduleConfiguration) {
        val sourceConfig: OpcuaSourceConfiguration = this.sources[source.key] ?: return

        val adapterConfiguration: OpcuaAdapterConfiguration = protocolAdapters[sourceConfig.protocolAdapterID]!!
        ConfigurationException.check(
            (adapterConfiguration.opcuaServers[sourceConfig.sourceAdapterOpcuaServerID] != null),
            "Schedule \"${schedule.name}\" source \"${source.key}\" $CONFIG_SOURCE_ADAPTER_OPCUA_SERVER \"${sourceConfig.sourceAdapterOpcuaServerID}\"does not exist, available servers for adapter \"${sourceConfig.protocolAdapterID}  are ${adapterConfiguration.opcuaServers.keys}",
            CONFIG_SOURCE_ADAPTER_OPCUA_SERVER,
            sourceConfig
        )

        validateSourceChannels(source, sourceConfig, schedule)

    }

    private fun validateSourceChannels(
        sourceChannels: Map.Entry<String, ArrayList<String>>,
        source: OpcuaSourceConfiguration?,
        schedule: ScheduleConfiguration
    ) {
        sourceChannels.value.forEach { channelID ->

            ConfigurationException.check(
                ((channelID == WILD_CARD) || source!!.channels.containsKey(channelID)),
                "Channel \"$channelID\" for source \"${sourceChannels.key}\" in schedule \"${schedule.name}\" does not exist, existing channels are ${source!!.channels}",
                "Schedule.$CONFIG_SCHEDULE_SOURCES",
                schedule.sources
            )
        }
    }


    companion object {
        const val OPC_UA_ADAPTER = "OPCUA"
        private val default = OpcuaConfiguration()

        const val CONFIG_NODE_ID = "NodeId"
        const val CONFIG_PROPERTIES = "Properties"
        const val CONFIG_INHERITS_FROM = "Inherits"

        fun create(sources: Map<String, OpcuaSourceConfiguration> = default._sources,
                   protocolAdapters: Map<String, OpcuaAdapterConfiguration> = default._protocolAdapters,
                   name: String = default._name,
                   version: String = default._version,
                   awsVersion: String? = default._awsVersion,
                   description: String = default._description,
                   schedules: List<ScheduleConfiguration> = default._schedules,
                   logLevel: LogLevel? = default._logLevel,
                   metadata: Map<String, String> = default._metadata,
                   elementNames: ElementNamesConfiguration = default._elementNames,
                   targetServers: Map<String, ServerConfiguration> = default._targetServers,
                   targetTypes: Map<String, InProcessConfiguration> = default._targetTypes,
                   adapterServers: Map<String, ServerConfiguration> = default._protocolAdapterServers,
                   adapterTypes: Map<String, InProcessConfiguration> = default._protocolTypes,
                   awsIotCredentialProviderClients: Map<String, AwsIotCredentialProviderClientConfiguration> = default._awsIoTCredentialProviderClients,
                   secretsManager: SecretsManagerConfiguration? = default._secretsManagerConfiguration): OpcuaConfiguration {

            val instance = createBaseConfiguration<OpcuaConfiguration>(
                name = name,
                version = version,
                awsVersion = awsVersion,
                description = description,
                schedules = schedules,
                logLevel = logLevel,
                metadata = metadata,
                elementNames = elementNames,
                targetServers = targetServers,
                targetTypes = targetTypes,
                adapterServers = adapterServers,
                adapterTypes = adapterTypes,
                awsIotCredentialProviderClients = awsIotCredentialProviderClients,
                secretsManagerConfiguration = secretsManager
            )

            with(instance) {
                _sources = sources
                _protocolAdapters = protocolAdapters
            }
            return instance
        }


    }

}

