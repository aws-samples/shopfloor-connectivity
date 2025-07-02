// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awss3tables.config

import com.amazonaws.sfc.awss3tables.AwsS3TablesHelper.Companion.validateName
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
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


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
        get() = _namespace ?: ""

    @SerializedName(CONFIG_TABLES)
    var _tables : List<TableConfiguration> =  emptyList()
    val tables : List<TableConfiguration>
        get() = _tables


    @SerializedName(CONFIG_AUTO_CREATE)
    var _autoCreate: Boolean = true
    val autoCreate: Boolean
        get() = _autoCreate

    @SerializedName(CONFIG_WARN_IF_VALUE_MISSING)
    var _warnIfValueMissing: Boolean = true
    val warnIfValueMissing: Boolean
        get() = _warnIfValueMissing

    @SerializedName(CONFIG_BUFFER_COUNT)
    private var _bufferCount: Int = DEFAULT_BUFFER_COUNT

    val bufferCount: Int
        get() = _bufferCount

    @SerializedName(CONFIG_INTERVAL)
    private var _interval: Int = DEFAULT_INTERVAL
    val interval : Duration
        get() = _interval.toDuration(DurationUnit.SECONDS)


    /**
     * Validates configuration
     * @throws ConfigurationException
     */
    override fun validate() {
        if (validated) return
        validateServiceRegion()
        validateNamespace()
        validateBucket()
        validateTables()
        validated = true

    }

    private fun validateTables(){
        ConfigurationException.check(
            _tables.isNotEmpty(),
            "At least one table must be specified",
            CONFIG_TABLES,
            this
        )
        tables.forEach { it.validate() }
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
        private const val CONFIG_BUFFER_COUNT = "BufferCount"
        private const val DEFAULT_BUFFER_COUNT = 100
        private const val DEFAULT_INTERVAL = 10 * 1000
        const val CONFIG_AUTO_CREATE = "AutoCreate"
        private const val CONFIG_TABLES = "Tables"
        private const val CONFIG_WARN_IF_VALUE_MISSING = "WarnIfValueMissing"


        private val default = AwsS3TablesTargetConfiguration()


        fun create(tableBucketName: String? = default._tableBucketName,
                   region: String? = default._region,
                   namespace: String? = default._namespace,
                   tables: List<TableConfiguration> = default._tables,
                   autoCreate: Boolean = default._autoCreate,
                   endPoint: String? = default._endPoint,
                   bufferSize: Int = default._bufferCount,
                   interval: Int = default._interval,
                   description: String = default._description,
                   active: Boolean = default._active,
                   warnIfValueMissing: Boolean = default._warnIfValueMissing,
                   targetServer: String? = default._server,
                   metrics: MetricsSourceConfiguration = default._metrics,
                   credentialProviderClient: String? = default._credentialProvideClient): AwsS3TablesTargetConfiguration {

            val instance = createTargetConfiguration<AwsS3TablesTargetConfiguration>(
                description = description,
                active = active,
                targetServer = targetServer,
                metrics = metrics,
                credentialProviderClient = credentialProviderClient) as AwsS3TablesTargetConfiguration

            with(instance) {
                _tableBucketName = tableBucketName
                _region = region
                _endPoint = endPoint
                _bufferCount = bufferSize
                _interval = interval
                _namespace = namespace
                _tables = tables
                _autoCreate = autoCreate
                _warnIfValueMissing = warnIfValueMissing
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




}
