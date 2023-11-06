
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.storeforward.config

import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.log.LogLevel
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class StoreForwardWriterConfiguration : BaseConfigurationWithMetrics() {

    @SerializedName(CONFIG_TARGETS)
    private var _targets: Map<String, StoreForwardTargetConfiguration> = emptyMap()

    val forwardingTargets: Map<String, StoreForwardTargetConfiguration>
        get() = _targets.filter { (it.value.targetType == STORE_FORWARD) }

    val forwardedTargets: Map<String, TargetConfiguration>
        get() {
            val targetKeys = forwardingTargets.values.flatMap { it.subTargets ?: emptyList() }
            return _targets.filter { target -> targetKeys.contains(target.key) }
        }

    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return
        super.validate()
        forwardingTargets.forEach {
            it.value.validate()
        }
        validated = true
    }

    companion object {
        const val STORE_FORWARD = "STORE-FORWARD"

        private val default = StoreForwardWriterConfiguration()

        fun create(targets: Map<String, StoreForwardTargetConfiguration> = default._targets,
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
                   secretsManagerConfiguration: SecretsManagerConfiguration? = default._secretsManagerConfiguration): StoreForwardWriterConfiguration {

            val instance = createBaseConfiguration<StoreForwardWriterConfiguration>(
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