
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.config

import com.amazonaws.sfc.metrics.MetricsConfiguration
import com.amazonaws.sfc.metrics.MetricsConfiguration.Companion.CONFIG_METRICS
import com.google.gson.annotations.SerializedName

open class BaseConfigurationWithMetrics : BaseConfiguration(), Validate {
    @SerializedName(CONFIG_METRICS)
    private val _metrics: MetricsConfiguration? = null

    val metrics: MetricsConfiguration?
        get() = _metrics

    override fun validate() {}

    val isCollectingMetrics: Boolean
        get() = ((_metrics != null) && (_metrics.isCollectingMetrics))

    override var validated: Boolean = true
}