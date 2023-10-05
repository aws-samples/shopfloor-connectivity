/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.mqtt.config

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.amazonaws.sfc.mqtt.config.MqttConfiguration.Companion.MQTT_ADAPTER
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class MqttAdapterConfiguration : ProtocolAdapterConfiguration(), Validate {

    @SerializedName(CONFIG_MQTT_BROKERS_SERVERS)
    private var _brokers = mapOf<String, MqttBrokerConfiguration>()

    val brokers: Map<String, MqttBrokerConfiguration>
        get() = _brokers

    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return
        super.validate()
        super.validate()
        brokers.values.forEach { it.validate() }
        validated = true

    }

    companion object {
        const val CONFIG_MQTT_BROKERS_SERVERS = "Brokers"
        private val default = MqttAdapterConfiguration()

        fun create(brokers: Map<String, MqttBrokerConfiguration> = default._brokers,
                   description: String = default._description,
                   metrics: MetricsSourceConfiguration? = default._metrics,
                   adapterServer: String? = default._protocolAdapterServer): MqttAdapterConfiguration {

            val instance = createAdapterConfiguration<MqttAdapterConfiguration>(
                description = description,
                adapterType = MQTT_ADAPTER,
                metrics = metrics,
                adapterServer = adapterServer)

            with(instance) {
                _brokers = brokers
            }
            return instance
        }


    }

}


