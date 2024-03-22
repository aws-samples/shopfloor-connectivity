
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsiotcore.config

import com.amazonaws.sfc.awsiotcore.config.AwsIotCoreWriterConfiguration.Companion.AWS_IOT_CORE_TARGET
import com.amazonaws.sfc.config.AwsServiceConfig
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_REGION
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_TARGETS
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.TargetConfiguration
import com.amazonaws.sfc.data.Compress
import com.amazonaws.sfc.data.CompressionType
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.google.gson.annotations.SerializedName
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * AWS IoT data plane topic target configuration.
 **/
@ConfigurationClass
class AwsIotCoreTargetConfiguration : AwsServiceConfig, TargetConfiguration() {
    @SerializedName(CONFIG_TOPIC_NAME)
    private var _topicName: String? = null

    /**
     * Name of the topic
     */
    val topicName: String
        get() = _topicName ?:""


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
        validateRegion()
        validateTopic()
        validated = true

    }

    private fun validateTopic() {

            ConfigurationException.check(
                (!_topicName.isNullOrEmpty()),
                "$CONFIG_TOPIC_NAME is required",
                CONFIG_TOPIC_NAME,
                this
            )

        ConfigurationException.check(
            (!_topicName!!.startsWith("$")),
            "$CONFIG_TOPIC_NAME cannot start with \"$\"",
            CONFIG_TOPIC_NAME,
            this
        )

    }

    // tests is region is valid, throws ConfigurationException if it is not valid
    private fun validateRegion() {
        if (!_region.isNullOrEmpty()) {

            val iotServiceRegions = IotDataPlaneClient.serviceMetadata().regions().map { it.id().lowercase() }

            ConfigurationException.check(
                (_region!!.lowercase() in iotServiceRegions),
                "Region \"$_region\" is not valid, valid regions are ${iotServiceRegions.joinToString()} ",
                CONFIG_TARGETS,
                this
            )
        }
    }

    @SerializedName(CONFIG_BATCH_COUNT)
    private var _batchCount: Int? = null
    val batchCount
        get() = _batchCount

    @SerializedName(CONFIG_BATCH_SIZE)
    private var _batchSize: Int? = null
    val batchSize
        get() = if (_batchSize != null) _batchSize!! * 1024 else null

    @SerializedName(CONFIG_BATCH_INTERVAL)
    private var _batchInterval: Int? = null
    val batchInterval : Duration
        get() = _batchInterval?.toDuration(DurationUnit.MILLISECONDS) ?: Duration.INFINITE

    @SerializedName(Compress.CONFIG_COMPRESS)
    private var _compressionType: CompressionType? = null

    val compressionType: CompressionType
        get() = _compressionType ?: CompressionType.NONE

    companion object {
        private const val CONFIG_TOPIC_NAME = "TopicName"
        const val CONFIG_BATCH_SIZE = "BatchSize"
        const val CONFIG_BATCH_COUNT = "BatchCount"
        const val CONFIG_BATCH_INTERVAL = "BatchInterval"

        private val default = AwsIotCoreTargetConfiguration()

        fun create(topicName: String? = default._topicName,
                   region: String? = default._region,
                   description: String = default._description,
                   active: Boolean = default._active,
                   template: String? = default._template,
                   targetServer: String? = default._server,
                   metrics: MetricsSourceConfiguration = default._metrics,
                   credentialProviderClient: String? = default._credentialProvideClient,
                   batchCount : Int? = default.batchCount,
                   batchSize : Int? = default._batchSize,
                   batchInterval : Int? = default._batchInterval): AwsIotCoreTargetConfiguration {

            val instance = createTargetConfiguration<AwsIotCoreTargetConfiguration>(description = description,
                active = active,
                targetType = AWS_IOT_CORE_TARGET,
                template = template,
                targetServer = targetServer,
                metrics = metrics,
                credentialProviderClient = credentialProviderClient) as AwsIotCoreTargetConfiguration

            with(instance) {
                _topicName = topicName
                _region = region
                _batchCount = batchCount
                _batchSize= batchSize
                _batchInterval = batchInterval
            }
            return instance
        }
    }
}

