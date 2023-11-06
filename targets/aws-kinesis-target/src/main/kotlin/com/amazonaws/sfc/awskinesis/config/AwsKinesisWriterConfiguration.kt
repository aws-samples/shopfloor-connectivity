
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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