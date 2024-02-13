
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsmsk.config


import com.amazonaws.sfc.awsmsk.config.AwsMskWriterConfiguration.Companion.AWS_MSK_TARGET
import com.amazonaws.sfc.config.AwsServiceConfig
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_BATCH_SIZE
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_INTERVAL
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.TargetConfiguration
import com.amazonaws.sfc.data.Compress.CONFIG_COMPRESS
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.google.gson.annotations.SerializedName
import software.amazon.awssdk.regions.Region
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class AwsMskTargetConfiguration : AwsServiceConfig, TargetConfiguration() {

    @SerializedName(CONFIG_BOOTSTRAP_SERVERS)
    private var _bootstrapServers : ArrayList<String> = ArrayList()
    val bootstrapServers
        get() = _bootstrapServers


    @SerializedName(CONFIG_TOPIC_NAME)
    private var _topicName: String? = null
    val topicName: String?
        get() = _topicName

    @SerializedName(CONFIG_KEY_VALUE)
    private var _key: String? = null
    val key: String?
        get() = _key

    @SerializedName(CONFIG_PARTITION)
    private var _partition: Int? = null
    val partition: Int?
        get() = _partition


    @SerializedName(CONFIG_BATCH_SIZE)
    private var _batchSize: Int? = null

    val batchSize: Int?
        get() = _batchSize

    @SerializedName(CONFIG_INTERVAL)
    private var _interval: Int? = null


    val interval: Duration?
        get() = _interval?.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_COMPRESS)
    private var _compression: Compression =  Compression.NONE

    val compressionType: Compression
        get() = _compression

    @SerializedName(CONFIG_SERIALIZATION)
    private var _serialization: Serialization = Serialization.JSON

    val serialization: Serialization
        get() = _serialization

    @SerializedName(CONFIG_ACKS)
    private var _acknowledgements: Acknowledgements = Acknowledgements.LEADER

    val acknowledgements: Acknowledgements
         get() = _acknowledgements

    @SerializedName(CONFIG_PROVIDER_PROPERTIES)
    private var _providerProperties: Map<String,String> = emptyMap()

    val providerProperties: Map<String,String>
        get() = _providerProperties

    @SerializedName(CONFIG_HEADERS)
    private var _headers: Map<String,String> = emptyMap()

    val headers: Map<String,String>
        get() = _headers

    /**
     * Validates the configuration.
     * @throws ConfigurationException
     */
    override fun validate() {
        if (validated) return

        super.validate()
        validateTopic()
        validateInterval()
        validated = true

    }

    // Validates the interval
    private fun validateInterval() =
        ConfigurationException.check(
            (_interval == null || _interval!! > 0),
            "Interval must be 1 or more",
            CONFIG_INTERVAL,
            this)

    // Validates the topic name
    private fun validateTopic() =
        ConfigurationException.check(
            (_topicName != null),
            "$CONFIG_TOPIC_NAME for MSK topic must be specified",
            CONFIG_TOPIC_NAME,
            this)


    companion object {

        private const val CONFIG_BOOTSTRAP_SERVERS = "BootstrapServers"
        private const val CONFIG_TOPIC_NAME = "TopicName"
        private const val CONFIG_KEY_VALUE = "Key"
        private const val CONFIG_PARTITION = "Partition"
        private const val CONFIG_SERIALIZATION= "Serialization"
        private const val CONFIG_ACKS = "Acknowledgements"
        private const val CONFIG_PROVIDER_PROPERTIES = "ProviderProperties"
        private const val CONFIG_HEADERS = "Headers"

        private val default = AwsMskTargetConfiguration()

        fun create(topicName: String? = null,
                   bootstrapServers : ArrayList<String> = default._bootstrapServers,
                   key : String? = default.key,
                   partition : Int? = default.partition,
                   batchSize: Int? = default._batchSize,
                   acknowledgements: Acknowledgements = default._acknowledgements,
                   serialization: Serialization = default._serialization,
                   compression: Compression = default._compression,
                   interval: Int? = default._interval,
                   providerProperties : Map<String, String>,
                   headers : Map<String, String>,
                   description: String = default._description,
                   active: Boolean = default._active,
                   template: String? = default._template,
                   targetServer: String? = default._server,
                   metrics: MetricsSourceConfiguration = default._metrics,
                   credentialProviderClient: String? = default._credentialProvideClient): AwsMskTargetConfiguration {


            val instance = createTargetConfiguration<AwsMskTargetConfiguration>(description = description,
                active = active,
                targetType = AWS_MSK_TARGET,
                template = template,
                targetServer = targetServer,
                metrics = metrics,
                credentialProviderClient = credentialProviderClient) as AwsMskTargetConfiguration


            with(instance) {
                _topicName = topicName
                _bootstrapServers = bootstrapServers
                _key = key
                _partition = partition
                _batchSize = batchSize
                _compression = compression
                _serialization = serialization
                _acknowledgements = acknowledgements
                _interval = interval
                _headers = headers
                _providerProperties = providerProperties
                _description = description
            }
            return instance
        }

    }

    override val region: Region?
        // not used, here to satisfy AwsService region property
        get() = Region.AWS_GLOBAL

}