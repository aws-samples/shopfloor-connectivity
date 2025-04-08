
/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 */


package com.amazonaws.sfc.slmp.config

import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.ScheduleConfiguration.Companion.CONFIG_SCHEDULE_SOURCES
import com.amazonaws.sfc.log.LogLevel
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class SlmpConfiguration : SourceAdapterBaseConfiguration() {

    @SerializedName(CONFIG_SOURCES)
    private var _sources = mapOf<String, SlmpSourceConfiguration>()

    val sources: Map<String, SlmpSourceConfiguration>
        get() = _sources.filter {
            it.value.protocolAdapterID in slmpProtocolAdapters.keys
        }

    @SerializedName(CONFIG_PROTOCOL_ADAPTERS)
    private var _protocolAdapters = mapOf<String, SlmpAdapterConfiguration>()

    val slmpProtocolAdapters: Map<String, SlmpAdapterConfiguration>
        get() = _protocolAdapters.filter { it.value.protocolAdapterType == SLMP_ADAPTER }


    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return
        super.validate()
        validateSchedules()
        validateAdapters()
        validateAtLeastOneSource()
        validateAdapterAddresses()
        sources.values.forEach { it.validate() }
        validated = true
    }


    private fun validateAdapterAddresses(){
        val addresses = slmpProtocolAdapters.values.map{it.controllers.values.map{ a->a.address}}.groupBy { it }
        addresses.forEach{
            if  (it.value.size > 1){
                throw ConfigurationException("Duplicate address ${it.key} found in protocol adapters", CONFIG_PROTOCOL_ADAPTERS)
            }
        }

    }
    private fun validateAdapters() {
        slmpProtocolAdapters.values.forEach { it.validate() }
    }

    private fun validateSchedules() {
        schedules.forEach { schedule ->
            schedule.sources.filter { isSlmpSource(it) }.forEach { source ->
                validateScheduleInput(source, schedule)
            }
        }
    }

    private fun isSlmpSource(source: Map.Entry<String, ArrayList<String>>) =
        slmpProtocolAdapters[sources[source.key]?.protocolAdapterID]?.protocolAdapterType == SLMP_ADAPTER

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
        source: SlmpSourceConfiguration?,
        schedule: ScheduleConfiguration
    ) {
        sourceChannelMap.value.forEach { channel ->
            ConfigurationException.check(
                ((channel == WILD_CARD) || source!!.channels.containsKey(channel)),
                "Channel item \"$channel\" for SLMP source \"${sourceChannelMap.key}\" in schedule \"${schedule.name}\" does not exist, existing channels are ${source!!.channels.keys}",
                "Schedule.$CONFIG_SCHEDULE_SOURCES",
                schedule.sources
            )
        }
    }


    private fun validateAtLeastOneSource() {
        ConfigurationException.check(
            sources.isNotEmpty(),
            "Configuration must have 1 or more SLMP sources",
            CONFIG_SCHEDULE_SOURCES
        )
    }


    companion object {
        const val SLMP_ADAPTER = "SLMP"

        private val default = SlmpConfiguration()

        fun create(
            sources: Map<String, SlmpSourceConfiguration> = default._sources,
            protocolAdapters: Map<String, SlmpAdapterConfiguration> = default._protocolAdapters,
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
            templates : TemplatesConfiguration? = default._templates
        ): SlmpConfiguration {

            val instance = createBaseConfiguration<SlmpConfiguration>(
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

