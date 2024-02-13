
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsmsk.config

import com.amazonaws.sfc.client.AwsServiceTargetsConfig
import com.amazonaws.sfc.config.BaseConfigurationWithMetrics
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class AwsMskWriterConfiguration : AwsServiceTargetsConfig<AwsMskTargetConfiguration>, BaseConfigurationWithMetrics() {

    @SerializedName(CONFIG_TARGETS)
    private val _targets: Map<String, AwsMskTargetConfiguration> = emptyMap()


    override val targets: Map<String, AwsMskTargetConfiguration>
        get() = _targets.filter { (it.value.targetType == AWS_MSK_TARGET) }

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
        const val AWS_MSK_TARGET = "AWS-MSK"
    }


}