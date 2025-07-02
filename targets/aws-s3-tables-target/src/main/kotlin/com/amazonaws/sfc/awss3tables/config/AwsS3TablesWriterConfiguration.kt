// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awss3tables.config

import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.client.AwsServiceTargetsConfig
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.ChannelConfiguration.Companion.CONFIG_TRANSFORMATION
import com.amazonaws.sfc.filters.ValueFilterConfiguration
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.transformations.Transformation
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

// AWS S3 target configuration
@ConfigurationClass
class AwsS3TablesWriterConfiguration : AwsServiceTargetsConfig<AwsS3TablesTargetConfiguration>, BaseConfigurationWithMetrics() {

    @SerializedName(CONFIG_TARGETS)
    private var _targets: Map<String, AwsS3TablesTargetConfiguration> = emptyMap()
    override val targets: Map<String, AwsS3TablesTargetConfiguration>
        get() = _targets.filter { (it.value.targetType == AWS_S3_TABLES) }

    @SerializedName(CONFIG_TRANSFORMATIONS)
    private var _transformations = mapOf<String, Transformation>()
    val transformations: Map<String, Transformation>
        get() = _transformations

    @SerializedName(CONFIG_VALUE_FILTERS)
    private var _valueFilters = mapOf<String, ValueFilterConfiguration>()

    /**
     * All configured Value  filters
     */
    val valueFilters: Map<String, ValueFilterConfiguration>
        get() = _valueFilters



    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return
        super.validate()
        targets.forEach {
            it.value.validate()
        }

        validateMappingTransformations()

        validated = true
    }

    private fun validateMappingTransformations() {
        targets.forEach { (tableName, targetConfig) ->
            targetConfig.tables.forEach { tableConfig ->
                tableConfig.mappings.forEachIndexed { mappingIndex, tableMapping ->
                    tableMapping.forEach { (fieldName, fieldMapping) ->
                        if (fieldMapping.transformationID != null) {
                            if (fieldMapping.transformationID !in _transformations.keys) {
                                throw ConfigurationException(
                                    "Transformation ${fieldMapping.transformationID} for table \"$tableName\", mapping $mappingIndex, field \"$fieldName\" not found in $CONFIG_TRANSFORMATIONS, configured transformations are ${transformations.keys}",
                                    CONFIG_TRANSFORMATION,
                                    "")
                            }
                            fieldMapping.subMappings.forEach { (subFieldName, subFieldMapping) ->
                                if (subFieldMapping.transformationID !in _transformations.keys) {
                                    throw ConfigurationException(
                                        "Transformation ${subFieldMapping.transformationID} for table \"$tableName\", mapping $mappingIndex, sub-field \"$fieldName.$subFieldName\",  not found in $CONFIG_TRANSFORMATIONS, configured transformations are ${transformations.keys}",
                                        CONFIG_TRANSFORMATION,
                                        "")
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    companion object {
        const val AWS_S3_TABLES = "AWS-S3-TABLES"
        private val default = AwsS3TablesWriterConfiguration()

        fun create(targets: Map<String, AwsS3TablesTargetConfiguration> = default._targets,
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
                   templatesConfiguration: TemplatesConfiguration? = default._templates): AwsS3TablesWriterConfiguration {

            val instance = createBaseConfiguration<AwsS3TablesWriterConfiguration>(
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

            instance._targets = targets
            return instance
        }


    }


}