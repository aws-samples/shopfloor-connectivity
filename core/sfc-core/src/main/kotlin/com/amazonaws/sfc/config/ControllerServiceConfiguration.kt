/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.config


import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.config.BaseSourceConfiguration.Companion.CONFIG_SOURCE_PROTOCOL_ADAPTER
import com.amazonaws.sfc.config.ChannelConfiguration.Companion.CONFIG_TRANSFORMATION
import com.amazonaws.sfc.config.ScheduleConfiguration.Companion.CONFIG_SCHEDULE_SOURCES
import com.amazonaws.sfc.config.ServerConfiguration.Companion.CONFIG_HEALTH_PROBE
import com.amazonaws.sfc.filters.ChangeFilterConfiguration
import com.amazonaws.sfc.filters.FilterConfiguration
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.transformations.TransformValidationError
import com.amazonaws.sfc.transformations.Transformation
import com.amazonaws.sfc.transformations.validateOperatorTypes
import com.google.gson.annotations.SerializedName

/**
 * Configuration for SFC core controller
 */
@ConfigurationClass
class ControllerServiceConfiguration : ServiceConfiguration() {


    @SerializedName(CONFIG_SOURCES)
    private var _sources = mapOf<String, SourceConfiguration>()

    /**
     * All configured sources.
     * @see SourceConfiguration
     */
    val sources: Map<String, SourceConfiguration>
        get() = _sources

    @SerializedName(CONFIG_TRANSFORMATIONS)
    private var _transformations = mapOf<String, Transformation>()

    /**
     * All configured transformations
     * @see Transformation
     */
    val transformations: Map<String, Transformation>
        get() = _transformations


    @SerializedName(CONFIG_CHANGE_FILTERS)
    private var _changeFilters = mapOf<String, ChangeFilterConfiguration>()

    /**
     * All configured Value  filters
     */
    val changeFilters: Map<String, ChangeFilterConfiguration>
        get() = _changeFilters


    @SerializedName(CONFIG_VALUE_FILTERS)
    private var _valueFilters = mapOf<String, FilterConfiguration>()

    /**
     * All configured Value  filters
     */
    val valueFilters: Map<String, FilterConfiguration>
        get() = _valueFilters


    @SerializedName(CONFIG_HEALTH_PROBE)
    private var _healthProbeConfiguration: HealthProbeConfiguration? = null
    val healthProbeConfiguration
        get() = _healthProbeConfiguration


    val activeSchedules by lazy { schedules.filter { s -> s.active } }

    val usedSources by lazy { sources.filter { src -> activeSchedules.any { it.sources.containsKey(src.key) } } }

    val usedProtocolAdapters by lazy { protocolAdapters.filter { a -> usedSources.any { it.value.protocolAdapterID == a.key } } }

    val usedProtocolAdapterInProcTypes by lazy { protocolAdapterTypes.filter { a -> usedProtocolAdapters.any { it.value.protocolAdapterType == a.key && it.value.protocolAdapterServer.isNullOrEmpty() } } }

    val usedProtocolServers by lazy { protocolAdapterServers.filter { t -> usedProtocolAdapters.any { it.value.protocolAdapterServer == t.key } } }

    val usedActiveTargets by lazy { targets.filter { t -> activeSchedules.any { it.targets.contains(t.key) } && t.value.active } }

    val usedActiveTargetInProcTypes by lazy { targetTypes.filter { t -> usedActiveTargets.any { it.value.targetType == t.key && it.value.server.isNullOrEmpty() } } }

    val usedActiveTargetServers by lazy { targetServers.filter { t -> usedActiveTargets.any { it.value.server == t.key } } }

    /**
     * Validates the configuration
     * @throws ConfigurationException
     */
    override fun validate() {
        if (validated) return
        super.validate()
        validateAtLeastOneActiveSchedule()
        validateSchedules()
        validateSources()
        validateAggregationSourceTransformations()
        validateTargetsCredentialClients()
        validateValueFilters()
        validateTargets()
        usedProtocolAdapterInProcTypes.forEach { it.value.validate() }
        usedProtocolServers.forEach { it.value.validate() }
        usedActiveTargetInProcTypes.forEach { it.value.validate() }
        usedActiveTargetServers.forEach { it.value.validate() }
        validated = true
    }


    private fun validateTargets() {

        fun walkTargets(targetID: String, targetIDs: List<String>): List<String> {

            var targetPath = mutableListOf<String>()
            targetPath.addAll(targetIDs)

            ConfigurationException.check(
                (!targetPath.contains(targetID)),
                "Target \"$targetID\" causes loop in sub target chain ${targetPath.joinToString(separator = " -> ")} -> $targetID",
                CONFIG_TARGETS,
                targets
            )
            targetPath.add(targetID)

            val target = targets[targetID]
            target?.subTargets?.forEach {
                targetPath = walkTargets(it, targetPath) as MutableList<String>
            }
            return targetPath
        }

        targets.forEach {
            it.value.validate()
            walkTargets(it.key, emptyList())
        }
    }

