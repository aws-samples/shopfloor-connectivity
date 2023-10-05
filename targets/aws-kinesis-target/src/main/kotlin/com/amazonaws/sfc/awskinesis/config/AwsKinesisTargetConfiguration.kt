/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.awskinesis.config

import com.amazonaws.sfc.awskinesis.config.AwsKinesisWriterConfiguration.Companion.AWS_KINESIS_TARGET
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_BATCH_SIZE
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_INTERVAL
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_REGION
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_TARGETS
import com.amazonaws.sfc.data.Compress
import com.amazonaws.sfc.data.CompressionType
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.google.gson.annotations.SerializedName
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kinesis.KinesisClient
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * AWS Kinesis target stream config
 */
@ConfigurationClass
class AwsKinesisTargetConfiguration : AwsServiceConfig, TargetConfiguration() {
    @SerializedName(CONFIG_STREAM_NAME)
    /**
     * Name of the Kinesis stream.
     */
    private var _streamName: String? = null
    val streamName: String?
        get() = _streamName

    @SerializedName(CONFIG_REGION)
    private var _region: String? = null

    /**
     * AWS Region.
     */
    override val region: Region?
        get() = if (_region.isNullOrEmpty()) null else Region.of(_region!!.lowercase())

    @SerializedName(CONFIG_BATCH_SIZE)
    private var _batchSize: Int = DEFAULT_BATCH_SIZE

    /**
     * Batch size for sending messages to the stream.
     */
    val batchSize: Int
        get() = minOf(_batchSize, KINESIS_MAX_BATCH_MESSAGES)

    @SerializedName(CONFIG_INTERVAL)
    private var _interval: Int? = null

    /**
     * Interval  for sending messages to the stream.
     */
    val interval: Duration
        get() = _interval?.toDuration(DurationUnit.MILLISECONDS) ?: Duration.INFINITE


    @SerializedName(Compress.CONFIG_COMPRESS)
    private var _compressionType: CompressionType? = null

    val compressionType: CompressionType
        get() = _compressionType ?: CompressionType.NONE

    /**
     * Validates the configuration.
     * @throws ConfigurationException
     */
    override fun validate() {
        if (validated) return

        super.validate()
        validateRegion(_region)
        validateStream()
        validateBatchSize()
        validateInterval()
        validated = true

    }

    // Validates the batch size
    private fun validateBatchSize() =
        ConfigurationException.check(
            (batchSize <= KINESIS_MAX_BATCH_MESSAGES),
            "Max batch size for Kinesis is $KINESIS_MAX_BATCH_MESSAGES",
            CONFIG_BATCH_SIZE,
            this)

    // Validates the interval
    private fun validateInterval() =
        ConfigurationException.check(
            (_interval == null || _interval!! > 0),
            "Interval must be 1 or more",
            CONFIG_INTERVAL,
            this)

    // Validates the stream name
    private fun validateStream() =
        ConfigurationException.check(
            (_streamName != null),
            "Stream name for Kinesis Target must be specified",
            CONFIG_STREAM_NAME,
            this)

    // Validates the region
    private fun validateRegion(_region: String?) {
        if (!_region.isNullOrEmpty()) {
            val validRegions = KinesisClient.serviceMetadata().regions().map { it.id().lowercase() }
            ConfigurationException.check(
                (_region.lowercase() in validRegions),
                "Region \"$_region\" is not valid, valid regions are ${validRegions.joinToString()} ",
                CONFIG_TARGETS,
                this)
        }
    }

    companion object {
        const val KINESIS_MAX_BATCH_MESSAGES = 500
        const val DEFAULT_BATCH_SIZE = 10
        private const val CONFIG_STREAM_NAME = "StreamName"

        val default = AwsKinesisTargetConfiguration()

        fun create(streamName: String? = null,
                   region: String? = default._region,
                   batchSize: Int = default._batchSize,
                   interval: Int? = default._interval,
                   description: String = default._description,
                   active: Boolean = default._active,
                   template: String? = default._template,
                   targetServer: String? = default._server,
                   metrics: MetricsSourceConfiguration = default._metrics,
                   credentialProviderClient: String? = default._credentialProvideClient): AwsKinesisTargetConfiguration {


            val instance = createTargetConfiguration<AwsKinesisTargetConfiguration>(description = description,
                active = active,
                targetType = AWS_KINESIS_TARGET,
                template = template,
                targetServer = targetServer,
                metrics = metrics,
                credentialProviderClient = credentialProviderClient) as AwsKinesisTargetConfiguration


            with(instance) {
                _streamName = streamName
                _region = region
                _batchSize = batchSize
                _interval = interval
                _description = description
            }
            return instance
        }

    }

}