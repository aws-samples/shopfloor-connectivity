
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awss3.config

import com.amazonaws.sfc.awss3.config.AwsS3WriterConfiguration.Companion.AWS_S3
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_INTERVAL
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_REGION
import com.amazonaws.sfc.data.Compress.CONFIG_COMPRESS
import com.amazonaws.sfc.data.Compress.CONTENT_TYPE
import com.amazonaws.sfc.data.CompressionType
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.google.gson.annotations.SerializedName
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

/**
 * AWS S3 Bucket target configuration
 */
@ConfigurationClass
class AwsS3TargetConfiguration : AwsServiceConfig, TargetConfiguration() {
    @SerializedName(CONFIG_BUCKET_NAME)
    private var _bucketName: String? = null

    /**
     * Name of the S3 Bucket.
     */
    val bucketName: String?
        get() = _bucketName

    @SerializedName(CONFIG_PREFIX)
    private var _prefix: String = ""

    /**
     * S3 key prefix
     */
    val prefix: String
        get() = _prefix

    @SerializedName(CONFIG_REGION)
    private var _region: String? = null

    /**
     * AWS Region.
     */
    override val region: Region?
        get() = if (_region.isNullOrEmpty()) null else Region.of(_region!!.lowercase())

    @SerializedName(CONFIG_BUFFER_SIZE)
    private var _bufferSize: Int = DEFAULT_BUFFER_SIZE // in MB

    /**
     * Batch size in bytes for writing data to S3 object
     */
    val bufferSize: Int
        get() = _bufferSize * 1024 * 1024 // to MB


    @SerializedName(CONFIG_COMPRESS)
    private var _compressionType: CompressionType? = null

    val compressionType: CompressionType
        get() = _compressionType ?: CompressionType.NONE

    @SerializedName(CONTENT_TYPE)
    private var _contentType: String? = null

    val contentType: String?
        get() = _contentType


    @SerializedName(CONFIG_INTERVAL)
    private var _interval: Int = DEFAULT_INTERVAL // in seconds

    /**
     * Interval in milliseconds for writing data to S3 object
     */
    val interval: Int
        get() = _interval * 1000 // to milliseconds

    /**
     * Validates configuration
     * @throws ConfigurationException
     */
    override fun validate() {
        if (validated) return

        validateServiceRegion(_region)
        validateBucket()
        validateBufferingInterval()
        validateBufferingSize()
        validated = true

    }

    // validates bucket name
    private fun validateBucket() {
        ConfigurationException.check(
            (_bucketName != null && _bucketName!!.length in 3..63),
            "Name of S3 bucket must be specified and must be between 3 and 63 characters long",
            CONFIG_BUCKET_NAME,
            this
        )

        ConfigurationException.check(
            (_bucketName!!.all { it in "abcdefghijklmnopqrstuvwxyxz.-" } &&
             (_bucketName!![0] !in "-.") &&
             (_bucketName!![_bucketName!!.length - 1] !in "-.")),
            "Name of S3 bucket is invalid",
            CONFIG_BUCKET_NAME,
            this
        )
    }

    // validates buffering interval
    private fun validateBufferingInterval() =
        ConfigurationException.check(
            (_interval in 60..900),
            "$CONFIG_INTERVAL must be in range 60..900 seconds",
            CONFIG_INTERVAL,
            this
        )

    // validates buffering interval
    private fun validateBufferingSize() =
        ConfigurationException.check(
            (_bufferSize in 1..128),
            "Buffer size must be in range 1..128 MB",
            CONFIG_BUFFER_SIZE,
            this
        )

    // validates AWS region
    private fun validateServiceRegion(_region: String?) {
        if (!_region.isNullOrEmpty()) {
            val validRegions = S3Client.serviceMetadata().regions().map { it.id() }
            ConfigurationException.check(
                (_region.lowercase() in validRegions),
                "$CONFIG_REGION \"$_region\" is not valid, valid regions are ${validRegions.joinToString()}",
                CONFIG_REGION,
                this
            )
        }
    }

    companion object {
        private const val CONFIG_BUCKET_NAME = "BucketName"
        private const val CONFIG_PREFIX = "Prefix"
        private const val CONFIG_BUFFER_SIZE = "BufferSize"
        private const val DEFAULT_BUFFER_SIZE = 1
        private const val DEFAULT_INTERVAL = 60


        private val default = AwsS3TargetConfiguration()

        @Suppress("unused")
        fun create(bucketName: String? = default._bucketName,
                   prefix: String = default._prefix,
                   region: String? = default._region,
                   bufferSize: Int = default._bufferSize,
                   interval: Int = default._interval,
                   description: String = default._description,
                   active: Boolean = default._active,
                   compressionType: CompressionType? = default._compressionType,
                   contentType: String? = default._contentType,
                   template: String? = default._template,
                   targetServer: String? = default._server,
                   metrics: MetricsSourceConfiguration = default._metrics,
                   credentialProviderClient: String? = default._credentialProvideClient): AwsS3TargetConfiguration {

            val instance = createTargetConfiguration<AwsS3TargetConfiguration>(
                description = description,
                active = active,
                targetType = AWS_S3,
                template = template,
                targetServer = targetServer,
                metrics = metrics,
                credentialProviderClient = credentialProviderClient) as AwsS3TargetConfiguration

            with(instance) {
                _bucketName = bucketName
                _prefix = prefix
                _region = region
                _bufferSize = bufferSize
                _interval = interval
                _compressionType = compressionType
                _contentType = contentType
            }
            return instance
        }


    }
}