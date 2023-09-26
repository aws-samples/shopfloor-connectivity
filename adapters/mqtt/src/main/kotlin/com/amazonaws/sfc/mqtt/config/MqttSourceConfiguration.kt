/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.mqtt.config

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANNELS
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class MqttSourceConfiguration : BaseSourceConfiguration() {

    @SerializedName(CONFIG_SOURCE_ADAPTER_BROKER)
    private var _sourceAdapterbrokerID: String = ""

    /**
     * ID of the opcua server in the adapter device
     */
    val sourceAdapterbrokerID: String
        get() = _sourceAdapterbrokerID

    @SerializedName(CONFIG_CHANNELS)
    private var _channels = mapOf<String, MqttChannelConfiguration>()
    val channels: Map<String, MqttChannelConfiguration>
        get() = _channels.filter { !it.key.startsWith(BaseConfiguration.CONFIG_DISABLED_COMMENT) }

    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return

        super.validate()
        validateMustHaveAdapterBroker()
        validateAtLeastOneChannel()
        channels.values.forEach { channel ->
            channel.validate()
        }


        validated = true
    }

    // Device must be set
    private fun validateMustHaveAdapterBroker() =
        ConfigurationException.check(
            (sourceAdapterbrokerID.isNotEmpty()),
            "$CONFIG_SOURCE_ADAPTER_BROKER for MQTT source must be set",
            CONFIG_SOURCE_ADAPTER_BROKER,
            this
        )

    private fun validateAtLeastOneChannel() =
        ConfigurationException.check(
            (channels.isNotEmpty()),
            "MQTT source must have 1 or more channels",
            CONFIG_CHANNELS,
            this
        )

    companion object {
        private const val CONFIG_SOURCE_ADAPTER_BROKER = "AdapterBroker"

        private val default = MqttSourceConfiguration()

        fun create(channels: Map<String, MqttChannelConfiguration> = default._channels,
                   adapterBroker: String = default._sourceAdapterbrokerID,
                   name: String = default._name,
                   description: String = default._description,
                   protocolAdapter: String? = default._protocolAdapterID): MqttSourceConfiguration {

            val instance = createSourceConfiguration<MqttSourceConfiguration>(
                name = name,
                description = description,
                protocolAdapter = protocolAdapter)

            with(instance) {
                _sourceAdapterbrokerID = adapterBroker
                _channels = channels
            }
            return instance
        }
    }


}
