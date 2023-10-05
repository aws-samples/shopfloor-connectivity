/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.awskinesis.config

import com.amazonaws.sfc.client.AwsServiceTargetsConfig
import com.amazonaws.sfc.config.BaseConfigurationWithMetrics
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.annotations.SerializedName

/**
 * AWS Kinesis target configuration
 */
@ConfigurationClass
class AwsKinesisWriterConfiguration : AwsServiceTargetsConfig<AwsKinesisTargetConfiguration>, BaseConfigurationWithMetrics() {

    @SerializedName(CONFIG_TARGETS)
    private val _targets: Map<String, AwsKinesisTargetConfiguration> = emptyMap()

    /**
     * Target Kinesis stream configurations
     * @see AwsKinesisTargetConfiguration
     */
    override val targets: Map<String, AwsKinesisTargetConfiguration>
        get() = _targets.filter { (it.value.targetType == AWS_KINESIS_TARGET) }

    /**
     * Validates the configuration.
     * @throws ConfigurationException
     */
    override fun validate() {
        if (validated) return

        super.validate()
        targets.forEach {
            it.value.validate()
        }
        validated = true

    }

    companion object {
        const val AWS_KINESIS_TARGET = "AWS-KINESIS"
    }


}