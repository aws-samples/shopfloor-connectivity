/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

@file:Suppress("PropertyName", "PropertyName", "PropertyName", "PropertyName", "PropertyName")

package com.amazonaws.sfc.metrics

import com.amazonaws.sfc.config.*
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Suppress("PropertyName")
@ConfigurationClass
open class MetricsConfiguration : MetricsSourceConfiguration(), Validate {

    @SerializedName(CONFIG_METRICS_INTERVAL)
    protected var _interval: Long = CONFIG_DEFAULT_METRICS_INTERVAL
    val interval: Duration
        get() = _interval.toDuration(DurationUnit.SECONDS)

    // Configuration of the actual writer, which can be an IPC client or in-process instance
    @SerializedName(CONFIG_METRICS_WRITER_CONFIG)
    protected var _writer: MetricsWriterConfiguration? = null
    val writer: MetricsWriterConfiguration?
        get() = _writer

    @SerializedName(CONFIG_METRICS_NAMESPACE)
    protected var _namespace: String = DEFAULT_METRICS_NAMESPACE
    val nameSpace: String
        get() = _namespace

    val isCollectingMetrics: Boolean
        get() = enabled && (writer != null) && ((_writer?.metricsWriter != null || _writer?.metricsServer != null))

    @SerializedName(CONFIG_METRICS_CORE)
    protected var _collectCoreMetrics: Boolean = true
    val collectCoreMetrics
        get() = _collectCoreMetrics

    val isCoreCollectingMetrics: Boolean
        get() = isCollectingMetrics && collectCoreMetrics


    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    var _validated = false

    override fun validate() {
        if (validated) return

        ConfigurationException.check(
            _writer != null,
            "$CONFIG_METRICS_WRITER_CONFIG not specified",
            CONFIG_METRICS_WRITER_CONFIG,
            this)

        _writer?.validate()
        validateInterval()
        validated = true
    }

    // Validates the interval for reading metrics
    private fun validateInterval() =
        ConfigurationException.check(
            (_interval > 0),
            "Interval must be 1 or more seconds",
            BaseConfiguration.CONFIG_INTERVAL,
            this)


    companion object {
        const val CONFIG_METRICS_WRITER_CONFIG = "Writer"

        const val CONFIG_METRICS_INTERVAL = "Interval"
        const val CONFIG_DEFAULT_METRICS_INTERVAL = 10L
        const val CONFIG_METRICS = "Metrics"
        const val CONFIG_METRICS_NAMESPACE = "Namespace"
        const val DEFAULT_METRICS_NAMESPACE = "SFC"
        const val CONFIG_METRICS_CORE = "CollectCoreMetrics"

        private val default = MetricsConfiguration()

        fun create(enabled: Boolean = default._enabled,
                   collectCoreMetrics: Boolean = default._collectCoreMetrics,
                   commonDimensions: Map<String, String>? = default._commonDimensions,
                   namespace: String = default._namespace,
                   interval: Duration = default._interval.toDuration(DurationUnit.SECONDS),
                   writer: MetricsWriterConfiguration? = default._writer): MetricsSourceConfiguration {

            val instance = MetricsConfiguration()
            with(instance) {
                _enabled = enabled
                _collectCoreMetrics = collectCoreMetrics
                _commonDimensions = commonDimensions
                _namespace = namespace
                _interval = interval.inWholeSeconds
                _writer = writer
            }
            return instance
        }


    }
}