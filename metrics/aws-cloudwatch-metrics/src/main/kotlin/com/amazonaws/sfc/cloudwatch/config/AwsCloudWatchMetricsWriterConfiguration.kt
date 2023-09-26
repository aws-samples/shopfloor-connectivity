/*
 Copyright (c) 2022. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.cloudwatch.config


import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.metrics.MetricsConfiguration.Companion.CONFIG_METRICS
import com.google.gson.annotations.SerializedName

class AwsCloudWatchMetricsWriterConfiguration : BaseConfiguration(), Validate {

    @SerializedName(CONFIG_METRICS)
    private var _metrics: AwsCloudWatchMetricsConfiguration? = null

    val metrics: AwsCloudWatchMetricsConfiguration?
        get() = _metrics

    override fun validate() {
        if (validated) return

        ConfigurationException.check(
            (_metrics?.cloudWatch?.credentialProviderClient == null || awsCredentialServiceClients.containsKey(_metrics!!.cloudWatch.credentialProviderClient)),
            "$CONFIG_CREDENTIAL_PROVIDER_CLIENT \"${_metrics?.cloudWatch?.credentialProviderClient}\" does not exist, available clients are ${awsCredentialServiceClients.keys}",
            CONFIG_CREDENTIAL_PROVIDER_CLIENT,
            this)
    }

    companion object {
        private val default = AwsCloudWatchMetricsWriterConfiguration()

        fun create(metrics: AwsCloudWatchMetricsConfiguration? = default._metrics,
                   name: String = default._name,
                   version: String = default._version,
                   aWSVersion: String? = default._awsVersion,
                   description: String = default._description,
                   schedules: List<ScheduleConfiguration> = default._schedules,
                   logLevel: LogLevel? = default._logLevel,
                   metadata: Map<String, String> = default._metadata,
                   elementNamesConfiguration: ElementNamesConfiguration = default._elementNames,
                   targetServers: Map<String, ServerConfiguration> = default._targetServers,
                   targetTypes: Map<String, InProcessConfiguration> = default._targetTypes,
                   adapterServers: Map<String, ServerConfiguration> = default._protocolAdapterServers,
                   adapterTypes: Map<String, InProcessConfiguration> = default._protocolTypes,
                   awsIotCredentialProviderClients: Map<String, AwsIotCredentialProviderClientConfiguration> = default._awsIoTCredentialProviderClients,
                   secretsManager: SecretsManagerConfiguration? = default._secretsManagerConfiguration): AwsCloudWatchMetricsWriterConfiguration {

            val instance = AwsCloudWatchMetricsWriterConfiguration()
            with(instance) {
                _metrics = metrics
                _name = name
                _version = version
                _awsVersion = aWSVersion
                _description = description
                _schedules = schedules
                _logLevel = logLevel
                _metadata = metadata
                _elementNames = elementNamesConfiguration
                _targetServers = targetServers
                _targetTypes = targetTypes
                _protocolAdapterServers = adapterServers
                _protocolTypes = adapterTypes
                _awsIoTCredentialProviderClients = awsIotCredentialProviderClients
                _secretsManagerConfiguration = secretsManager
            }
            return instance
        }
    }
}


