/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.awstimestream.config

import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.client.AwsServiceTargetsConfig
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.data.JmesPathExtended
import com.amazonaws.sfc.log.LogLevel
import com.google.gson.annotations.SerializedName
import io.burt.jmespath.Expression

/**
 * AWS Timestream target configuration
 * @see AwsTimestreamWriterConfiguration
 */
@ConfigurationClass
class AwsTimestreamWriterConfiguration : AwsServiceTargetsConfig<AwsTimestreamTargetConfiguration>, BaseConfigurationWithMetrics(), Validate {

    @SerializedName(CONFIG_TARGETS)
    private var _targets: Map<String, AwsTimestreamTargetConfiguration> = emptyMap()

    /**
     * Configured Timestream target values
     */
    override val targets: Map<String, AwsTimestreamTargetConfiguration>
        get() = _targets.filter { (it.value.targetType == AWS_TIMESTREAM) }


    /**
     * Validates configuration.
     * @throws ConfigurationException
     */
    override fun validate() {

        super.validate()
        targets.forEach {
            it.value.validate()
        }
    }

    companion object {

        const val AWS_TIMESTREAM = "AWS-TIMESTREAM"

        fun getExpression(path: String?): Expression<Any>? {
            if (path.isNullOrEmpty()) {
                return null
            }

            val p: String = JmesPathExtended.escapeJMesString(path)
            if (!cachedJmespathQueries.containsKey(p)) {
                cachedJmespathQueries[p] = try {
                    jmespath.compile(if (p.startsWith("@.")) p else "@.$p")
                } catch (e: Throwable) {
                    null
                }
            }
            return cachedJmespathQueries[p]
        }

        private val jmespath by lazy {
            JmesPathExtended.create()
        }

        // Caching compiled JMESPath queries
        private val cachedJmespathQueries = mutableMapOf<String, Expression<Any>?>()

        private val default = AwsTimestreamWriterConfiguration()

        fun create(targets: Map<String, AwsTimestreamTargetConfiguration> = default._targets,
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
                   secretsManagerConfiguration: SecretsManagerConfiguration? = default._secretsManagerConfiguration): AwsTimestreamWriterConfiguration {

            val instance = createBaseConfiguration<AwsTimestreamWriterConfiguration>(
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


