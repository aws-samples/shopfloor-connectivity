
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awssitewise.config

import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.client.AwsServiceTargetsConfig
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.log.LogLevel
import com.google.gson.annotations.SerializedName

/**
 * AWS Sitewise target configuration
 * @see AwsSiteWiseTargetConfiguration
 */
@ConfigurationClass
class AwsSiteWiseWriterConfiguration : AwsServiceTargetsConfig<AwsSiteWiseTargetConfiguration>, BaseConfigurationWithMetrics(), Validate {

    @SerializedName(CONFIG_TARGETS)
    private var _targets: Map<String, AwsSiteWiseTargetConfiguration> = emptyMap()

    /**
     * Configured Sitewise target assets.
     */
    override val targets: Map<String, AwsSiteWiseTargetConfiguration>
        get() = _targets.filter { (it.value.targetType == AWS_SITEWISE) }

    /**
     * Validates configuration.
     * @throws ConfigurationException
     */
    override fun validate() {
        if (validated) return
        super.validate()
        targets.forEach {
            it.value.validate()
        }
        validated = true
    }

    companion object {
        const val AWS_SITEWISE = "AWS-SITEWISE"

        private val default = AwsSiteWiseWriterConfiguration()

        fun create(targets: Map<String, AwsSiteWiseTargetConfiguration> = default._targets,
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
                   secretsManagerConfiguration: SecretsManagerConfiguration? = default._secretsManagerConfiguration): AwsSiteWiseWriterConfiguration {

            val instance = createBaseConfiguration<AwsSiteWiseWriterConfiguration>(
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

            instance._targets = targets
            return instance
        }
    }


}