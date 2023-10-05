/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.mqtt.config

import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.ScheduleConfiguration.Companion.CONFIG_SCHEDULE_SOURCES
import com.amazonaws.sfc.log.LogLevel
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class MqttConfiguration : SourceAdapterBaseConfiguration() {

    @SerializedName(CONFIG_SOURCES)
    private var _sources = mapOf<String, MqttSourceConfiguration>()

    val sources: Map<String, MqttSourceConfiguration>
        get() = _sources.filter { it.value.protocolAdapterID in mqttProtocolAdapters.keys }

    @SerializedName(CONFIG_PROTOCOL_ADAPTERS)
    private var _protocolAdapters = mapOf<String, MqttAdapterConfiguration>()

    val mqttProtocolAdapters: Map<String, MqttAdapterConfiguration>
        get() = _protocolAdapters.filter { it.value.protocolAdapterType == MQTT_ADAPTER }

    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return
        super.validate()
        validateSchedules()
        validateAtLeastOneSource()
        validated = true
    }


    private fun validateSchedules() {
        schedules.forEach { schedule ->
            schedule.sources.filter { isMqttSource(it) }.forEach { source ->
                validateScheduleInput(source, schedule)
            }
        }
    }

    private fun isMqttSource(source: Map.Entry<String, ArrayList<String>>) =
        mqttProtocolAdapters[sources[source.key]?.protocolAdapterID]?.protocolAdapterType == MQTT_ADAPTER

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
        source: MqttSourceConfiguration?,
        schedule: ScheduleConfiguration
    ) {
        sourceChannelMap.value.forEach { channel ->
            ConfigurationException.check(
                ((channel == WILD_CARD) || source!!.channels.containsKey(channel)),
                "Channel item \"$channel\" for MQTT source \"${sourceChannelMap.key}\" in schedule \"${schedule.name}\" does not exist, existing channels are ${source!!.channels.keys}",
                "Schedule.$CONFIG_SCHEDULE_SOURCES",
                schedule.sources
            )
        }
    }


    private fun validateAtLeastOneSource() {
        ConfigurationException.check(
            sources.isNotEmpty(),
            "Configuration must have 1 or more MQTT sources",
            CONFIG_SCHEDULE_SOURCES
        )
    }


    companion object {
        const val MQTT_ADAPTER = "MQTT"

        private val default = MqttConfiguration()

        fun create(sources: Map<String, MqttSourceConfiguration> = default._sources,
                   protocolAdapters: Map<String, MqttAdapterConfiguration> = default._protocolAdapters,
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
                   secretsManagerConfiguration: SecretsManagerConfiguration? = default._secretsManagerConfiguration): MqttConfiguration {

            val instance = createBaseConfiguration<MqttConfiguration>(
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
                _sources = sources
                _protocolAdapters = protocolAdapters
            }
            return instance
        }
    }


}

