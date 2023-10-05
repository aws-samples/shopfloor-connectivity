/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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