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