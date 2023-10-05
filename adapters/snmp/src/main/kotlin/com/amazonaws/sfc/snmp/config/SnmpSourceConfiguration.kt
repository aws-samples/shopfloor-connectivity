/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.snmp.config

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANNELS
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class SnmpSourceConfiguration : BaseSourceConfiguration() {

    @SerializedName(CONFIG_SOURCE_ADAPTER_SNMP_DEVICE)
    private var _sourceAdapterDeviceID: String = ""

    /**
     * ID of the device for the adapter device
     */
    val sourceAdapterDeviceID: String
        get() = _sourceAdapterDeviceID

    @SerializedName(CONFIG_CHANNELS)
    private var _channels = mapOf<String, SnmpChannelConfiguration>()
    val channels: Map<String, SnmpChannelConfiguration>
        get() = _channels.filter { !it.key.startsWith(BaseConfiguration.CONFIG_DISABLED_COMMENT) }

    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return

        super.validate()
        validateMustHaveAdapterDevice()
        validateAtLeastOneChannel()
        channels.values.forEach { channel ->
            channel.validate()
        }

        validated = true
    }

    // Device must be set
    private fun validateMustHaveAdapterDevice() =
        ConfigurationException.check(
            (sourceAdapterDeviceID.isNotEmpty()),
            "$$CONFIG_SOURCE_ADAPTER_SNMP_DEVICE for SNMP source must be set",
            CONFIG_SOURCE_ADAPTER_SNMP_DEVICE,
            this
        )

    private fun validateAtLeastOneChannel() =
        ConfigurationException.check(
            (channels.isNotEmpty()),
            "SNMP source must have 1 or more channels",
            CONFIG_CHANNELS,
            this
        )

    companion object {
        private const val CONFIG_SOURCE_ADAPTER_SNMP_DEVICE = "AdapterDevice"

        private val default = SnmpSourceConfiguration()

        fun create(adapterDevice: String = default._sourceAdapterDeviceID,
                   channels: Map<String, SnmpChannelConfiguration> = default._channels,
                   name: String = default._name,
                   description: String = default._description,
                   protocolAdapter: String? = default._protocolAdapterID): SnmpSourceConfiguration {

            val instance = createSourceConfiguration<SnmpSourceConfiguration>(
                name = name,
                description = description,
                protocolAdapter = protocolAdapter)
            with(instance) {
                _sourceAdapterDeviceID = adapterDevice
                _channels = channels
            }
            return instance
        }


    }


}
