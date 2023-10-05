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

import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.ScheduleConfiguration.Companion.CONFIG_SCHEDULE_SOURCES
import com.amazonaws.sfc.log.LogLevel
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class SnmpConfiguration : SourceAdapterBaseConfiguration() {

    @SerializedName(CONFIG_SOURCES)
    private var _sources = mapOf<String, SnmpSourceConfiguration>()

    val snmpSources: Map<String, SnmpSourceConfiguration>
        get() = _sources.filter { it.value.protocolAdapterID in snmpProtocolAdapters.keys }

    @SerializedName(CONFIG_PROTOCOL_ADAPTERS)
    private var _protocolAdapters = mapOf<String, SnmpAdapterConfiguration>()

    val snmpProtocolAdapters: Map<String, SnmpAdapterConfiguration>
        get() = _protocolAdapters.filter { it.value.protocolAdapterType == SNMP_ADAPTER }

    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return
        super.validate()
        validateSchedules()
        validateAdapters()
        validateSources()
        validated = true
    }


    private fun validateSchedules() {
        schedules.forEach { schedule ->
            schedule.sources.filter { isSnmpSource(it) }.forEach { source ->
                validateScheduleInput(source, schedule)
            }
        }
    }

    private fun validateAdapters() {
        ConfigurationException.check(
            snmpProtocolAdapters.isNotEmpty(),
            "No SNMP protocol adapters configured",
            CONFIG_PROTOCOL_ADAPTERS,
            this._protocolAdapters
        )

        snmpProtocolAdapters.forEach { adapter ->
            adapter.value.validate()
        }
    }

    private fun isSnmpSource(source: Map.Entry<String, ArrayList<String>>) =
        snmpProtocolAdapters[snmpSources[source.key]?.protocolAdapterID]?.protocolAdapterType == SNMP_ADAPTER

    private fun validateScheduleInput(
        source: Map.Entry<String, ArrayList<String>>,
        schedule: ScheduleConfiguration
    ) {
        val sourceConfig = _sources[source.key]
        ConfigurationException.check(
            (sourceConfig != null),
            "Schedule \"${schedule.name}\" input source \"${source.key}\" `does not exist, existing SNMP sources are ${this.snmpSources.keys}",
            "Schedule.$CONFIG_SCHEDULE_SOURCES",
            schedule.sources
        )

        validateInputChannels(source, sourceConfig, schedule)
    }

    private fun validateInputChannels(
        sourceChannelMap: Map.Entry<String, ArrayList<String>>,
        source: SnmpSourceConfiguration?,
        schedule: ScheduleConfiguration
    ) {
        sourceChannelMap.value.forEach { channel ->
            ConfigurationException.check(
                ((channel == WILD_CARD) || source!!.channels.containsKey(channel)),
                "Channel item \"$channel\" for SNMP source \"${sourceChannelMap.key}\" in schedule \"${schedule.name}\" does not exist, existing channels are ${source!!.channels.keys}",
                "Schedule.$CONFIG_SCHEDULE_SOURCES",
                schedule.sources
            )
        }
    }


    private fun validateSources() {
        ConfigurationException.check(
            snmpSources.isNotEmpty(),
            "Configuration must have 1 or more SNMP sources",
            CONFIG_SCHEDULE_SOURCES
        )

        snmpSources.forEach { source ->
            source.value.validate()
        }

    }


    companion object {
        const val SNMP_ADAPTER = "SNMP"

        private val default = SnmpConfiguration()

        fun create(sources: Map<String, SnmpSourceConfiguration> = default._sources,
                   protocolAdapters: Map<String, SnmpAdapterConfiguration> = default._protocolAdapters,
                   name: String = default._name,
                   version: String = default._version,
                   aWSVersion: String? = default._awsVersion,
                   description: String = default._description,
                   schedules: List<ScheduleConfiguration> = default._schedules,
                   logLevel: LogLevel? = default._logLevel,
                   metadata: Map<String, String> = default._metadata,
                   elementNamesConfiguration: ElementNamesConfiguration = default._elementNames,
                   targetServers: Map<String, ServerConfiguration> = default._targetServers,
                   targetTypes: Map<String, InProcessConfiguration> = default._targetTypes,
                   adapterServers: Map<String, ServerConfiguration> = default._protocolAdapterServers,
                   adapterTypes: Map<String, InProcessConfiguration> = default._protocolTypes,
                   awsIotCredentialProviderClients: Map<String, AwsIotCredentialProviderClientConfiguration> = default._awsIoTCredentialProviderClients,
                   secretsManager: SecretsManagerConfiguration? = default._secretsManagerConfiguration): SnmpConfiguration {

            val instance = SnmpConfiguration()
            with(instance) {
                _sources = sources
                _protocolAdapters = protocolAdapters
                _name = name
                _version = version
                _awsVersion = aWSVersion
                _description = description
                _schedules = schedules
                _logLevel = logLevel
                _metadata = metadata
                _elementNames = elementNamesConfiguration
                _targetServers = targetServers
                _targetTypes = targetTypes
                _protocolAdapterServers = adapterServers
                _protocolTypes = adapterTypes
                _awsIoTCredentialProviderClients = awsIotCredentialProviderClients
                _secretsManagerConfiguration = secretsManager
            }
            return instance
        }


    }


}

