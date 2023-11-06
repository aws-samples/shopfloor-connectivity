
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.config

import com.google.gson.annotations.SerializedName

class MetricsWriterConfiguration : Validate {
    @SerializedName(CONFIG_METRICS_METRICS_WRITER)
    private var _metricsWriter: InProcessConfiguration? = null
    val metricsWriter: InProcessConfiguration?
        get() = _metricsWriter

    @SerializedName(CONFIG_METRICS_METRICS_SERVER)
    private var _metricsServer: ServerConfiguration? = null
    val metricsServer: ServerConfiguration?
        get() = _metricsServer

    private var _validated = false

    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    override fun validate() {
        ConfigurationException.check(
            (metricsWriter != null) || (metricsServer != null),
            "No $CONFIG_METRICS_METRICS_SERVER or $CONFIG_METRICS_METRICS_SERVER specified for ${this::class.simpleName}",
            "$CONFIG_METRICS_METRICS_SERVER, $CONFIG_METRICS_METRICS_WRITER",
            this)

        metricsWriter?.validate()

        metricsServer?.validate()

        validated = true
    }

    companion object {
        const val CONFIG_METRICS_METRICS_WRITER = "MetricsWriter"
        const val CONFIG_METRICS_METRICS_SERVER = "MetricsServer"
    }
}