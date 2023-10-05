/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.awstimestream.config

import com.amazonaws.sfc.awstimestream.config.AwsTimestreamWriterConfiguration.Companion.AWS_TIMESTREAM
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_BATCH_SIZE
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_INTERVAL
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_REGION
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.google.gson.annotations.SerializedName
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.timestreamwrite.TimestreamWriteClient
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@ConfigurationClass
class AwsTimestreamTargetConfiguration : AwsServiceConfig, TargetConfiguration(), Validate {

    @SerializedName(CONFIG_TIMESTREAM_TABLE_NAME)
    private var _tableName: String? = null

    val tableName: String
        get() = _tableName ?: ""

    @SerializedName(CONFIG_TIMESTREAM_DATABASE_NAME)
    private var _database: String? = null

    val database: String
        get() {
            return _database ?: ""
        }

    @SerializedName(CONFIG_REGION)
    private var _region: String? = null

    /**
     * AWS region
     */
    override val region: Region?
        get() = if (_region.isNullOrEmpty()) null else Region.of(_region!!.lowercase())

    @SerializedName(CONFIG_BATCH_SIZE)
    private var _batchSize: Int = 10

    /**
     * Number of messages to combine in a batch before sending record to Timestream
     */
    val batchSize: Int
        get() = _batchSize


    @SerializedName(CONFIG_INTERVAL)
    private var _interval: Int? = null

    /**
     * Interval  for sending messages to the stream.
     */
    val interval: Duration
        get() = _interval?.toDuration(DurationUnit.MILLISECONDS) ?: Duration.INFINITE


    @SerializedName(CONFIG_TIMESTREAM_RECORDS)
    private var _records: List<AwsTimestreamRecordConfiguration> = emptyList()

    val records
        get() = _records

    override fun validate() {
        if (validated) return
        super.validate()
        validateDatabaseAndTable()
        validateRegion()
        validateRecords()
        validateInterval()
        validated = true
    }

    private fun validateDatabaseAndTable() {

        ConfigurationException.check(
            (_database != null),
            "$CONFIG_TIMESTREAM_DATABASE_NAME must be specified",
            CONFIG_TIMESTREAM_DATABASE_NAME,
            this
        )
        ConfigurationException.check(
            (_tableName != null),
            "$CONFIG_TIMESTREAM_TABLE_NAME must be specified",
            CONFIG_TIMESTREAM_TABLE_NAME,
            this
        )
    }

    private fun validateRecords() {

        ConfigurationException.check(
            (records.isNotEmpty()),
            "One or more records must be configured for target",
            CONFIG_TIMESTREAM_RECORDS,
            this
        )

        records.forEach {
            it.validate()
        }
    }

    // Validates AWS region
    private fun validateRegion() {
        if (!_region.isNullOrEmpty()) {
            val validRegions = TimestreamWriteClient.serviceMetadata().regions().map { it.id() }
            ConfigurationException.check(
                (_region!!.lowercase() in validRegions || validRegions.isEmpty()),
                "$CONFIG_REGION \"$_region\" is not valid, valid regions are ${validRegions.joinToString()} ",
                BaseConfiguration.CONFIG_TARGETS,
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

        private const val CONFIG_TIMESTREAM_TABLE_NAME = "TableName"
        private const val CONFIG_TIMESTREAM_DATABASE_NAME = "Database"
        private const val CONFIG_TIMESTREAM_RECORDS = "Records"

        private val default = AwsTimestreamTargetConfiguration()

        fun create(tableName: String? = default._tableName,
                   database: String? = default._database,
                   region: String? = default._region,
                   batchSize: Int = default._batchSize,
                   interval: Int? = default._interval,
                   records: List<AwsTimestreamRecordConfiguration> = emptyList(),
                   description: String = default._description,
                   active: Boolean = default._active,
                   template: String? = default._template,
                   targetServer: String? = default._server,
                   metrics: MetricsSourceConfiguration = default._metrics,
                   credentialProviderClient: String? = default._credentialProvideClient): AwsTimestreamTargetConfiguration {

            val instance = createTargetConfiguration<AwsTimestreamTargetConfiguration>(description = description,
                active = active,
                targetType = AWS_TIMESTREAM,
                template = template,
                targetServer = targetServer,
                metrics = metrics,
                credentialProviderClient = credentialProviderClient) as AwsTimestreamTargetConfiguration

            with(instance) {
                _tableName = tableName
                _database = database
                _region = region
                _batchSize = batchSize
                _interval = interval
                _records = records
            }
            return instance
        }

    }


}