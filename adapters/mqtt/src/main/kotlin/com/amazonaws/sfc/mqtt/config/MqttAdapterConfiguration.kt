
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.mqtt.config

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.amazonaws.sfc.mqtt.config.MqttConfiguration.Companion.MQTT_ADAPTER
import com.google.gson.annotations.SerializedName
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class MqttAdapterConfiguration : ProtocolAdapterConfiguration(), Validate {

    @SerializedName(CONFIG_MQTT_BROKERS_SERVERS)
    private var _brokers = mapOf<String, MqttBrokerConfiguration>()

    val brokers: Map<String, MqttBrokerConfiguration>
        get() = _brokers

    @SerializedName(CONFIG_RECEIVED_DATA_CHANNEL_SIZE)
    protected val _receivedDataChannelSize = DEFAULT_RECEIVED_DATA_CHANNEL_SIZE
    val receivedDataChannelSize
        get() = _receivedDataChannelSize

    @SerializedName(CONFIG_RECEIVED_DATA_CHANNEL_TIMEOUT)
    protected val _receivedDataChannelTimeout = DEFAULT_RECEIVED_DATA_CHANNEL_TIMEOUT
    val receivedDataChannelTimeout
        get() = _receivedDataChannelTimeout.toDuration(DurationUnit.MILLISECONDS)

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
        const val CONFIG_RECEIVED_DATA_CHANNEL_SIZE = "ReceivedDataChannelSize"
        const val CONFIG_RECEIVED_DATA_CHANNEL_TIMEOUT = "ReceivedDataChannelTimeout"

        const val DEFAULT_RECEIVED_DATA_CHANNEL_SIZE = 1000
        const val DEFAULT_RECEIVED_DATA_CHANNEL_TIMEOUT = 1000

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


