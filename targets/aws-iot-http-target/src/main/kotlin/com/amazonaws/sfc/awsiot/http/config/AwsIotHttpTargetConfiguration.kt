/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.awsiot.http.config

import com.amazonaws.sfc.awsiot.http.config.AwsIotHttpWriterConfiguration.Companion.AWS_IOT_HTTP_TARGET
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_REGION
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_TARGETS
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.google.gson.annotations.SerializedName
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient

/**
 * AWS IoT data plane topic target configuration.
 **/
@ConfigurationClass
class AwsIotHttpTargetConfiguration : AwsServiceConfig, TargetConfiguration() {
    @SerializedName(CONFIG_TOPIC_NAME)
    private var _topicName: String? = null

    /**
     * Name of the topic
     */
    val topicName: String?
        get() = _topicName


    @SerializedName(CONFIG_REGION)
    private var _region: String? = null

    /**
     * Region of the topic.
     */
    override val region: Region?
        get() = if (_region.isNullOrEmpty()) null else Region.of(_region!!.lowercase())

    /**
     * Validates topic configuration, throws ConfigurationException if it is invalid.
     * @throws ConfigurationException
     */
    override fun validate() {
        if (validated) return

        super.validate()
        validateRegion(_region)
        validated = true

    }

    // tests is region is valid, throws ConfigurationException if it is not valid
    private fun validateRegion(_region: String?) {
        if (!_region.isNullOrEmpty()) {

            val iotServiceRegions = IotDataPlaneClient.serviceMetadata().regions().map { it.id().lowercase() }

            ConfigurationException.check(
                (_region.lowercase() in iotServiceRegions),
                "Region \"$_region\" is not valid, valid regions are ${iotServiceRegions.joinToString()} ",
                CONFIG_TARGETS,
                this
            )
        }
    }

    companion object {
        private const val CONFIG_TOPIC_NAME = "TopicName"

        private val default = AwsIotHttpTargetConfiguration()

        fun create(topicName: String? = default._topicName,
                   region: String? = default._region,
                   description: String = default._description,
                   active: Boolean = default._active,
                   template: String? = default._template,
                   targetServer: String? = default._server,
                   metrics: MetricsSourceConfiguration = default._metrics,
                   credentialProviderClient: String? = default._credentialProvideClient): AwsIotHttpTargetConfiguration {

            val instance = createTargetConfiguration<AwsIotHttpTargetConfiguration>(description = description,
                active = active,
                targetType = AWS_IOT_HTTP_TARGET,
                template = template,
                targetServer = targetServer,
                metrics = metrics,
                credentialProviderClient = credentialProviderClient) as AwsIotHttpTargetConfiguration

            with(instance) {
                _topicName = topicName
                _region = region
            }
            return instance
        }
    }
}

