
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.cloudwatch.config

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_BATCH_SIZE
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_INTERVAL
import com.google.gson.annotations.SerializedName
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import kotlin.time.DurationUnit
import kotlin.time.toDuration

// THis class contains the actual cloudwatch configuration of which an instance is added in the CloudWatchMetricsConfiguration class
@ConfigurationClass
class AwsCloudWatchConfiguration : AwsServiceConfig, Validate {
    @SerializedName(BaseConfiguration.CONFIG_REGION)
    private var _region: String? = null


    override val region: Region?
        get() = if (_region.isNullOrEmpty()) null else Region.of(_region!!.lowercase())

    @SerializedName(CONFIG_INTERVAL)
    private var _interval: Int = CONFIG_DEFAULT_CW_WRITE_INTERVAL // in seconds

    val interval: kotlin.time.Duration
        get() = _interval.toDuration(DurationUnit.SECONDS)


    @SerializedName(CONFIG_BATCH_SIZE)
    private var _batchSize: Int = CONFIG_CW_MAX_BATCH_SIZE

    val batchSize
        get() = _batchSize



    // id of a configured credential provider client to obtain credentials to access CloudWatch service
    @SerializedName(BaseConfiguration.CONFIG_CREDENTIAL_PROVIDER_CLIENT)
    private var _credentialProvideClient: String? = null

    override val credentialProviderClient: String?
        get() = _credentialProvideClient


    private var _validated = false

    /**
     * Validates configuration.
     * @throws ConfigurationException
     */
    override fun validate() {

        if (validated) return
        validateServiceRegion()
        validateInterval()
        validateBatchSize()
        validated = true

    }

    override var validated: Boolean
        get() = _validated
        set(value) {
            _validated = value
        }


    // validates AWS region
    private fun validateServiceRegion() {
        if (!_region.isNullOrEmpty()) {
            val validRegions = CloudWatchClient.serviceMetadata().regions().map { it.id() }
            ConfigurationException.check(
                (_region!!.lowercase() in validRegions),
                "${BaseConfiguration.CONFIG_REGION} \"$_region\" is not valid, valid regions are ${validRegions.joinToString()}",
                BaseConfiguration.CONFIG_REGION,
                this
            )
        }
    }

    // Period in which at least metrics must be written nto CloudWatch service
    private fun validateInterval() =
        ConfigurationException.check(
            (_interval > 0),
            "Interval must be 1 or more seconds",
            CONFIG_INTERVAL,
            this)

    // Validates batch size for writing data points to CloudWatch
    private fun validateBatchSize() =
        ConfigurationException.check(
            (_batchSize in 1..CONFIG_CW_MAX_BATCH_SIZE),
            "Batch size for CloudWatch Metrics must be in range 1..$CONFIG_CW_MAX_BATCH_SIZE",
            CONFIG_BATCH_SIZE,
            this
        )

    companion object {
        const val CONFIG_CW_MAX_BATCH_SIZE = 1000
        const val CONFIG_DEFAULT_CW_WRITE_INTERVAL = 60

        private val default = AwsCloudWatchConfiguration()

        fun create(region: String? = default._region,
                   interval: Int = default._interval,
                   batchSize: Int = default._batchSize,
                   credentialProviderClient: String? = default._credentialProvideClient): AwsCloudWatchConfiguration {

            val instance = AwsCloudWatchConfiguration()
            with(instance) {
                _region = region
                _interval = interval
                _batchSize = batchSize
                _credentialProvideClient = credentialProviderClient
            }
            return instance
        }


    }


}