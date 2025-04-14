// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awss3tables.config

import com.amazonaws.sfc.awss3tables.config.AwsS3TablesWriterConfiguration.Companion.AWS_S3_TABLES
import com.amazonaws.sfc.config.AwsServiceConfig
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_ENDPOINT
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_INTERVAL
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_REGION
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.TargetConfiguration
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.google.gson.annotations.SerializedName
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3tables.S3TablesClient

/**
 * AWS S3 Bucket target configuration
 */
@ConfigurationClass
class AwsS3TablesTargetConfiguration : AwsServiceConfig, TargetConfiguration() {
    @SerializedName(CONFIG_TABLE_BUCKET_NAME)
    private var _tableBucketName: String? = null
    val tableBucketName: String?
        get() = _tableBucketName


    @SerializedName(CONFIG_ENDPOINT)
    var _endPoint : String? = null
    override val endpoint : String?
        get() = _endPoint

    @SerializedName(CONFIG_REGION)
    private var _region: String? = null

    override val region: Region?
        get() = if (_region.isNullOrEmpty()) null else Region.of(_region!!.lowercase())

    @SerializedName(CONFIG_NAMESPACE)
    var _namespace: String? = null
    val namespace: String?
        get() = _namespace

    @SerializedName(CONFIG_TABLE_NAME)
    var _tableName: String? = null
    val tableName: String?
        get() = _tableName

    @SerializedName(CONFIG_AUTO_CREATE)
    var _autoCreate: Boolean = true
    val autoCreate: Boolean
        get() = _autoCreate

    @SerializedName(CONFIG_BUFFER_SIZE)
    private var _bufferSize: Int = DEFAULT_BUFFER_SIZE // in MB

    /**
     * Batch size in bytes for writing data to S3 object
     */
    val bufferSize: Int
        get() = _bufferSize * 1024 * 1024 // to MB


    @SerializedName(CONFIG_INTERVAL)
    private var _interval: Int = DEFAULT_INTERVAL // in seconds

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

        val (bucketIsValid, reason) = validateS3BucketName(_tableBucketName)
        if (!bucketIsValid) {
            throw ConfigurationException(reason, CONFIG_TABLE_BUCKET_NAME, this)
        }
    }

    // validates buffering interval
    private fun validateBufferingInterval() =
        ConfigurationException.check(
            (_interval in 1..900),
            "$CONFIG_INTERVAL must be in range 1..900 seconds",
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
        ConfigurationException.check(
            !(_region.isNullOrEmpty() && _endPoint.isNullOrEmpty()),
            "Either $CONFIG_REGION must be specified",
            CONFIG_REGION,
            this
        )
    }

    companion object {
        private const val CONFIG_TABLE_BUCKET_NAME = "TableBucketName"
        private const val CONFIG_NAMESPACE = "Namespace"
        private const val CONFIG_BUFFER_SIZE = "BufferSize"
        private const val CONFIG_TABLE_NAME = "TableName"
        private const val DEFAULT_BUFFER_SIZE = 1
        private const val DEFAULT_INTERVAL = 10
        private const val CONFIG_AUTO_CREATE = "AutoCreate"

        private val default = AwsS3TablesTargetConfiguration()

        @Suppress("unused")
        fun create(tableBucketName: String? = default._tableBucketName,
                   region: String? = default._region,
                   namespace: String? = default._namespace,
                   tableName: String? = default._tableName,
                   autoCreate: Boolean = default._autoCreate,
                   endPoint : String? = default._endPoint,
                   bufferSize: Int = default._bufferSize,
                   interval: Int = default._interval,
                   description: String = default._description,
                   active: Boolean = default._active,
                   template: String? = default._template,
                   targetServer: String? = default._server,
                   metrics: MetricsSourceConfiguration = default._metrics,
                   credentialProviderClient: String? = default._credentialProvideClient): AwsS3TablesTargetConfiguration {

            val instance = createTargetConfiguration<AwsS3TablesTargetConfiguration>(
                description = description,
                active = active,
                targetType = AWS_S3_TABLES,
                template = template,
                targetServer = targetServer,
                metrics = metrics,
                credentialProviderClient = credentialProviderClient) as AwsS3TablesTargetConfiguration

            with(instance) {
                _tableBucketName = tableBucketName
                _region = region
                _endPoint = endPoint
                _bufferSize = bufferSize
                _interval = interval
                _namespace = namespace
                _tableName = tableName
                _autoCreate = autoCreate
            }
            return instance
        }
    }

    fun validateS3BucketName(bucketName: String?): Pair<Boolean, String> {
        // Check if bucket name is null or empty
        if (bucketName.isNullOrEmpty()) {
            return Pair(false, "$CONFIG_TABLE_BUCKET_NAME cannot be empty")
        }

        // Check length (3-63 characters)
        if (bucketName.length < 3 || bucketName.length > 63) {
            return Pair(false, "$CONFIG_TABLE_BUCKET_NAME must be between 3 and 63 characters long")
        }

        // Check for valid characters
        val validCharacters = bucketName.all { it.isLowerCase() || it.isDigit() || it == '.' || it == '-' }
        if (!validCharacters) {
            return Pair(false, "$CONFIG_TABLE_BUCKET_NAME can only contain lowercase letters, numbers, periods (.), and hyphens (-)")
        }

        // Check if starts with letter or number
        if (!bucketName[0].isLetterOrDigit()) {
            return Pair(false, "$CONFIG_TABLE_BUCKET_NAME must begin with a letter or number")
        }

        // Check if ends with letter or number
        if (!bucketName.last().isLetterOrDigit()) {
            return Pair(false, "$CONFIG_TABLE_BUCKET_NAME must end with a letter or number")
        }

        // Check for consecutive periods
        if (bucketName.contains("..")) {
            return Pair(false, "$CONFIG_TABLE_BUCKET_NAME must not contain two adjacent periods")
        }

        // Check if formatted as IP address
        val ipAddressPattern = "^\\d+\\.\\d+\\.\\d+\\.\\d+$".toRegex()
        if (bucketName.matches(ipAddressPattern)) {
            return Pair(false, "$CONFIG_TABLE_BUCKET_NAME must not be formatted as an IP address")
        }

        // Check forbidden prefixes
        val forbiddenPrefixes = listOf("xn--", "sthree-", "amzn-s3-demo-")
        forbiddenPrefixes.forEach { prefix ->
            if (bucketName.startsWith(prefix)) {
                return Pair(false, "$CONFIG_TABLE_BUCKET_NAME must not start with the prefix '$prefix'")
            }
        }

        // Check forbidden suffixes
        val forbiddenSuffixes = listOf("-s3alias", "--ol-s3", ".mrap", "--x-s3")
        forbiddenSuffixes.forEach { suffix ->
            if (bucketName.endsWith(suffix)) {
                return Pair(false, "$CONFIG_TABLE_BUCKET_NAME must not end with the suffix '$suffix'")
            }
        }

        return Pair(true, "")
    }


}
