
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.config

import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.config.ServerConfiguration.Companion.CONFIG_SERVER_ADDRESS
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.metrics.MetricsConfiguration
import com.google.gson.annotations.SerializedName

/**
 * Base class for services that run an SFC source, target or the SC core
 */
@ConfigurationClass
open class ServiceConfiguration : BaseConfiguration() {


    @SerializedName(CONFIG_TARGETS)
    protected var _targets: Map<String, TargetConfiguration> = emptyMap()

    /**
     * Output targets
     */
    val targets: Map<String, TargetConfiguration>
        get() = _targets


    /**
     * Active Output targets
     */
    val activeTargets: Map<String, TargetConfiguration>
        get() = _targets.filter { it.value.active }


    @SerializedName(CONFIG_PROTOCOL_ADAPTERS)
    @Suppress("PropertyName")
    protected var _protocols: Map<String, ProtocolAdapterConfiguration> = emptyMap()

    val protocolAdapters: Map<String, ProtocolAdapterConfiguration>
        get() = _protocols

    @SerializedName(MetricsConfiguration.CONFIG_METRICS)
    private val _metrics: MetricsConfiguration? = null

    val metrics: MetricsConfiguration?
        get() = _metrics

    /**
     * Validates configuration
     */
    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return

        super.validate()

        protocolAdapters.values.forEach {
            it.validate()
        }

        targets.values.forEach {
            it.validate()
        }

        validateTargetServerUsedBySingleTargetOnly()
        validateTargetServerAddressesAreUnique()
        validateProtocolAdapterServerAddressesAreUnique()


        validated = true

    }

    private fun validateTargetServerAddressesAreUnique() {
        validateServerAddressesAreUnique(targetServers, "target")
    }

    private fun validateProtocolAdapterServerAddressesAreUnique() {
        validateServerAddressesAreUnique(protocolAdapterServers, "protocol adapter")
    }


    private fun validateServerAddressesAreUnique(servers: Map<String, ServerConfiguration>, type: String) {
        servers.forEach { server ->
            val serversUsingAddress = servers.filter {
                (server.value.addressStr == it.value.addressStr)
            }
            if (serversUsingAddress.size > 1) {
                throw ConfigurationException("Address ${server.value.addressStr} of $type server ${server.key} is used by multiple servers (${serversUsingAddress.keys}, addresses for each server must be unique", CONFIG_PROTOCOL_SERVERS)
            }
        }
    }

    private fun validateTargetServerUsedBySingleTargetOnly() {
        targetServers.forEach { server ->
            val usedByServers = allServersUsingAddress(server)
            if (usedByServers.size > 1) {
                throw ConfigurationException("Address ${server.value.addressStr} of protocol adapter server ${server.key} is used by multiple servers (${usedByServers.keys}, addresses for each server must be unique", CONFIG_SERVER_ADDRESS)
            }
        }
    }


    private fun allServersUsingAddress(server: Map.Entry<String, ServerConfiguration>): Map<String, ServerConfiguration> {
        val allServersUsingAddress =
            targetServers.filter {
                (server.value.addressStr == it.value.addressStr)
            } +
            protocolAdapterServers.filter {
                (server.value.addressStr == it.value.addressStr)
            }
        return allServersUsingAddress
    }

    companion object {

        protected val default = ServiceConfiguration()

        @JvmStatic
        protected inline fun <reified T : ServiceConfiguration> createServiceConfiguration(targets: Map<String, TargetConfiguration>,
                                                                                           protocolAdapters: Map<String, ProtocolAdapterConfiguration>,
                                                                                           name: String,
                                                                                           version: String,
                                                                                           awsVersion: String?,
                                                                                           description: String,
                                                                                           schedules: List<ScheduleConfiguration>,
                                                                                           logLevel: LogLevel?,
                                                                                           metadata: Map<String, String>,
                                                                                           elementNames: ElementNamesConfiguration,
                                                                                           targetServers: Map<String, ServerConfiguration>,
                                                                                           targetTypes: Map<String, InProcessConfiguration>,
                                                                                           adapterServers: Map<String, ServerConfiguration>,
                                                                                           adapterTypes: Map<String, InProcessConfiguration>,
                                                                                           awsIotCredentialProviderClients: Map<String, AwsIotCredentialProviderClientConfiguration>,
                                                                                           secretsManagerConfiguration: SecretsManagerConfiguration?): T {

            val instance = createBaseConfiguration<T>(
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
                tuningConfiguration = tuningConfiguration)

            with(instance) {
                _targets = targets
                _protocols = protocolAdapters
            }
            return instance
        }

        fun create(targets: Map<String, TargetConfiguration> = default._targets,
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
                   awsIotCredentialProviderClients: Map<String, AwsIotCredentialProviderClientConfiguration> = default._awsIoTCredentialProviderClients,
                   secretsManagerConfiguration: SecretsManagerConfiguration? = default._secretsManagerConfiguration): ServiceConfiguration =
            createServiceConfiguration(targets = targets,
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


    }


}