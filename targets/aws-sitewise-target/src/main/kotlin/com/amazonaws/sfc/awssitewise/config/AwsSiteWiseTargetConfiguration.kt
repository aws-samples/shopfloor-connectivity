/*
 Copyright (c) 2021. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.awssitewise.config

import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseWriterConfiguration.Companion.AWS_SITEWISE
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_BATCH_SIZE
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_INTERVAL
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_REGION
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.google.gson.annotations.SerializedName
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.iotsitewise.IoTSiteWiseClient
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * AWS SiteWise target assets configuration
 */
@ConfigurationClass
class AwsSiteWiseTargetConfiguration : AwsServiceConfig, TargetConfiguration() {
    @SerializedName("Assets")
    private var _assets: List<AwsSiteWiseAssetConfiguration> = emptyList()

    val assets: List<AwsSiteWiseAssetConfiguration>
        get() {
            return _assets
        }

    /**
     * AWS region
     */
    @SerializedName(CONFIG_REGION)
    private var _region: String? = null


    override val region: Region?
        get() = if (_region.isNullOrEmpty()) null else Region.of(_region!!.lowercase())

    @SerializedName(CONFIG_BATCH_SIZE)
    private var _batchSize: Int = DEFAULT_BATCH_SIZE

    /**
     * Number of reads to combine in a batch before writing to SiteWise service
     */
    val batchSize: Int
        get() = _batchSize

    @SerializedName(CONFIG_INTERVAL)
    private var _interval: Int? = null

    /**
     * Interval for sending messages to the stream.
     */
    val interval: Duration
        get() = _interval?.toDuration(DurationUnit.MILLISECONDS) ?: Duration.INFINITE


    /**
     * Validates configuration
     * @throws ConfigurationException
     */
    override fun validate() {

        if (validated) return
        super.validate()
        validateInterval()
        validateRegion()
        validated = true
    }

    // Validates AWS region
    private fun validateRegion() {
        if (!_region.isNullOrEmpty()) {
            val validRegions = IoTSiteWiseClient.serviceMetadata().regions().map { it.id() }
            ConfigurationException.check(
                // 25 Oct 2021, IoTSiteWiseClient does not implement metadata, adding OR condition to skip check
                (_region!!.lowercase() in validRegions || validRegions.isEmpty()),
                "$CONFIG_REGION \"$_region\" is not valid, valid regions are ${validRegions.joinToString()} ",
                CONFIG_REGION,
                this
            )
        }
    }

    // Validates the interval
    private fun validateInterval() =
        ConfigurationException.check(
            (_interval == null || _interval!! > 10),
            "Interval must be 10 or more",
            CONFIG_INTERVAL,
            this)


    companion object {
        private const val DEFAULT_BATCH_SIZE = 10

        private val default = AwsSiteWiseTargetConfiguration()

        fun create(assets: List<AwsSiteWiseAssetConfiguration> = default.assets,
                   region: String? = default._region,
                   batchSize: Int = default._batchSize,
                   interval: Int? = default._interval,
                   description: String = default._description,
                   active: Boolean = default._active,
                   template: String? = default._template,
                   targetServer: String? = default._server,
                   metrics: MetricsSourceConfiguration = default._metrics,
                   credentialProviderClient: String? = default._credentialProvideClient): AwsSiteWiseTargetConfiguration {

            val instance = createTargetConfiguration<AwsSiteWiseTargetConfiguration>(description = description,
                active = active,
                targetType = AWS_SITEWISE,
                template = template,
                targetServer = targetServer,
                metrics = metrics,
                credentialProviderClient = credentialProviderClient) as AwsSiteWiseTargetConfiguration

            with(instance) {
                _assets = assets
                _region = region
                _batchSize = batchSize
                _interval = interval
            }
            return instance
        }


    }

}