    private fun validateValueFilters() {
        valueFilters.values.forEach {
            it.validate()
        }
    }

    // validates the all configured transformations
    private fun validateAggregationSourceTransformations() {
        transformations.forEach { (id, transformation) ->
            validateTransformation(id, transformation)
        }
    }

    // validates a single transformation
    private fun validateTransformation(id: String, transformation: Transformation) {
        val error: TransformValidationError? = transformation.validateOperatorTypes()
        ConfigurationException.check(
            (error == null),
            "Transformation \"$id\" is invalid, $error",
            CONFIG_TRANSFORMATIONS,
            transformation.toString()
        )
    }


    // validates all configured sources
    private fun validateSources() {
        sources.forEach { (sourceID, source) ->

            source.validate()

            ConfigurationException.check(
                source.protocolAdapterID in protocolAdapters.keys,
                "Protocol adapter \"${source.protocolAdapterID}\" for source \"$sourceID\" is not a valid adapter, valid adapters are ${protocolAdapters.keys}",
                CONFIG_SOURCE_PROTOCOL_ADAPTER,
                source
            )

            validateSourceChangeFilter(sourceID, source)

            source.channels.forEach { (channelID, channel) ->
                validateChannel(sourceID, channelID, channel)
            }
        }
    }


    // validates all configured schedules
    private fun validateSchedules() {
        schedules.forEach { schedule ->
            schedule.validate()
            validateScheduleInput(schedule)
            validateAggregationTransformations(schedule)
        }
    }


    // validates a configured channel in a source
    private fun validateChannel(sourceID: String, channelID: String, channel: ChannelConfiguration) {

        validateChannelName(sourceID, channel)
        validateChannelTransformation(sourceID, channelID, channel)
        validateChannelValueFilter(sourceID, channelID, channel)
        validateChannelChangeFilter(sourceID, channelID, channel)
    }

    private fun validateChannelValueFilter(sourceID: String, channelID: String, channel: ChannelConfiguration) {

        val valueFilterID = channel.valueFilterID ?: return

        ConfigurationException.check(
            (valueFilters.containsKey(valueFilterID)),
            "$CONFIG_VALUE_FILTER \"${valueFilterID}\" in channel \"${channelID}\" for source \"${sourceID}\"  does not exist, existing value filters are are ${valueFilters.keys}",
            "Channel.$CONFIG_VALUE_FILTER",
            channel
        )

        val filterConfig = valueFilters[valueFilterID]
        filterConfig?.validate()

    }

    private fun validateChannelChangeFilter(sourceID: String, channelID: String, channel: ChannelConfiguration) {
        val changeFilterID = channel.changeFilterID ?: return
        ConfigurationException.check(
            ((changeFilters.containsKey(changeFilterID))),
            "$CONFIG_CHANGE_FILTER \"${changeFilterID}\" in channel \"${channelID}\" for source \"${sourceID}\"  does not exist, existing value filters are are ${changeFilters.keys}",
            "Channel.$CONFIG_CHANGE_FILTER",
            channel
        )

        val filterConfig = valueFilters[changeFilterID]
        filterConfig?.validate()
    }

    private fun validateSourceChangeFilter(sourceID: String, source: SourceConfiguration) {
        val changeFilterID = source.changeFilterID ?: ""
        ConfigurationException.check(
            ((changeFilterID.isEmpty()) || (changeFilters.containsKey(changeFilterID))),
            "$CONFIG_CHANGE_FILTER \"${changeFilterID}\" in  source \"${sourceID}\"  does not exist, existing value filters are are ${changeFilters.keys}",
            "Source.$CONFIG_CHANGE_FILTER",
            source
        )
    }

    private fun validateChannelTransformation(sourceID: String, channelID: String, channel: ChannelConfiguration) {
        val transformationID = channel.transformationID ?: ""
        ConfigurationException.check(
            ((transformationID.isEmpty()) || (transformations.containsKey(transformationID))),
            "$CONFIG_TRANSFORMATION \"${transformationID}\" in channel \"${channelID}\" for source \"${sourceID}\"  does not exist, existing transformations are ${transformations.keys}",
            "Channel.$CONFIG_TRANSFORMATION",
            channel
        )
    }

    private fun validateChannelName(sourceID: String, channel: ChannelConfiguration) {
        ConfigurationException.check(
            ChannelConfiguration.CHANNEL_SEPARATOR !in sourceID,
            "ChannelID \"$channel\" of source \"$sourceID\" can not contain '${ChannelConfiguration.CHANNEL_SEPARATOR}' character",
            "Channels",
            sources[sourceID]?.channels
        )
    }


    // validates all  configured transformations for aggregated output
    private fun validateAggregationTransformations(schedule: ScheduleConfiguration) {
        if (schedule.aggregation != null) {

            schedule.aggregation!!.transformations.forEach { (source, transformation) ->
                validateAggregationSourceTransformations(schedule, source, transformation)
            }
        }
    }

