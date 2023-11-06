
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.cloudwatch.config

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.metrics.MetricsConfiguration
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class AwsCloudWatchMetricsConfiguration : MetricsConfiguration(), Validate {

    @SerializedName(CONFIG_CLOUDWATCH)
    private var _cloudWatchConfiguration: AwsCloudWatchConfiguration? = null

    val cloudWatch: AwsCloudWatchConfiguration
        get() = _cloudWatchConfiguration ?: AwsCloudWatchConfiguration()

    /**
     * Validates configuration.
     * @throws ConfigurationException
     */
    override fun validate() {

        if (validated) return
        cloudWatch.validate()

        validated = true

    }

    override var validated: Boolean
        get() = _validated
        set(value) {
            _validated = value
        }


    companion object {

        private const val CONFIG_CLOUDWATCH = "CloudWatch"
        private val default = AwsCloudWatchMetricsConfiguration()

        fun create(cloudWatch: AwsCloudWatchConfiguration? = default._cloudWatchConfiguration,
                   interval: Duration = default._interval.toDuration(DurationUnit.SECONDS),
                   writer: MetricsWriterConfiguration? = default._writer,
                   namespace: String = default._namespace,
                   enabled: Boolean = default._enabled,
                   commonDimensions: Map<String, String>? = default._commonDimensions): AwsCloudWatchMetricsConfiguration {

            val instance = AwsCloudWatchMetricsConfiguration()
            with(instance) {
                _cloudWatchConfiguration = cloudWatch
                _interval = interval.inWholeSeconds
                _writer = writer
                _namespace = namespace
                _enabled = enabled
                _commonDimensions = commonDimensions
            }
            return instance
        }
    }

}