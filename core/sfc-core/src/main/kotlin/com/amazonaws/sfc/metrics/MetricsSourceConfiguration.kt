
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.metrics

import com.amazonaws.sfc.config.BaseConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.annotations.SerializedName

@ConfigurationClass
open class MetricsSourceConfiguration {
    @SerializedName(BaseConfiguration.CONFIG_ENABLED)
    protected var _enabled: Boolean = true

    val enabled: Boolean
        get() = _enabled

    @SerializedName(CONFIG_METRICS_COMMON_DIMENSIONS)
    protected var _commonDimensions: Map<String, String>? = null

    val commonDimensions: Map<String, String>?
        get() = _commonDimensions

    companion object {


        const val CONFIG_METRICS_COMMON_DIMENSIONS = "CommonDimensions"

        private val default = MetricsSourceConfiguration()

        fun create(
            enabled: Boolean = default._enabled,
            commonDimensions: Map<String, String>? = default._commonDimensions,
        ): MetricsSourceConfiguration {

            val instance = MetricsSourceConfiguration()
            with(instance) {
                _enabled = enabled
                _commonDimensions = commonDimensions
            }
            return instance
        }


    }

}