/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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