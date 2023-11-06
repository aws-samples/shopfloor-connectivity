
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awslambda.config

import com.amazonaws.sfc.awslambda.config.AwsLambdaWriterConfiguration.Companion.AWS_LAMBDA
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_BATCH_SIZE
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_INTERVAL
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_REGION
import com.amazonaws.sfc.data.Compress
import com.amazonaws.sfc.data.CompressionType
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.google.gson.annotations.SerializedName
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.lambda.LambdaClient
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * AWS Lambda function target configuration
 */
@ConfigurationClass
class AwsLambdaTargetConfiguration : AwsServiceConfig, TargetConfiguration() {
    @SerializedName(CONFIG_FUNCTION_NAME)
    private var _functionName: String? = null

    /**
     * Name of the Lambda function.
     */
    val functionName: String?
        get() = _functionName

    @SerializedName(CONFIG_QUALIFIER)
    private var _qualifier: String? = null

    /**
     * Lambda function qualifier.
     */
    val qualifier: String?
        get() = _qualifier

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
     * Batch size for combining multiple messages in a payload for called Lambda function
     */
    val batchSize: Int
        get() = _batchSize

    @SerializedName(Compress.CONFIG_COMPRESS)
    private var _compressionType: CompressionType? = null

    val compressionType: CompressionType
        get() = _compressionType ?: CompressionType.NONE

    @SerializedName(CONFIG_INTERVAL)
    private var _interval: Int? = null

    /**
     * Interval  for sending messages to the stream.
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
        validateServiceRegion(_region)
        validateLambda()
        validateInterval()
        validated = true

    }

    // validates lambda name
    private fun validateLambda() =
        ConfigurationException.check(
            (_functionName != null),
            "Name of Lambda function must be specified",
            CONFIG_FUNCTION_NAME,
            this
        )

    // validates AWS region
    private fun validateServiceRegion(_region: String?) {
        if (!_region.isNullOrEmpty()) {
            val validRegions = LambdaClient.serviceMetadata().regions().map { it.id() }
            ConfigurationException.check(
                (_region.lowercase() in validRegions),
                "$CONFIG_REGION \"$_region\" is not valid, valid regions are ${validRegions.joinToString()}",
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
        private const val CONFIG_FUNCTION_NAME = "FunctionName"
        private const val CONFIG_QUALIFIER = "Qualifier"
        private const val DEFAULT_BATCH_SIZE = 10

        private val default = AwsLambdaTargetConfiguration()

        fun create(functionName: String? = default._functionName,
                   qualifier: String? = default._qualifier,
                   region: String? = default._region,
                   batchSize: Int = default._batchSize,
                   interval: Int? = default._interval,
                   description: String = default._description,
                   active: Boolean = default._active,
                   template: String? = default._template,
                   targetServer: String? = default._server,
                   metrics: MetricsSourceConfiguration = default._metrics,
                   credentialProviderClient: String? = default._credentialProvideClient): AwsLambdaTargetConfiguration {

            val instance = createTargetConfiguration<AwsLambdaTargetConfiguration>(
                description = description,
                active = active,
                targetType = AWS_LAMBDA,
                template = template,
                targetServer = targetServer,
                metrics = metrics,
                credentialProviderClient = credentialProviderClient) as AwsLambdaTargetConfiguration

            with(instance) {
                _functionName = functionName
                _qualifier = qualifier
                _region = region
                _batchSize = batchSize
                _interval = interval
            }
            return instance
        }


    }

}