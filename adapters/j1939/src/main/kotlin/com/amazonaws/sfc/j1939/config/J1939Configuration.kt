// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.j1939.config

import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseSourceConfiguration.Companion.CONFIG_SOURCE_PROTOCOL_ADAPTER
import com.amazonaws.sfc.config.ScheduleConfiguration.Companion.CONFIG_SCHEDULE_SOURCES
import com.amazonaws.sfc.j1939.config.J1939SourceConfiguration.Companion.CONFIG_ADAPTER_CAN_SOCKET
import com.amazonaws.sfc.log.LogLevel
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class J1939Configuration : SourceAdapterBaseConfiguration() {

    @SerializedName(CONFIG_SOURCES)
    private var _sources = mapOf<String, J1939SourceConfiguration>()

    val sources: Map<String, J1939SourceConfiguration>
        get() = _sources.filter { it.value.protocolAdapterID in j1939ProtocolAdapters.keys }

    @SerializedName(CONFIG_PROTOCOL_ADAPTERS)
    private var _protocolAdapters = mapOf<String, J1939AdapterConfiguration>()

    val j1939ProtocolAdapters: Map<String, J1939AdapterConfiguration>
        get() = _protocolAdapters.filter { it.value.protocolAdapterType == J1939_ADAPTER }

    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return
        super.validate()
        j1939ProtocolAdapters.forEach { it.value.validate() }
        validateSchedules()
        validateAtLeastOneSource()
        validateSourceSockets()
        validated = true
    }

    private fun validateSchedules() {
        schedules.forEach { schedule ->
            schedule.sources.filter { isJ1939Source(it) }.forEach { source ->
                validateScheduleInput(source, schedule)
            }
        }
    }

    private fun validateSourceSockets() {
        sources.forEach { (sourceID, sourceConfig) ->
            val sourceAdapter = sourceConfig.protocolAdapterID
            val adapter = j1939ProtocolAdapters[sourceAdapter] ?: throw ConfigurationException(
                "J1939 source $sourceID has invalid protocol adapter $sourceAdapter, valid j1939 protocols adapters are ${j1939ProtocolAdapters.keys}",
                CONFIG_SOURCE_PROTOCOL_ADAPTER,
                this)
            if (sourceConfig.adapterCanSocket !in adapter.canSockets.keys)
                throw ConfigurationException(
                    "J1939 source $sourceID $CONFIG_ADAPTER_CAN_SOCKET \"${sourceConfig.adapterCanSocket}\" is not configured for adapter ${sourceConfig.protocolAdapterID}, configured sockets are ${adapter.canSockets.keys}",
                    CONFIG_ADAPTER_CAN_SOCKET,
                    sourceConfig)
        }
    }

    private fun isJ1939Source(source: Map.Entry<String, ArrayList<String>>) =
        j1939ProtocolAdapters[sources[source.key]?.protocolAdapterID]?.protocolAdapterType == J1939_ADAPTER

    private fun validateScheduleInput(
        source: Map.Entry<String, ArrayList<String>>,
        schedule: ScheduleConfiguration
    ) {
        val sourceConfig = _sources[source.key]
        ConfigurationException.check(
            (sourceConfig != null),
            "Schedule \"${schedule.name}\" input source \"${source.key}\" `does not exist, existing sources are ${this.sources.keys}",
            "Schedule.$CONFIG_SCHEDULE_SOURCES",
            schedule.sources
        )

        validateInputChannels(source, sourceConfig, schedule)
    }

    private fun validateInputChannels(
        sourceChannelMap: Map.Entry<String, ArrayList<String>>,
        source: J1939SourceConfiguration?,
        schedule: ScheduleConfiguration
    ) {
        sourceChannelMap.value.forEach { channel ->
            ConfigurationException.check(
                ((channel == WILD_CARD) || source!!.channels.containsKey(channel)),
                "Channel item \"$channel\" for J1939 source \"${sourceChannelMap.key}\" in schedule \"${schedule.name}\" does not exist, existing channels are ${source!!.channels.keys}",
                "Schedule.$CONFIG_SCHEDULE_SOURCES",
                schedule.sources
            )
        }
    }


    private fun validateAtLeastOneSource() {
        ConfigurationException.check(
            sources.isNotEmpty(),
            "Configuration must have 1 or more J1939 sources",
            CONFIG_SCHEDULE_SOURCES
        )
    }

    companion object {
        const val J1939_ADAPTER = "J1939"


        private val default = J1939Configuration()

        fun create(
            sources: Map<String, J1939SourceConfiguration> = default._sources,
            protocolAdapters: Map<String, J1939AdapterConfiguration> = default._protocolAdapters,
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
            secretsManagerConfiguration: SecretsManagerConfiguration? = default._secretsManagerConfiguration,
            monitorIncludedConfigFiles: Boolean = default._monitorIncludedConfigFiles,
            monitorIncludedConfigFilesInterval: Duration = default._monitorIncludedConfigFilesInterval.toDuration(DurationUnit.SECONDS),
            templates: TemplatesConfiguration?
        ): J1939Configuration {

            val instance = createBaseConfiguration<J1939Configuration>(
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
                secretsManagerConfiguration = secretsManagerConfiguration,
                templates = templates,
                monitorIncludedConfigFilesInterval = monitorIncludedConfigFilesInterval,
                monitorIncludedConfigFiles = monitorIncludedConfigFiles
            )

            with(instance) {
                _sources = sources
                _protocolAdapters = protocolAdapters
            }
            return instance
        }
    }


}

