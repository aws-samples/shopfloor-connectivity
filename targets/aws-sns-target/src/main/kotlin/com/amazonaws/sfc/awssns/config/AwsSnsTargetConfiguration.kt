/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.awssns.config

import com.amazonaws.sfc.awssns.config.AwsSnsWriterConfiguration.Companion.AWS_SNS
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_BATCH_SIZE
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_INTERVAL
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_REGION
import com.amazonaws.sfc.data.Compress
import com.amazonaws.sfc.data.CompressionType
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.google.gson.annotations.SerializedName
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * AWS SNS target topic configuration
 */
@ConfigurationClass
class AwsSnsTargetConfiguration : AwsServiceConfig, TargetConfiguration() {
    @SerializedName(CONFIG_TOPIC_ARN)
    private var _topicArn: String? = null

    /**
     * URL of the AWS target sns topic
     */
    val topicArn: String?
        get() = _topicArn

    @SerializedName(CONFIG_SUBJECT)
    private var _subject: String? = null

    /**
     * Subject
     */
    val subject: String?
        get() = _subject

    @SerializedName(CONFIG_MESSAGE_GROUP_ID)
    private var _messageGroupId: String? = null

    /**
     * MessageGroup ID
     */
    val messageGroupId: String?
        get() = _messageGroupId

    @SerializedName(CONFIG_SERIAL_DEDUPLICATION_ID)
    private var _serialAsMessageDeduplicationId: Boolean = false

    /**
     * Use target data serial as MessageDeduplicationId ID
     */
    val serialAsMessageDeduplicationId: Boolean
        get() = _serialAsMessageDeduplicationId

    @SerializedName(CONFIG_REGION)
    private var _region: String? = null

    /**
     * AWS region
     */
    override val region: Region?
        get() = if (_region.isNullOrEmpty()) null else Region.of(_region!!.lowercase())

    @SerializedName(CONFIG_INTERVAL)
    private var _interval: Int? = null

    /**
     * Interval  for sending messages to the stream.
     */
    val interval: Duration
        get() = _interval?.toDuration(DurationUnit.MILLISECONDS) ?: Duration.INFINITE

    @SerializedName(CONFIG_BATCH_SIZE)
    private var _batchSize: Int = DEFAULT_BATCH_SIZE


    @SerializedName(Compress.CONFIG_COMPRESS)
    private var _compressionType: CompressionType? = null

    val compressionType: CompressionType
        get() = _compressionType ?: CompressionType.NONE


    /**
     * Number of messages to combine in a batch when writing to SNS topic
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
        validateArn()
        validateBatchSize()
        validateInterval()
        validated = true
    }

    // Validates SNS topic ARN
    private fun validateArn() =
        ConfigurationException.check(
            (_topicArn != null),
            "$CONFIG_TOPIC_ARN for SNS Target must be specified",
            CONFIG_TOPIC_ARN,
            this
        )

    // Validates batch size
    private fun validateBatchSize() =
        ConfigurationException.check(
            (batchSize in 1..10),
            "$CONFIG_BATCH_SIZE for SNS must be in range 1..10",
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
            val validRegions = SnsClient.serviceMetadata().regions().map { it.id() }
            ConfigurationException.check(
                (_region.lowercase() in validRegions),
                "$CONFIG_REGION \"$_region\" is not valid, valid regions are ${validRegions.joinToString()} ",
                CONFIG_REGION,
                this
            )


        }
    }

    companion object {
        private const val CONFIG_TOPIC_ARN = "TopicArn"
        private const val CONFIG_SUBJECT = "Subject"
        private const val CONFIG_MESSAGE_GROUP_ID = "MessageGroupId"
        private const val CONFIG_SERIAL_DEDUPLICATION_ID = "SerialAsMessageDeduplicationId"
        private const val DEFAULT_BATCH_SIZE = 10

        private val default = AwsSnsTargetConfiguration()

        fun create(topicArn: String? = default._topicArn,
                   subject: String? = default._subject,
                   messageGroupId: String? = default._messageGroupId,
                   serialAsMessageDeduplicationId: Boolean = default._serialAsMessageDeduplicationId,
                   region: String? = default._region,
                   interval: Int? = default._interval,
                   batchSize: Int = default._batchSize,
                   description: String = default._description,
                   active: Boolean = default._active,
                   template: String? = default._template,
                   targetServer: String? = default._server,
                   metrics: MetricsSourceConfiguration = default._metrics,
                   credentialProviderClient: String? = default._credentialProvideClient): AwsSnsTargetConfiguration {

            val instance = createTargetConfiguration<AwsSnsTargetConfiguration>(description = description,
                active = active,
                targetType = AWS_SNS,
                template = template,
                targetServer = targetServer,
                metrics = metrics,
                credentialProviderClient = credentialProviderClient) as AwsSnsTargetConfiguration

            with(instance) {
                _topicArn = topicArn
                _subject = subject
                _messageGroupId = messageGroupId
                _serialAsMessageDeduplicationId = serialAsMessageDeduplicationId
                _region = region
                _interval = interval
                _batchSize = batchSize
            }
            return instance
        }
    }
}