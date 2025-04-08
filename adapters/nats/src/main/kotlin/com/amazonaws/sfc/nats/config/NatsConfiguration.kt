// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.nats.config

import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.ScheduleConfiguration.Companion.CONFIG_SCHEDULE_SOURCES
import com.amazonaws.sfc.log.LogLevel
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class NatsConfiguration : SourceAdapterBaseConfiguration() {

    @SerializedName(CONFIG_SOURCES)
    private var _sources = mapOf<String, NatsSourceConfiguration>()

    val sources: Map<String, NatsSourceConfiguration>
        get() = _sources.filter { it.value.protocolAdapterID in natsProtocolAdapters.keys  }

    @SerializedName(CONFIG_PROTOCOL_ADAPTERS)
    private var _protocolAdapters = mapOf<String, NatsAdapterConfiguration>()

    val natsProtocolAdapters: Map<String, NatsAdapterConfiguration>
        get() = _protocolAdapters.filter { it.value.protocolAdapterType == NATS_ADAPTER }

    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return
        super.validate()
        validateSchedules()
        validateAtLeastOneSource()
        validateProtocolAdapters()
        validated = true
    }


    private fun validateSchedules() {
        schedules.forEach { schedule ->
            schedule.sources.filter { isNastSource(it) }.forEach { source ->
                validateScheduleInput(source, schedule)
            }
        }
    }

    private fun validateProtocolAdapters(){
        natsProtocolAdapters.values.forEach { protocolAdapter ->
           protocolAdapter.validate()
        }
    }

    private fun isNastSource(source: Map.Entry<String, ArrayList<String>>) =
        natsProtocolAdapters[sources[source.key]?.protocolAdapterID]?.protocolAdapterType == NATS_ADAPTER

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
        source: NatsSourceConfiguration?,
        schedule: ScheduleConfiguration
    ) {
        sourceChannelMap.value.forEach { channel ->
            ConfigurationException.check(
                ((channel == WILD_CARD) || source!!.channels.containsKey(channel)),
                "Channel item \"$channel\" for NATS source \"${sourceChannelMap.key}\" in schedule \"${schedule.name}\" does not exist, existing channels are ${source!!.channels.keys}",
                "Schedule.$CONFIG_SCHEDULE_SOURCES",
                schedule.sources
            )
        }
    }


    private fun validateAtLeastOneSource() {
        ConfigurationException.check(
            sources.isNotEmpty(),
            "Configuration must have 1 or more NATS sources",
            CONFIG_SCHEDULE_SOURCES
        )
    }


    companion object {
        const val NATS_ADAPTER = "NATS"


        private val default = NatsConfiguration()

        fun create(
            sources: Map<String, NatsSourceConfiguration> = default._sources,
            protocolAdapters: Map<String, NatsAdapterConfiguration> = default._protocolAdapters,
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
            monitorIncludedConfigFilesInterval : Duration = default._monitorIncludedConfigFilesInterval.toDuration(DurationUnit.SECONDS),
            templates: TemplatesConfiguration?
        ): NatsConfiguration {

            val instance = createBaseConfiguration<NatsConfiguration>(
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

