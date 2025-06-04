// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awss3.config

import com.amazonaws.sfc.awss3.config.AwsS3WriterConfiguration.Companion.AWS_S3
import com.amazonaws.sfc.config.AwsServiceConfig
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_ENDPOINT
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_INTERVAL
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_REGION
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.InProcessConfiguration
import com.amazonaws.sfc.config.TargetConfiguration
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

    @SerializedName(CONFIG_OBJECT_KEY)
    var _objectKey: String? = null
    val objectKey : String?
        get() = _objectKey

    @SerializedName(CONFIG_EXTENSION)
    var _extension : String? = null
    val extension : String?
        get() = _extension?.substringAfterLast('.')

    @SerializedName(CONFIG_ENDPOINT)
    var _endPoint : String? = null
    override val endpoint : String?
        get() = _endPoint

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
        validated = true

    }

    // validates bucket name
    private fun validateBucket() {

        val (bucketIsValid, reason) = validateS3BucketName(_bucketName)
        if (!bucketIsValid) {
            throw ConfigurationException(reason, CONFIG_BUCKET_NAME, this)
        }
    }

    // validates buffering interval
    private fun validateBufferingInterval() =
        ConfigurationException.check(
            (_interval in 60..900),
            "$CONFIG_INTERVAL must be in range 60..900 seconds",
            CONFIG_INTERVAL,
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
        private const val CONFIG_EXTENSION = "Extension"
        private const val CONFIG_OBJECT_KEY = "ObjectKey"
        private const val DEFAULT_BUFFER_SIZE = 1
        private const val DEFAULT_INTERVAL = 60


        private val default = AwsS3TargetConfiguration()

        @Suppress("unused")
        fun create(bucketName: String? = default._bucketName,
                   prefix: String = default._prefix,
                   objectKey : String? = default._objectKey,
                   extension: String? = default._extension,
                   region: String? = default._region,
                   endPoint : String? = default._endPoint,
                   bufferSize: Int = default._bufferSize,
                   interval: Int = default._interval,
                   description: String = default._description,
                   active: Boolean = default._active,
                   compressionType: CompressionType? = default._compressionType,
                   contentType: String? = default._contentType,
                   template: String? = default._template,
                   formatter : InProcessConfiguration? = default._formatter,
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
                formatter = formatter,
                credentialProviderClient = credentialProviderClient) as AwsS3TargetConfiguration

            with(instance) {
                _bucketName = bucketName
                _prefix = prefix
                _objectKey = objectKey
                _extension = extension
                _region = region
                _endPoint = endPoint
                _bufferSize = bufferSize
                _interval = interval
                _compressionType = compressionType
                _contentType = contentType
            }
            return instance
        }
    }

    fun validateS3BucketName(bucketName: String?): Pair<Boolean, String> {
        // Check if bucket name is null or empty
        if (bucketName.isNullOrEmpty()) {
            return Pair(false, "$CONFIG_BUCKET_NAME cannot be empty")
        }

        // Check length (3-63 characters)
        if (bucketName.length < 3 || bucketName.length > 63) {
            return Pair(false, "$CONFIG_BUCKET_NAME must be between 3 and 63 characters long")
        }

        // Check for valid characters
        val validCharacters = bucketName.all { it.isLowerCase() || it.isDigit() || it == '.' || it == '-' }
        if (!validCharacters) {
            return Pair(false, "$CONFIG_BUCKET_NAME can only contain lowercase letters, numbers, periods (.), and hyphens (-)")
        }

        // Check if starts with letter or number
        if (!bucketName[0].isLetterOrDigit()) {
            return Pair(false, "$CONFIG_BUCKET_NAME must begin with a letter or number")
        }

        // Check if ends with letter or number
        if (!bucketName.last().isLetterOrDigit()) {
            return Pair(false, "$CONFIG_BUCKET_NAME must end with a letter or number")
        }

        // Check for consecutive periods
        if (bucketName.contains("..")) {
            return Pair(false, "$CONFIG_BUCKET_NAME must not contain two adjacent periods")
        }

        // Check if formatted as IP address
        val ipAddressPattern = "^\\d+\\.\\d+\\.\\d+\\.\\d+$".toRegex()
        if (bucketName.matches(ipAddressPattern)) {
            return Pair(false, "$CONFIG_BUCKET_NAME must not be formatted as an IP address")
        }

        // Check forbidden prefixes
        val forbiddenPrefixes = listOf("xn--", "sthree-", "amzn-s3-demo-")
        forbiddenPrefixes.forEach { prefix ->
            if (bucketName.startsWith(prefix)) {
                return Pair(false, "$CONFIG_BUCKET_NAME must not start with the prefix '$prefix'")
            }
        }

        // Check forbidden suffixes
        val forbiddenSuffixes = listOf("-s3alias", "--ol-s3", ".mrap", "--x-s3")
        forbiddenSuffixes.forEach { suffix ->
            if (bucketName.endsWith(suffix)) {
                return Pair(false, "$CONFIG_BUCKET_NAME must not end with the suffix '$suffix'")
            }
        }

        return Pair(true, "")
    }


}
