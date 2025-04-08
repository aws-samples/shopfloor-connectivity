// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.simulator.config

import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.ScheduleConfiguration.Companion.CONFIG_SCHEDULE_SOURCES
import com.amazonaws.sfc.log.LogLevel
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class SimulatorConfiguration : SourceAdapterBaseConfiguration() {

    @SerializedName(CONFIG_SOURCES)
    private var _sources = mapOf<String, SimulatorSourceConfiguration>()

    val sources: Map<String, SimulatorSourceConfiguration>
        get() = _sources.filter { it.value.protocolAdapterID in simulatorProtocolAdapters.keys }

    @SerializedName(CONFIG_PROTOCOL_ADAPTERS)
    private var _protocolAdapters = mapOf<String, SimulatorAdapterConfiguration>()

    val simulatorProtocolAdapters: Map<String, SimulatorAdapterConfiguration>
        get() = _protocolAdapters.filter { it.value.protocolAdapterType == SIMULATOR_ADAPTER }


    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return
        super.validate()
        validateSchedules()
        validateAdapters()
        validateAtLeastOneSource()
        sources.values.forEach { it.validate() }
        validated = true
    }

    private fun validateAdapters() {
        simulatorProtocolAdapters.values.forEach { it.validate() }
    }


    private fun validateSchedules() {
        schedules.forEach { schedule ->
            schedule.sources.filter { isSimulationSource(it) }.forEach { source ->
                validateScheduleInput(source, schedule)
            }
        }
    }

    private fun isSimulationSource(source: Map.Entry<String, ArrayList<String>>) =
        simulatorProtocolAdapters[sources[source.key]?.protocolAdapterID]?.protocolAdapterType == SIMULATOR_ADAPTER

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
        source: SimulatorSourceConfiguration?,
        schedule: ScheduleConfiguration
    ) {
        sourceChannelMap.value.forEach { channel ->
            ConfigurationException.check(
                ((channel == WILD_CARD) || source!!.channels.containsKey(channel)),
                "Channel item \"$channel\" for Simulation source \"${sourceChannelMap.key}\" in schedule \"${schedule.name}\" does not exist, existing channels are ${source!!.channels.keys}",
                "Schedule.$CONFIG_SCHEDULE_SOURCES",
                schedule.sources
            )
        }
    }


    private fun validateAtLeastOneSource() {
        ConfigurationException.check(
            sources.isNotEmpty(),
            "Configuration must have 1 or more Simulation sources",
            CONFIG_SCHEDULE_SOURCES
        )
    }


    companion object {
        const val SIMULATOR_ADAPTER = "SIMULATOR"


        const val CONFIG_ARRAY_SIZE = "Size"
        const val CONFIG_CYCLE_LENGTH = "CycleLength"
        const val CONFIG_DATA_TYPE = "DataType"
        const val CONFIG_DIRECTION = "Direction"
        const val CONFIG_INTERVAL = "Interval"
        const val CONFIG_ITEM = "Item"
        const val CONFIG_ITEMS = "Items"
        const val CONFIG_MAX = "Max"
        const val CONFIG_MIN = "Min"
        const val CONFIG_STRUCT_PROPERTIES = "Properties"
        const val CONFIG_RANDOM = "Random"
        const val CONFIG_SHIFT = "Shift"
        const val CONFIG_SIMULATION = "Simulation"
        const val CONFIG_SIMULATION_TYPE = "SimulationType"
        const val CONFIG_STEP = "Step"
        const val CONFIG_VALUE = "Value"
        const val CONFIG_VALUES = "Values"

        const val DEFAULT_CYCLE_LENGTH = 10 * 1000L
        const val DEFAULT_INTERVAL = 1000L

        private val default = SimulatorConfiguration()

        fun create(sources: Map<String, SimulatorSourceConfiguration> = default._sources,
                   protocolAdapters: Map<String, SimulatorAdapterConfiguration> = default._protocolAdapters,
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
                   templatesConfiguration: TemplatesConfiguration? = default._templates): SimulatorConfiguration {

            val instance = createBaseConfiguration<SimulatorConfiguration>(
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
                templates = templatesConfiguration,
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

