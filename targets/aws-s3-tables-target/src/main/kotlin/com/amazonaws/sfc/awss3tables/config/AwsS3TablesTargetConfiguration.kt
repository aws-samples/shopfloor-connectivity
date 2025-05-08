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

/**
 * AWS S3 Bucket target configuration
 */
@ConfigurationClass
class AwsS3TablesTargetConfiguration : AwsServiceConfig, TargetConfiguration() {
    @SerializedName(CONFIG_TABLE_BUCKET_NAME)
    private var _tableBucketName: String? = null
    val tableBucketName: String
        get() = _tableBucketName ?: ""


    @SerializedName(CONFIG_ENDPOINT)
    var _endPoint: String? = null
    override val endpoint: String?
        get() = _endPoint

    @SerializedName(CONFIG_REGION)
    private var _region: String? = null

    override val region: Region?
        get() = if (_region.isNullOrEmpty()) null else Region.of(_region!!.lowercase())

    @SerializedName(CONFIG_NAMESPACE)
    var _namespace: String? = null
    val namespace: String
        get() = _namespace?:""

    @SerializedName(CONFIG_TABLE_NAME)
    var _tableName: String? = null
    val tableName: String
        get() = _tableName ?:""

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
        validateServiceRegion()
        validateNamespace()
        validateTablename()
        validateBucket()
        validateBufferingInterval()
        validateBufferingSize()
        validated = true

    }

    // validates bucket name
    private fun validateBucket() {

        val (bucketIsValid, reason) = validateS3BucketName(_tableBucketName)
        if (!bucketIsValid) {
            throw ConfigurationException("Invalid bucket name in ${CONFIG_TABLE_BUCKET_NAME}, $reason", CONFIG_TABLE_BUCKET_NAME, this)
        }
    }

    private fun validateNamespace() {
        ConfigurationException.check(
            !(_namespace.isNullOrEmpty()),
            "$CONFIG_NAMESPACE must be specified",
            CONFIG_NAMESPACE,
            this
        )
        val (namespaceIsValid, reason) = validateName(_namespace!!)
        if (!namespaceIsValid) {
            throw ConfigurationException("Invalid $CONFIG_NAMESPACE \"$_namespace\", $reason", CONFIG_NAMESPACE, this)
        }
    }

    private fun validateTablename() {
        ConfigurationException.check(
            !(_tableName.isNullOrEmpty()),
            "$CONFIG_TABLE_NAME must be specified",
            CONFIG_TABLE_NAME,
            this
        )
        val (namespaceIsValid, reason) = validateName(_namespace!!)
        if (!namespaceIsValid) {
            throw ConfigurationException("Invalid $CONFIG_TABLE_NAME \"$_tableName\", $reason", CONFIG_TABLE_NAME, this)
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
    private fun validateServiceRegion() {

        ConfigurationException.check(
            !(_region.isNullOrEmpty() && _endPoint.isNullOrEmpty()),
            "Either $CONFIG_REGION must be specified",
            CONFIG_REGION,
            this
        )

        // check region, note that not all regions may support s3 tables
        ConfigurationException.check(
            try {
                Region.regions().contains(Region.of(_region!!.lowercase()))
            } catch (_: Exception) {
                false
            },
            "Invalid $CONFIG_REGION \"$_region\"",
            CONFIG_REGION,
            this)
    }

    companion object {
        private const val CONFIG_TABLE_BUCKET_NAME = "TableBucket"
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
                   endPoint: String? = default._endPoint,
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

        // Check forbidden prefixes
        val forbiddenPrefixes = listOf("xn--", "sthree-", "amzn-s3-demo-")
        forbiddenPrefixes.forEach { prefix ->
            if (bucketName.startsWith(prefix)) {
                return Pair(false, "$CONFIG_TABLE_BUCKET_NAME must not start with the prefix '$prefix'")
            }
        }

        // Check forbidden suffixes
        val forbiddenSuffixes = listOf("-s3alias", "--ol-s3", "--x-s3")
        forbiddenSuffixes.forEach { suffix ->
            if (bucketName.endsWith(suffix)) {
                return Pair(false, "$CONFIG_TABLE_BUCKET_NAME must not end with the suffix '$suffix'")
            }
        }

        return Pair(true, "")
    }


    fun validateName(namespaceName: String): Pair<Boolean, String> {
        // Check if namespace is reserved
        if (namespaceName.equals("aws_s3_metadata", ignoreCase = true)) {
            return Pair(false, "Namespace name 'aws_s3_metadata' is reserved and cannot be used")
        }

        // Check length (1-225 characters)
        if (namespaceName.isEmpty() || namespaceName.length > 225) {
            return Pair(false, "Namespace name must be between 1 and 225 characters long")
        }

        // Check if starts with underscore
        if (namespaceName.startsWith('_')) {
            return Pair(false, "Namespace name cannot start with an underscore")
        }

        // Check if starts with letter or number
        if (!namespaceName[0].isLetterOrDigit()) {
            return Pair(false, "Namespace name must begin with a letter or number")
        }

        // Check if ends with letter or number
        if (!namespaceName.last().isLetterOrDigit()) {
            return Pair(false, "Namespace name must end with a letter or number")
        }

        // Check for valid characters and forbidden characters
        val containsInvalidChars = namespaceName.any { char ->
            !char.isLowerCase() && !char.isDigit() && char != '_'
        }

        if (containsInvalidChars) {
            return Pair(false, "Namespace name can only contain lowercase letters, numbers, and underscores")
        }

        // Check for hyphens and periods
        if (namespaceName.contains('-') || namespaceName.contains('.')) {
            return Pair(false, "Namespace name cannot contain hyphens or periods")
        }

        return Pair(true, "")
    }


}
