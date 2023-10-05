/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.s7.config

import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.ScheduleConfiguration.Companion.CONFIG_SCHEDULE_SOURCES
import com.amazonaws.sfc.log.LogLevel
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class S7Configuration : SourceAdapterBaseConfiguration() {

    @SerializedName(CONFIG_SOURCES)
    private var _sources = mapOf<String, S7SourceConfiguration>()

    val sources: Map<String, S7SourceConfiguration>
        get() = _sources.filter { it.value.protocolAdapterID in s7ProtocolAdapters.keys }

    @SerializedName(CONFIG_PROTOCOL_ADAPTERS)
    private var _protocolAdapters = mapOf<String, S7AdapterConfiguration>()

    val s7ProtocolAdapters
        get() = _protocolAdapters.filter { it.value.protocolAdapterType == S7_ADAPTER }


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
            schedule.sources.filter { isS7Source(it) }.forEach { source ->
                validateScheduleInput(source, schedule)
            }
        }
    }

    private fun validateAdapters() {
        ConfigurationException.check(
            s7ProtocolAdapters.isNotEmpty(),
            "No S7 Protocol adapters found",
            CONFIG_PROTOCOL_ADAPTERS,
            this._protocolAdapters
        )
        s7ProtocolAdapters.forEach { adapter ->
            adapter.value.validate()
        }
    }

    private fun isS7Source(source: Map.Entry<String, ArrayList<String>>) =
        s7ProtocolAdapters[sources[source.key]?.protocolAdapterID]?.protocolAdapterType == S7_ADAPTER

    private fun validateScheduleInput(
        source: Map.Entry<String, ArrayList<String>>,
        schedule: ScheduleConfiguration
    ) {
        val sourceConfig = _sources[source.key]
        ConfigurationException.check(
            (sourceConfig != null),
            "Schedule \"${schedule.name}\" input source \"${source.key}\" `does not exist, S7 existing sources are ${this.sources.keys}",
            "Schedule.$CONFIG_SCHEDULE_SOURCES",
            schedule.sources
        )

        validateInputChannels(source, sourceConfig, schedule)
    }

    private fun validateInputChannels(
        sourceChannelMap: Map.Entry<String, ArrayList<String>>,
        source: S7SourceConfiguration?,
        schedule: ScheduleConfiguration
    ) {
        sourceChannelMap.value.forEach { channel ->
            ConfigurationException.check(
                ((channel == WILD_CARD) || source!!.channels.containsKey(channel)),
                "Channel item \"$channel\" for S7 source \"${sourceChannelMap.key}\" in schedule \"${schedule.name}\" does not exist, existing channels are ${source!!.channels.keys}",
                "Schedule.$CONFIG_SCHEDULE_SOURCES",
                schedule.sources
            )
        }
    }


    private fun validateSources() {
        ConfigurationException.check(
            sources.isNotEmpty(),
            "Configuration must have 1 or more S7 sources",
            CONFIG_SCHEDULE_SOURCES
        )

        sources.forEach { source ->
            source.value.validate()
        }
    }


    companion object {
        const val S7_ADAPTER = "S7"

        private val default = S7Configuration()

        fun create(sources: Map<String, S7SourceConfiguration> = default._sources,
                   protocolAdapters: Map<String, S7AdapterConfiguration> = default._protocolAdapters,
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
                   secretsManager: SecretsManagerConfiguration? = default._secretsManagerConfiguration): S7Configuration {

            val instance = createBaseConfiguration<S7Configuration>(
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

