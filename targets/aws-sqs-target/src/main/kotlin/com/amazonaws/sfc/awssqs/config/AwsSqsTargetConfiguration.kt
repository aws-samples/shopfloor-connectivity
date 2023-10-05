/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.awssqs.config

import com.amazonaws.sfc.awssqs.config.AwsSqsWriterConfiguration.Companion.AWS_SQS
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_BATCH_SIZE
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_INTERVAL
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_REGION
import com.amazonaws.sfc.data.Compress
import com.amazonaws.sfc.data.CompressionType
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.google.gson.annotations.SerializedName
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * AWS SQS target queue configuration
 */
@ConfigurationClass
class AwsSqsTargetConfiguration : AwsServiceConfig, TargetConfiguration() {
    @SerializedName(CONFIG_QUEUE_URL)
    private var _queueUrl: String? = null

    /**
     * URL of the AWS target queue
     */
    val queueUrl: String?
        get() = _queueUrl

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
     * Number of messages to combine in a batch when writing to SQS queue
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

    @SerializedName(Compress.CONFIG_COMPRESS)
    private var _compressionType: CompressionType? = null

    val compressionType: CompressionType
        get() = _compressionType ?: CompressionType.NONE


    /**
     * Validates configuration
     * @throws ConfigurationException
     */
    override fun validate() {
        if (validated) return

        super.validate()
        validateRegion(_region)
        validateQueue()
        validateBatchSize()
        validateInterval()
        validated = true
    }

    // Validates SQS queue url
    private fun validateQueue() =
        ConfigurationException.check(
            (_queueUrl != null),
            "$CONFIG_QUEUE_URL for SQS Target must be specified",
            CONFIG_QUEUE_URL,
            this
        )

    private fun validateBatchSize() =
        ConfigurationException.check(
            (batchSize in 1..10),
            "$CONFIG_BATCH_SIZE for SQS must be in range 1..10",
            CONFIG_BATCH_SIZE,
            this
        )

    // Validates the interval
    private fun validateInterval() =
        ConfigurationException.check(
            (_interval == null || _interval!! > 0),
            "Interval must be 1 or more",
            CONFIG_INTERVAL,
            this)


    // Validates AWS region
    private fun validateRegion(_region: String?) {
        if (!_region.isNullOrEmpty()) {
            val validRegions = SqsClient.serviceMetadata().regions().map { it.id() }
            ConfigurationException.check(
                (_region.lowercase() in validRegions),
                "$CONFIG_REGION \"$_region\" is not valid, valid regions are ${validRegions.joinToString()} ",
                CONFIG_REGION,
                this
            )


        }
    }

    companion object {
        private const val CONFIG_QUEUE_URL = "QueueUrl"
        private const val DEFAULT_BATCH_SIZE = 10

        private val default = AwsSqsTargetConfiguration()

        fun create(queueUrl: String? = null,
                   region: String? = null,
                   batchSize: Int,
                   interval: Int? = null,
                   description: String = default._description,
                   active: Boolean = default._active,
                   template: String? = default._template,
                   targetServer: String? = default._server,
                   metrics: MetricsSourceConfiguration = default._metrics,
                   credentialProviderClient: String? = default._credentialProvideClient): AwsSqsTargetConfiguration {

            val instance = createTargetConfiguration<AwsSqsTargetConfiguration>(description = description,
                active = active,
                targetType = AWS_SQS,
                template = template,
                targetServer = targetServer,
                metrics = metrics,
                credentialProviderClient = credentialProviderClient) as AwsSqsTargetConfiguration

            with(instance) {
                _queueUrl = queueUrl
                _region = region
                _batchSize = batchSize
                _interval = interval
            }
            return instance
        }
    }
}