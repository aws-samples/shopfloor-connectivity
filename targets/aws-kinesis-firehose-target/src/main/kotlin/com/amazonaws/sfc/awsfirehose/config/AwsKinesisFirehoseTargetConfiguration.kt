
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsfirehose.config

import com.amazonaws.sfc.awsfirehose.config.AwsFirehoseWriterConfiguration.Companion.AWS_KINESIS_FIREHOSE
import com.amazonaws.sfc.config.AwsServiceConfig
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_BATCH_SIZE
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_REGION
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.TargetConfiguration
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.google.gson.annotations.SerializedName
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.firehose.FirehoseClient

/**
 * AWS Firehose Stream target configuration.
 */
@ConfigurationClass
class AwsKinesisFirehoseTargetConfiguration : AwsServiceConfig, TargetConfiguration() {
    @SerializedName(CONFIG_STREAM_NAME)
    private var _streamName: String? = null

    /**
     * Name of the Firehose stream
     */
    val streamName: String?
        get() = _streamName

    @SerializedName(CONFIG_REGION)
    private var _region: String? = null

    /**
     * Name of the region
     */
    override val region: Region?
        get() = if (_region.isNullOrEmpty()) null else Region.of(_region!!.lowercase())

    @SerializedName(CONFIG_BATCH_SIZE)
    private var _batchSize: Int = DEFAULT_BATCH_SIZE

    /**
     * Batch size of messages to write to the stream
     */
    val batchSize: Int
        get() = minOf(_batchSize, FIREHOSE_MAX_BATCH_MESSAGES)

    /**
     * Validates the configuration
     * @throws ConfigurationException
     */
    override fun validate() {
        if (validated) return
        super.validate()
        validateRegion(_region)
        validateStream()
        validateBatchSize()
        validated = true

    }

    // Validates batch size
    private fun validateBatchSize() =
        ConfigurationException.check(
            (batchSize <= FIREHOSE_MAX_BATCH_MESSAGES),
            "Max batch size for Kinesis Firehose Target is $FIREHOSE_MAX_BATCH_MESSAGES",
            CONFIG_BATCH_SIZE,
            this
        )


    // Validates stream name
    private fun validateStream() =
        ConfigurationException.check(
            (_streamName != null),
            "Delivery stream name for Kinesis Firehose Target must be specified",
            CONFIG_STREAM_NAME,
            this
        )


    // Validates region
    private fun validateRegion(_region: String?) {
        if (!_region.isNullOrEmpty()) {
            val validRegions = FirehoseClient.serviceMetadata().regions().map { it.id().lowercase() }

            ConfigurationException.check(
                (_region.lowercase() in validRegions),
                "Region \"$_region\" is not valid, valid regions are ${validRegions.joinToString()} ",
                CONFIG_REGION,
                this
            )
        }
    }

    companion object {
        private const val FIREHOSE_MAX_BATCH_MESSAGES = 500
        private const val DEFAULT_BATCH_SIZE = 10
        private const val CONFIG_STREAM_NAME = "StreamName"

        private val default = AwsKinesisFirehoseTargetConfiguration()

        fun create(streamName: String? = default._streamName,
                   region: String? = default._region,
                   batchSize: Int = default._batchSize,
                   description: String = default._description,
                   active: Boolean = default._active,
                   template: String? = default._template,
                   targetServer: String? = default._server,
                   metrics: MetricsSourceConfiguration = default._metrics,
                   credentialProviderClient: String? = default._credentialProvideClient): AwsKinesisFirehoseTargetConfiguration {

            val instance = createTargetConfiguration<AwsKinesisFirehoseTargetConfiguration>(description = description,
                active = active,
                targetType = AWS_KINESIS_FIREHOSE,
                template = template,
                targetServer = targetServer,
                metrics = metrics,
                credentialProviderClient = credentialProviderClient) as AwsKinesisFirehoseTargetConfiguration


            with(instance) {
                _streamName = streamName
                _region = region
                _batchSize = batchSize
            }
            return instance
        }

    }


}