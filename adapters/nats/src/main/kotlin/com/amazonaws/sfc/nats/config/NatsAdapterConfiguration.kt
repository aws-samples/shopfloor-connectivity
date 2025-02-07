// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.nats.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.ProtocolAdapterConfiguration
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.amazonaws.sfc.nats.config.NatsConfiguration.Companion.NATS_ADAPTER
import com.google.gson.annotations.SerializedName
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class NatsAdapterConfiguration : ProtocolAdapterConfiguration(), Validate {

    @SerializedName(CONFIG_NATS_SERVERS)
    private var _servers = mapOf<String, NatsServerConfiguration>()

    val servers: Map<String, NatsServerConfiguration>
        get() = _servers

    @SerializedName(CONFIG_RECEIVED_DATA_CHANNEL_SIZE)
    private val _receivedDataChannelSize = DEFAULT_RECEIVED_DATA_CHANNEL_SIZE
    val receivedDataChannelSize
        get() = _receivedDataChannelSize

    @SerializedName(CONFIG_RECEIVED_DATA_CHANNEL_TIMEOUT)
    private val _receivedDataChannelTimeout = DEFAULT_RECEIVED_DATA_CHANNEL_TIMEOUT
    val receivedDataChannelTimeout
        get() = _receivedDataChannelTimeout.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_READ_MODE)
    private var _readMode = ReadMode.KEEP_LAST
    val readMode: ReadMode
        get() = _readMode

    @SerializedName(CONFIG_MAX_RETAIN_SIZE)
    private var _maxRetainSize: Int = DEFAULT_MAX_RETAIN_SIZE
    val maxRetainSize: Int
        get() = _maxRetainSize

    @SerializedName(CONFIG_MAX_RETAIN_PERIOD)
    private var _maxRetainPeriod: Int = DEFAULT_MAX_RETAIN_PERIOD
    val maxRetainPeriod: Int
        get() = _maxRetainPeriod


    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return
        super.validate()
        servers.values.forEach { it.validate() }
        validated = true

    }

    companion object {
        const val CONFIG_NATS_SERVERS = "Servers"
        const val CONFIG_RECEIVED_DATA_CHANNEL_SIZE = "ReceivedDataChannelSize"
        const val CONFIG_RECEIVED_DATA_CHANNEL_TIMEOUT = "ReceivedDataChannelTimeout"

        const val CONFIG_READ_MODE = "ReadMode"

        const val DEFAULT_RECEIVED_DATA_CHANNEL_SIZE = 1000
        const val DEFAULT_RECEIVED_DATA_CHANNEL_TIMEOUT = 1000
        const val CONFIG_MAX_RETAIN_SIZE = "MaxRetainSize"
        const val CONFIG_MAX_RETAIN_PERIOD = "MaxRetainPeriod"

        const val DEFAULT_MAX_RETAIN_PERIOD = 1000 * 60 * 60
        const val DEFAULT_MAX_RETAIN_SIZE = 10000

        private val default = NatsAdapterConfiguration()

        fun create(servers: Map<String, NatsServerConfiguration> = default._servers,
                   description: String = default._description,
                   metrics: MetricsSourceConfiguration? = default._metrics,
                   readMode: ReadMode = default._readMode,
                   maxRetainSize: Int = default._maxRetainSize,
                   maxRetainPeriod: Int = default._maxRetainPeriod,
                   adapterServer: String? = default._protocolAdapterServer): NatsAdapterConfiguration {

            val instance = createAdapterConfiguration<NatsAdapterConfiguration>(
                description = description,
                adapterType = NATS_ADAPTER,
                metrics = metrics,
                adapterServer = adapterServer)

            with(instance) {
                _servers = servers
                _readMode = readMode
                _maxRetainSize = maxRetainSize
                _maxRetainPeriod = maxRetainPeriod
            }
            return instance
        }


    }

}


