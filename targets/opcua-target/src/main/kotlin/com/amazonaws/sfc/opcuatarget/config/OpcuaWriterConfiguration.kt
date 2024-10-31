
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.opcuatarget.config

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.transformations.Transformation
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * AWS IoT Core target configuration
 */
@ConfigurationClass
class OpcuaWriterConfiguration : BaseConfigurationWithMetrics(), Validate{


    @SerializedName(CONFIG_TARGETS)
    private var _targets: Map<String, OpcuaTargetConfiguration> = emptyMap()
    val targets: Map<String, OpcuaTargetConfiguration>
        get() = _targets.filter { (it.value.targetType == OPCUA_TARGET) }


    @SerializedName(CONFIG_TRANSFORMATIONS)
    private var _transformations = mapOf<String, Transformation>()
    val transformations: Map<String, Transformation>
        get() = _transformations



    /**
     * Validates configuration.
     * @throws ConfigurationException
     */
    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return

        super.validate()
        targets.forEach {
            it.value.validate()
        }
        validated = true

    }

    companion object {
        const val OPCUA_TARGET = "OPCUA-TARGET"

        private val default = OpcuaWriterConfiguration()

        fun create(targets: Map<String, OpcuaTargetConfiguration> = default._targets,
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
                   secretsManagerConfiguration: SecretsManagerConfiguration? = default._secretsManagerConfiguration,
                   monitorIncludedConfigFiles: Boolean = default._monitorIncludedConfigFiles,
                   monitorIncludedConfigFilesInterval : Duration = default._monitorIncludedConfigFilesInterval.toDuration(DurationUnit.SECONDS),
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
            return instance
        }

    }

}