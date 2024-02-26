
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awss3.config

import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.client.AwsServiceTargetsConfig
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.log.LogLevel
import com.google.gson.annotations.SerializedName

// AWS S3 target configuration
@ConfigurationClass
class AwsS3WriterConfiguration : AwsServiceTargetsConfig<AwsS3TargetConfiguration>, BaseConfigurationWithMetrics() {

    @SerializedName(CONFIG_TARGETS)
    private var _targets: Map<String, AwsS3TargetConfiguration> = emptyMap()
    override val targets: Map<String, AwsS3TargetConfiguration>
        get() = _targets.filter { (it.value.targetType == AWS_S3) }

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
        const val AWS_S3 = "AWS-S3"
        private val default = AwsS3WriterConfiguration()

        fun create(targets: Map<String, AwsS3TargetConfiguration> = default._targets,
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
                   secretsManagerConfiguration: SecretsManagerConfiguration? = default._secretsManagerConfiguration): AwsS3WriterConfiguration {

            val instance = createBaseConfiguration<AwsS3WriterConfiguration>(
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
                tuningConfiguration = TuningConfiguration())

            instance._targets = targets
            return instance
        }


    }


}