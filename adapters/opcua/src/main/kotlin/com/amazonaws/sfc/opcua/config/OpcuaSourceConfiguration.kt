/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.opcua.config

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANNELS
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class OpcuaSourceConfiguration : BaseSourceConfiguration() {

    @SerializedName(CONFIG_SOURCE_ADAPTER_OPCUA_SERVER)
    private var _sourceAdapterOpcuaServerID: String = ""

    /**
     * ID of the opcua server in the adapter device
     */
    val sourceAdapterOpcuaServerID: String
        get() = _sourceAdapterOpcuaServerID

    @SerializedName(CONFIG_CHANNELS)
    private var _channels = mapOf<String, OpcuaNodeChannelConfiguration>()
    val channels: Map<String, OpcuaNodeChannelConfiguration>
        get() = _channels.filter { !it.key.startsWith(BaseConfiguration.CONFIG_DISABLED_COMMENT) }

    @SerializedName(CONFIG_SOURCE_READING_MODE)
    private var _readingMode: OpcuaSourceReadingMode = OpcuaSourceReadingMode.SUBSCRIPTION
    val readingMode: OpcuaSourceReadingMode
        get() = _readingMode

    @SerializedName(CONFIG_SUBSCRIBE_PUBLISHING_INTERVAL)
    private var _subscribePublishingInterval: Long? = null
    val subscribePublishingInterval: Duration?
        get() = _subscribePublishingInterval?.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_EVENT_QUEUE_SIZE)
    private var _eventQueueSize: Int = CONFIG_DEFAULT_EVENT_QUEUE_SIZE
    val eventQueueSize: Int
        get() = _eventQueueSize

    @SerializedName(CONFIG_EVENT_SAMPLING_INTERVAL)
    private var _eventSamplingInterval: Int = CONFIG_DEFAULT_EVENT_SAMPLING_INTERVAL
    val eventSamplingInterval: Int
        get() = _eventSamplingInterval


    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return
        super.validate()
        validateMustHaveAdapterOpcuaServer()
        validateAtLeastOneNode()

        channels.values.forEach { channel ->
            channel.validate()
        }

        validated = true
    }

    private fun validateMustHaveAdapterOpcuaServer() =
        ConfigurationException.check(
            (sourceAdapterOpcuaServerID.isNotEmpty()),
            "$CONFIG_SOURCE_ADAPTER_OPCUA_SERVER must be set",
            CONFIG_SOURCE_ADAPTER_OPCUA_SERVER,
            this
        )

    private fun validateAtLeastOneNode() =
        ConfigurationException.check(
            (channels.isNotEmpty()),
            "OPC UA source must have 1 or more node channels",
            CONFIG_CHANNELS,
            this
        )

    companion object {
        const val CONFIG_SOURCE_ADAPTER_OPCUA_SERVER = "AdapterOpcuaServer"
        const val CONFIG_SOURCE_READING_MODE = "SourceReadingMode"
        const val CONFIG_SUBSCRIBE_PUBLISHING_INTERVAL = "SubscribePublishingInterval"
        const val CONFIG_EVENT_QUEUE_SIZE = "EventQueueSize"
        const val CONFIG_EVENT_SAMPLING_INTERVAL = "EventSamplingInterval"

        const val CONFIG_DEFAULT_EVENT_QUEUE_SIZE = 10
        const val CONFIG_DEFAULT_EVENT_SAMPLING_INTERVAL = 0

        private val default = OpcuaSourceConfiguration()

        fun create(adapterOpcuaServer: String = default._sourceAdapterOpcuaServerID,
                   channels: Map<String, OpcuaNodeChannelConfiguration> = default._channels,
                   sourceReadingMode: OpcuaSourceReadingMode = default._readingMode,
                   subscribePublishingInterval: Long? = default._subscribePublishingInterval,
                   name: String = default._name,
                   description: String = default._description,
                   protocolAdapter: String? = default._protocolAdapterID,
                   eventQueueSize: Int = default._eventQueueSize,
                   eventSamplingInterval: Int = default._eventSamplingInterval): OpcuaSourceConfiguration {

            val instance = createSourceConfiguration<OpcuaSourceConfiguration>(
                name = name,
                description = description,
                protocolAdapter = protocolAdapter)

            with(instance) {
                _sourceAdapterOpcuaServerID = adapterOpcuaServer
                _channels = channels
                _readingMode = sourceReadingMode
                _subscribePublishingInterval = subscribePublishingInterval
                _eventQueueSize = eventQueueSize
                _eventSamplingInterval = eventSamplingInterval
            }
            return instance
        }
    }


}
