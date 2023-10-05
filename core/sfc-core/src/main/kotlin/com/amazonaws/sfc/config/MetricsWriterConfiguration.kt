/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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