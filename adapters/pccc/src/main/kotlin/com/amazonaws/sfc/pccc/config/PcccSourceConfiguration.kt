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
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANNELS
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class PcccSourceConfiguration : BaseSourceConfiguration() {

    @SerializedName(CONFIG_ADAPTER_CONTROLLER)
    private var _adapterControllerID: String = ""
    val adapterControllerID: String
        get() = _adapterControllerID

    @SerializedName(CONFIG_CHANNELS)
    private var _channels = mapOf<String, PcccChannelConfiguration>()
    val channels: Map<String, PcccChannelConfiguration>
        get() = _channels.filter { !it.key.startsWith(BaseConfiguration.CONFIG_DISABLED_COMMENT) }


    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return

        super.validate()
        validateMustHaveController()
        validateAtLeastOneChannel()
        channels.values.forEach { channel ->
            channel.validate()
        }

        validated = true
    }

    // Server must be set
    private fun validateMustHaveController() =
        ConfigurationException.check(
            (_adapterControllerID.isNotEmpty()),
            "$CONFIG_ADAPTER_CONTROLLER for PCCC source must be set",
            CONFIG_ADAPTER_CONTROLLER,
            this
        )

    private fun validateAtLeastOneChannel() =
        ConfigurationException.check(
            (channels.isNotEmpty()),
            "PCCC source must have 1 or more channels",
            CONFIG_CHANNELS,
            this
        )

    companion object {
        const val CONFIG_ADAPTER_CONTROLLER = "AdapterController"

        private val default = PcccSourceConfiguration()

        fun create(
            channels: Map<String, PcccChannelConfiguration> = default._channels,
            adapterControllerID: String = default._adapterControllerID,
            name: String = default._name,
            description: String = default._description,
            protocolAdapter: String? = default._protocolAdapterID
        ): PcccSourceConfiguration {

            val instance = createSourceConfiguration<PcccSourceConfiguration>(
                name = name,
                description = description,
                protocolAdapter = protocolAdapter
            )

            with(instance) {
                _adapterControllerID = adapterControllerID
                _channels = channels
            }
            return instance
        }
    }

}
