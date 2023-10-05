/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.modbus.tcp.config


import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseSourceConfiguration.Companion.CONFIG_SOURCE_PROTOCOL_ADAPTER
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.modbus.config.ModbusConfiguration
import com.amazonaws.sfc.modbus.config.ModbusSourceConfiguration
import com.amazonaws.sfc.modbus.config.ModbusSourceConfiguration.Companion.CONFIG_SOURCE_ADAPTER_DEVICE
import com.google.gson.annotations.SerializedName

/**
 * Modbus TCP protocol adapter configuration
 */
@ConfigurationClass
open class ModbusTcpConfiguration : ModbusConfiguration() {

    @SerializedName(CONFIG_PROTOCOL_ADAPTERS)
    private var _protocolAdapters = mapOf<String, ModbusTcpAdapterConfiguration>()

    val modbusTcpAdapters: Map<String, ModbusTcpAdapterConfiguration>
        get() = _protocolAdapters.filter { it.value.protocolAdapterType == MODBUS_TCP_ADAPTER }

    @SerializedName(CONFIG_SOURCES)
    private var _sources = mapOf<String, ModbusSourceConfiguration>()

    /**
     * Configured Modbus sources
     */
    override val sources: Map<String, ModbusSourceConfiguration>
        get() = _sources.filter { it.value.protocolAdapterID in modbusTcpAdapters.keys }


    /**
     * Validates the configuration
     * @throws ConfigurationException
     */
    override fun validate() {
        if (validated) return
        super.validate()
        validateAtLeastOneSource()
        schedules.forEach { schedule ->
            validateSchedule(schedule)
        }
        sources.forEach {
            validateSource(it)
        }
        validated = true
    }

    private fun validateAtLeastOneSource() {
        ConfigurationException.check(
            sources.isNotEmpty(),
            "Configuration must have 1 or more sources",
            ScheduleConfiguration.CONFIG_SCHEDULE_SOURCES
        )
    }

    private fun validateSchedule(schedule: ScheduleConfiguration) {
        schedule.sources.forEach { source ->
            validateScheduleSources(source, schedule)
        }
    }

    private fun validateScheduleSources(sources: Map.Entry<String, ArrayList<String>>, schedule: ScheduleConfiguration) {
        val source = this.sources[sources.key]
        // source can be null if the schedule contains sources of different protocol types, sources property only contains sources of Modbus TCP type
        if (source != null) {
            validateSourceChannels(sources, source, schedule)
        }
    }

    // Validates a configured Modbus source
    private fun validateSource(source: Map.Entry<String, ModbusSourceConfiguration>) {

        ConfigurationException.check(
            modbusTcpAdapters.containsKey(source.value.protocolAdapterID),
            "Source \"${source.key}\", protocols adapter \"${source.value.protocolAdapterID}\" does not exist, existing adapters are ${modbusTcpAdapters.keys}",
            CONFIG_SOURCE_PROTOCOL_ADAPTER,
            source
        )

        val modbusTcpAdapterConfiguration = modbusTcpAdapters[source.value.protocolAdapterID]
        val adapterDevices = modbusTcpAdapterConfiguration?.devices ?: emptyMap()

        ConfigurationException.check(
            source.value.sourceAdapterDevice in adapterDevices.keys,
            "Source \"${source.key}\", $CONFIG_SOURCE_ADAPTER_DEVICE \"${source.value.sourceAdapterDevice}\" does not exist for $CONFIG_SOURCE_PROTOCOL_ADAPTER \"${source.value.protocolAdapterID}\", existing device are ${adapterDevices.keys}",
            CONFIG_SOURCE_ADAPTER_DEVICE,
            source
        )

    }

    private fun validateSourceChannels(
        sourceChannels: Map.Entry<String, ArrayList<String>>,
        source: ModbusSourceConfiguration?,
        schedule: ScheduleConfiguration
    ) {
        sourceChannels.value.forEach { channel ->
            ConfigurationException.check(
                ((channel == WILD_CARD) || source!!.channels.containsKey(channel)),
                "Channel \"$channel\" for source \"${sourceChannels.key}\" in schedule \"${schedule.name}\" does not exist, existing channels are ${source!!.channels}",
                "Schedule.${ScheduleConfiguration.CONFIG_SCHEDULE_SOURCES}",
                schedule.sources
            )
        }
    }

    companion object {
        const val MODBUS_TCP_ADAPTER = "MODBUS-TCP"

        private val default = ModbusTcpConfiguration()

        fun create(protocolAdapters: Map<String, ModbusTcpAdapterConfiguration> = default._protocolAdapters,
                   sources: Map<String, ModbusSourceConfiguration> = default._sources,
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
                   secretsManagerConfiguration: SecretsManagerConfiguration? = default._secretsManagerConfiguration): ModbusTcpConfiguration {

            val instance = createBaseConfiguration<ModbusTcpConfiguration>(
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
                secretsManagerConfiguration = secretsManagerConfiguration)

            with(instance) {
                _protocolAdapters = protocolAdapters
                _sources = sources
            }

            return instance
        }

    }

}