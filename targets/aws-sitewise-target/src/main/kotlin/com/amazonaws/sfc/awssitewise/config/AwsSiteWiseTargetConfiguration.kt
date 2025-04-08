
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awssitewise.config

import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseWriterConfiguration.Companion.AWS_SITEWISE
import com.amazonaws.sfc.config.AwsServiceConfig
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_BATCH_SIZE
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_ENDPOINT
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_INTERVAL
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_REGION
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.TargetConfiguration
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
    @SerializedName(CONFIG_ASSETS)
    private var _assets: List<AwsSiteWiseAssetConfiguration> = emptyList()

    val assets: List<AwsSiteWiseAssetConfiguration>
        get() {
            return _assets
        }

    @SerializedName(CONFIG_ASSET_CREATION)
    private var _assetCreationConfiguration: AwsSiteWiseAssetCreationConfiguration? = null
    val assetCreationConfiguration: AwsSiteWiseAssetCreationConfiguration?
        get() =  _assetCreationConfiguration

    /**
     * AWS region
     */
    @SerializedName(CONFIG_REGION)
    private var _region: String? = null


    override val region: Region?
        get() = if (_region.isNullOrEmpty()) null else Region.of(_region!!.lowercase())

    @SerializedName(CONFIG_ENDPOINT)
    var _endPoint : String? = null
    override val endpoint : String?
        get() = _endPoint

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
        assets.forEach { it.validate() }
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
        private const val CONFIG_ASSETS = "Assets"
        private const val CONFIG_ASSET_CREATION = "AssetCreation"

        private val default = AwsSiteWiseTargetConfiguration()

        fun create(assets: List<AwsSiteWiseAssetConfiguration> = default.assets,
                   assetCreation : AwsSiteWiseAssetCreationConfiguration? = default._assetCreationConfiguration,
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
                _assetCreationConfiguration = assetCreation
                _region = region
                _batchSize = batchSize
                _interval = interval
            }
            return instance
        }

        const val ID_REGEX_STR = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
        val ID_REGEX = ID_REGEX_STR.toRegex()


    }

}


