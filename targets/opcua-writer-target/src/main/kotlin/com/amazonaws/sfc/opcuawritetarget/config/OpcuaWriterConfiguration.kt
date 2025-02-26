// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.opcuawritetarget.config

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.ChannelConfiguration.Companion.CONFIG_TRANSFORMATION
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.transformations.TransformValidationError
import com.amazonaws.sfc.transformations.Transformation
import com.amazonaws.sfc.transformations.validateOperatorTypes
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * AWS IoT Core target configuration
 */
@ConfigurationClass
class OpcuaWriterConfiguration : BaseConfigurationWithMetrics(), Validate {

    @SerializedName(CONFIG_TARGETS)
    private var _targets: Map<String, OpcuaWriterTargetConfiguration> = emptyMap()
    val targets: Map<String, OpcuaWriterTargetConfiguration>
        get() = _targets.filter { (it.value.targetType == OPCUA_WRITER_TARGET) }

    @SerializedName(CONFIG_TRANSFORMATIONS)
    private var _transformations = mapOf<String, Transformation>()
    val transformations: Map<String, Transformation>
        get() = _transformations


    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return

        super.validate()
        validateTargets()
        validateTransformations()
        validated = true
    }

    private fun validateTransformations() {
        _transformations.forEach {
            val error: TransformValidationError? = it.value.validateOperatorTypes()
            ConfigurationException.check(
                (error == null),
                "Transformation \"${it.key}\" is invalid, $error",
                CONFIG_TRANSFORMATIONS,
                it.value.toString()
            )
        }
        targets.forEach { target ->
            target.value.nodes.forEach { node ->
                if (node.transformationID != null) {
                    ConfigurationException.check(
                        _transformations.containsKey(node.transformationID),
                        "Transformation ${node.transformationID} for node ${node.nodeId?.toParseableString()} not found, available transformations are ${transformations.keys}",
                        CONFIG_TRANSFORMATION,
                        node)
                }
            }
        }
    }

    private fun validateTargets() {
        targets.forEach { target ->
            target.value.validate()
        }
    }


    companion object {
        const val OPCUA_WRITER_TARGET = "OPCUA-WRITER-TARGET"
        private val default = OpcuaWriterConfiguration()

        fun create(targets: Map<String, OpcuaWriterTargetConfiguration> = default._targets,
                   name: String = default._name,
                   version: String = default._version,
                   awsVersion: String? = default._awsVersion,
                   description: String = default._description,
                   schedules: List<ScheduleConfiguration> = default._schedules,
                   logLevel: LogLevel? = default._logLevel,
                   metadata: Map<String, String> = default._metadata,
                   elementNames: ElementNamesConfiguration = default._elementNames,
                   transformations: Map<String, Transformation> = default._transformations,
                   targetServers: Map<String, ServerConfiguration> = default._targetServers,
                   targetTypes: Map<String, InProcessConfiguration> = default._targetTypes,
                   adapterServers: Map<String, ServerConfiguration> = default._protocolAdapterServers,
                   adapterTypes: Map<String, InProcessConfiguration> = default._protocolTypes,
                   secretsManagerConfiguration: SecretsManagerConfiguration? = default._secretsManagerConfiguration,
                   monitorIncludedConfigFiles: Boolean = default._monitorIncludedConfigFiles,
                   monitorIncludedConfigFilesInterval: Duration = default._monitorIncludedConfigFilesInterval.toDuration(DurationUnit.SECONDS),
                   templatesConfiguration: TemplatesConfiguration? = default._templates): OpcuaWriterConfiguration {

            val instance = createBaseConfiguration<OpcuaWriterConfiguration>(
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
                awsIotCredentialProviderClients = emptyMap(),
                secretsManagerConfiguration = secretsManagerConfiguration,
                templates = templatesConfiguration,
                monitorIncludedConfigFilesInterval = monitorIncludedConfigFilesInterval,
                monitorIncludedConfigFiles = monitorIncludedConfigFiles
            )

            instance._targets = targets
            instance._transformations = transformations
            return instance
        }

    }

}