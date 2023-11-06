
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.modbus.config

import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_ENABLED
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName

/**
 * Options for optimizing call to read from modbus devices
 */
class ModbusOptimization : Validate {

    @SerializedName(CONFIG_ENABLED)
    private var _enabled = true
    val enabled: Boolean
        get() = _enabled

    @SerializedName(CONFIG_COIL_MAX_GAP_SIZE)
    private var _coilMaxGapSize: Int = DEFAULT_COIL_MAX_GAP_SIZE

    /**
     * Max size of a gap in between adjacent coil address ranges that are combined into single reads.
     */
    val coilMaxGapSize: Int
        get() = _coilMaxGapSize


    @SerializedName(CONFIG_REGISTER_MAX_GAP_SIZE)
    private var _registerMaxGapSize: Int = DEFAULT_REGISTER_MAX_GAP_SIZE

    /**
     * Max size of a gap in between adjacent register address ranges that are combined into single reads.
     */
    val registerMaxGapSize: Int
        get() = _registerMaxGapSize

    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    /**
     * Validates configuration.
     * @throws ConfigurationException
     */
    override fun validate() {
        if (validated) return
        validateCoilMaxGapSize()
        validateRegisterMaxGapSize()
        validated = true
    }

    // validates register gap size
    private fun validateRegisterMaxGapSize() =
        ConfigurationException.check(
            (registerMaxGapSize > 0),
            "Register max gap $CONFIG_REGISTER_MAX_GAP_SIZE size must be 1 or more",
            CONFIG_REGISTER_MAX_GAP_SIZE,
            this
        )

    // validates coil gap size
    private fun validateCoilMaxGapSize() =
        ConfigurationException.check(
            (coilMaxGapSize > 0),
            "Coil max gap $CONFIG_COIL_MAX_GAP_SIZE size must be 1 or more",
            CONFIG_COIL_MAX_GAP_SIZE,
            this
        )


    companion object {

        private const val DEFAULT_COIL_MAX_GAP_SIZE = 16
        private const val DEFAULT_REGISTER_MAX_GAP_SIZE = 8
        private const val CONFIG_COIL_MAX_GAP_SIZE = "CoilMaxGapSize"
        private const val CONFIG_REGISTER_MAX_GAP_SIZE = "RegisterMaxGapSize"

        var DEFAULT_OPTIMIZATION: ModbusOptimization = create(enabled = true,
            coilMaxGapSize = DEFAULT_COIL_MAX_GAP_SIZE,
            registerMaxGapSize = DEFAULT_REGISTER_MAX_GAP_SIZE)


        private val default = ModbusOptimization()

        fun create(enabled: Boolean = default._enabled,
                   coilMaxGapSize: Int = default._coilMaxGapSize,
                   registerMaxGapSize: Int = default._registerMaxGapSize): ModbusOptimization {

            val instance = ModbusOptimization()
            with(instance) {
                _enabled = enabled
                _coilMaxGapSize = coilMaxGapSize
                _registerMaxGapSize = registerMaxGapSize
            }
            return instance
        }

    }
}