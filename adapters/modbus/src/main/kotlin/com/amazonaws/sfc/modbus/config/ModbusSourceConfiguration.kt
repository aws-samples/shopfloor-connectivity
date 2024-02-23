
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.config

import com.amazonaws.sfc.config.BaseConfiguration
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANNELS
import com.amazonaws.sfc.config.BaseSourceConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Modbus source configuration
 */
@ConfigurationClass
class ModbusSourceConfiguration : BaseSourceConfiguration() {

    @SerializedName(CONFIG_CHANNELS)
    private var _channels = mapOf<String, ModbusChannelConfiguration>()

    /**
     * Configured modbus channels
     */
    val channels: Map<String, ModbusChannelConfiguration>
        get() = _channels.filter { !it.key.startsWith(BaseConfiguration.CONFIG_DISABLED_COMMENT) }

    @SerializedName(CONFIG_SOURCE_ADAPTER_DEVICE)
    private var _sourceAdapterDevice: String? = null

    /**
     * ID of modbus device
     */
    val sourceAdapterDevice: String
        get() = _sourceAdapterDevice.toString()

    @SerializedName(CONFIG_OPTIMIZATION)
    private var _optimization = ModbusOptimization.DEFAULT_OPTIMIZATION

    /**
     * Optimization options for reading from modbus device
     */
    val optimization: ModbusOptimization
        get() = _optimization

    @SerializedName(CONFIG_READ_TIMEOUT)
    private var _readTimeout: Long = DEFAULT_READ_TIMEOUT_MS

    /**
     * Timeout for reading from Modbus device
     */
    val readTimeout: Duration
        get() = _readTimeout.toDuration(DurationUnit.MILLISECONDS)

    /**
     * Validates configuration.
     * @throws ConfigurationException
     */
    override fun validate() {
        if (validated) return
        super.validate()
        validateReadTimeout()
        validateSourceDevice()
        validateIsAtLeastOneChannel()
        optimization.validate()
        validated = true
    }

    // Device must be set
    private fun validateSourceDevice() =
        ConfigurationException.check(
            (_sourceAdapterDevice != null),
            "$CONFIG_SOURCE_ADAPTER_DEVICE for modbus source must be set",
            CONFIG_SOURCE_ADAPTER_DEVICE,
            this
        )

    // Source must have at least one channel
    private fun validateIsAtLeastOneChannel() =
        ConfigurationException.check(
            (channels.isNotEmpty()),
            "Modbus source must have 1 or more modbus channels",
            CONFIG_CHANNELS,
            this
        )

    // Validate timeout period for reading
    private fun validateReadTimeout() =
        ConfigurationException.check(
            (readTimeout > 0.toDuration(DurationUnit.MILLISECONDS)),
            "Read timeout must be at least 1 millisecond",
            CONFIG_READ_TIMEOUT,
            this
        )

    companion object {
        const val CONFIG_SOURCE_ADAPTER_DEVICE = "AdapterDevice"
        private const val CONFIG_OPTIMIZATION = "Optimization"
        private const val CONFIG_READ_TIMEOUT = "ReadTimeout"
        private const val DEFAULT_READ_TIMEOUT_MS = 10000L

        private val default = ModbusSourceConfiguration()

        fun create(channels: Map<String, ModbusChannelConfiguration> = default._channels,
                   adapterDevice: String? = default._sourceAdapterDevice,
                   optimization: ModbusOptimization = default._optimization,
                   readTimeout: Long = default._readTimeout,
                   name: String = default._name,
                   description: String = default._description,
                   protocolAdapter: String? = default._protocolAdapterID): ModbusSourceConfiguration {

            val instance = createSourceConfiguration<ModbusSourceConfiguration>(
                name = name,
                description = description,
                protocolAdapter = protocolAdapter)

            @Suppress("DuplicatedCode")
            with(instance) {
                _channels = channels
                _sourceAdapterDevice = adapterDevice
                _optimization = optimization
                _readTimeout = readTimeout
            }
            return instance
        }


    }

}