    // aggregates all transformations for aggregated outputs for a source
    private fun validateAggregationSourceTransformations(
        schedule: ScheduleConfiguration,
        source: List<String>,
        transformation: Map<List<String>, Map<List<String>, String>>
    ) {
        transformation.forEach { (channelName, channel) ->
            validateAggregationChannelTransformations(schedule, source, channelName, channel)
        }
    }

    // validates a transformations for aggregates outputs for a channel
    private fun validateAggregationChannelTransformations(
        schedule: ScheduleConfiguration,
        source: List<String>,
        channelName: List<String>,
        channel: Map<List<String>, String>
    ) {
        channel.forEach { (outputName, transformationID) ->
            validateAggregationOutputTransformation(schedule, source, channelName, outputName, transformationID)
        }
    }

    // validated a transformation for an aggregated output
    private fun validateAggregationOutputTransformation(
        schedule: ScheduleConfiguration,
        source: List<String>,
        channelName: List<String>,
        outputName: List<String>,
        transformationID: String
    ) =
        ConfigurationException.check(
            (transformations.containsKey(transformationID)),
            "Transformation \"$transformationID for aggregation of  source \"${source}\", channel \"${channelName}\", output $outputName " +
            "in schedule \"${schedule.name}\" is not a valid transformation",
            "Schedule.Aggregation.TransformationsDeserializer",
            schedule.aggregation!!.transformations
        )

    // validates the input for a configured schedule
    private fun validateScheduleInput(schedule: ScheduleConfiguration) {
        schedule.sources.forEach { (sourceID, sourceChannels) ->
            val source = validateInputSourceExists(sourceID, schedule)
            sourceChannels.forEach { channel ->
                validateInputSourceChannels(schedule, sourceID, source, channel)
            }
        }
    }

    // validates a channel configured for a source
    private fun validateInputSourceChannels(schedule: ScheduleConfiguration, inputID: String, source: SourceConfiguration, channel: String) =
        ConfigurationException.check(
            ((channel == WILD_CARD) || (source.channels.containsKey(channel))),
            "Channel \"$channel\" for source \"$inputID\" in schedule \"${schedule.name}\" does not exist, existing channels are ${source.channels.keys}",
            "Schedule.$CONFIG_SCHEDULE_SOURCES",
            schedule.sources
        )

    // validates if a configured source for a schedule is configured
    private fun validateInputSourceExists(inputID: String, schedule: ScheduleConfiguration): SourceConfiguration {
        val source = sources[inputID]
        ConfigurationException.check(
            (source != null),
            "Schedule \"${schedule.name}\" source \"$inputID\" does not exist, existing sources are ${sources.keys}",
            "Schedule.$CONFIG_SCHEDULE_SOURCES",
            schedule.sources
        )
        return source!!
    }


    // check there is at least a single active schedule in the configuration
    private fun validateAtLeastOneActiveSchedule() {

        ConfigurationException.check(
            schedules.isNotEmpty(),
            "No schedules in configuration",
            CONFIG_SCHEDULES
        )

        ConfigurationException.check(
            (schedules.any { it.active }),
            "Configuration must contain one or more active schedules",
            CONFIG_SCHEDULES
        )
    }

    private fun validateTargetsCredentialClients() {
        awsCredentialServiceClients.values.forEach {
            it.validate()
        }


        targets.forEach { (targetID, target) ->
            val clientID = target.credentialProviderClient
            if (clientID != null) {
                ConfigurationException.check(
                    awsCredentialServiceClients.containsKey(clientID),
                    "CredentialsClient \"$clientID\" for target $targetID does not exist, configured clients are ${awsCredentialServiceClients.keys}",
                    CONFIG_TARGETS,
                    target
                )
            }
        }

    }

    companion object {
        private val default = ControllerServiceConfiguration()

        fun create(sources: Map<String, SourceConfiguration> = default._sources,
                   transformations: Map<String, Transformation> = default._transformations,
                   changeFilters: Map<String, ChangeFilterConfiguration> = default._changeFilters,
                   valueFilters: Map<String, FilterConfiguration> = default._valueFilters,
                   targets: Map<String, TargetConfiguration> = default._targets,
                   protocolAdapters: Map<String, ProtocolAdapterConfiguration> = default._protocols,
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
                   healthProbeConfiguration: HealthProbeConfiguration? = default._healthProbeConfiguration,
                   awsIotCredentialProviderClients: Map<String, AwsIotCredentialProviderClientConfiguration> = default._awsIoTCredentialProviderClients,
                   secretsManagerConfiguration: SecretsManagerConfiguration? = default._secretsManagerConfiguration): ControllerServiceConfiguration {

            val instance = createServiceConfiguration<ControllerServiceConfiguration>(
                targets = targets,
                protocolAdapters = protocolAdapters,
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
                _transformations = transformations
                _changeFilters = changeFilters
                _valueFilters = valueFilters
                _healthProbeConfiguration = healthProbeConfiguration
            }
            return instance
        }
    }

}


