/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.pccc.config

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.amazonaws.sfc.pccc.config.PcccConfiguration.Companion.PCCC_ADAPTER
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class PcccAdapterConfiguration : ProtocolAdapterConfiguration(), Validate {

    @SerializedName(CONFIG_CONTROLLERS)
    private var _controllers = mapOf<String, PcccControllerConfiguration>()

    val controllers: Map<String, PcccControllerConfiguration>
        get() = _controllers

    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return
        super.validate()
        controllers.values.forEach { it.validate() }
        validated = true

    }

    companion object {
        const val CONFIG_CONTROLLERS = "Controllers"

        private val default = PcccAdapterConfiguration()

        fun create(
            controllers: Map<String, PcccControllerConfiguration> = default._controllers,
            description: String = default._description,
            metrics: MetricsSourceConfiguration? = default._metrics,
            adapterServer: String? = default._protocolAdapterServer
        ): PcccAdapterConfiguration {

            val instance = createAdapterConfiguration<PcccAdapterConfiguration>(
                description = description,
                adapterType = PCCC_ADAPTER,
                metrics = metrics,
                adapterServer = adapterServer
            )

            with(instance) {
                _controllers = controllers
            }
            return instance
        }

    }
}


