
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.config

import com.amazonaws.sfc.metrics.MetricsConfiguration
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class ConfigWithMetrics : Validate {
    @SerializedName(MetricsConfiguration.CONFIG_METRICS)
    private var _metrics: MetricsConfiguration? = null

    val metrics: MetricsConfiguration?
        get() = _metrics


    override fun validate() {}

    override var validated: Boolean = true

    companion object {
        private val default = ConfigWithMetrics()

        fun create(metricsConfiguration: MetricsConfiguration): ConfigWithMetrics {

            val instance = ConfigWithMetrics()
            with(instance) {
                _metrics = metricsConfiguration
            }
            return instance
        }
    }
}