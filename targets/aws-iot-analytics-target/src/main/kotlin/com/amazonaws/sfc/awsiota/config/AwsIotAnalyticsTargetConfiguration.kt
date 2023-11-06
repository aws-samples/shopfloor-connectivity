
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsiota.config

import com.amazonaws.sfc.awsiota.config.AwsIotAnalyticsWriterConfiguration.Companion.AWS_IOT_ANALYTICS
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_BATCH_SIZE
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_REGION
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.google.gson.annotations.SerializedName
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.iotanalytics.IoTAnalyticsClient

/**
 * AWS IoT Analytics target channel configuration
 */
@ConfigurationClass
class AwsIotAnalyticsTargetConfiguration : AwsServiceConfig, TargetConfiguration() {
    @SerializedName(CONFIG_CHANNEL_NAME)
    private var _channelName: String? = null

    /**
     * Name of the channel
     */
    val channelName: String?
        get() = _channelName

    @SerializedName(CONFIG_REGION)
    private var _region: String? = null

    /**
     * AWS region
     */
    override val region: Region?
        get() = if (_region.isNullOrEmpty()) null else Region.of(_region!!.lowercase())

    @SerializedName(CONFIG_BATCH_SIZE)
    private var _batchSize: Int = DEFAULT_BATCH_SIZE

    /**
     * Number of messages in a batch when writing to channel
     */
    val batchSize: Int
        get() = _batchSize

    /**
     * Validates configuration
     * @throws ConfigurationException
     */
    override fun validate() {

        if (validated) return

        super.validate()
        validateRegion(_region)
        validateStream()
        validated = true

    }

    // Validates channel name
    private fun validateStream() =
        ConfigurationException.check(
            (_channelName != null),
            "$CONFIG_CHANNEL_NAME for IoT Analytics Target must be specified",
            CONFIG_CHANNEL_NAME,
            this
        )


    // Validates AWS region
    private fun validateRegion(_region: String?) {
        if (!_region.isNullOrEmpty()) {
            val validRegions = IoTAnalyticsClient.serviceMetadata().regions().map { it.id() }
            ConfigurationException.check(
                (_region.lowercase() in validRegions),
                "Region \"$_region\" is not valid, valid regions are ${validRegions.joinToString()} ",
                CONFIG_REGION,
                this
            )
        }
    }

    companion object {
        private const val CONFIG_CHANNEL_NAME = "ChannelName"
        private const val DEFAULT_BATCH_SIZE = 10


        private val default = AwsIotAnalyticsTargetConfiguration()

        fun create(channelName: String? = default._channelName,
                   region: String? = default._region,
                   batchSize: Int = default._batchSize,
                   description: String = default._description,
                   active: Boolean = default._active,
                   template: String? = default._template,
                   targetServer: String? = default._server,
                   metrics: MetricsSourceConfiguration = default._metrics,
                   credentialProviderClient: String? = default._credentialProvideClient): AwsIotAnalyticsTargetConfiguration {

            val instance = createTargetConfiguration<AwsIotAnalyticsTargetConfiguration>(description = description,
                active = active,
                targetType = AWS_IOT_ANALYTICS,
                template = template,
                targetServer = targetServer,
                metrics = metrics,
                credentialProviderClient = credentialProviderClient) as AwsIotAnalyticsTargetConfiguration

            with(instance) {
                _channelName = channelName
                _region = region
                _batchSize = batchSize
            }
            return instance
        }

    }
}