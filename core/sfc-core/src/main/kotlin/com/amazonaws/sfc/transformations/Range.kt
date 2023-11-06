
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class Range : Validate {

    @SerializedName(CONFIG_RANGE_MIN)
    private var _minValue: Number? = null
    val minValue
        get() = _minValue!!

    @SerializedName(CONFIG_RANGE_MAX)
    private var _maxValue: Number? = null
    val maxValue
        get() = _maxValue!!

    override fun validate() {
        if (validated) return
        validateMInAndMaxAreSet()
        validateMInAndMaxValues()
        validated = true
    }

    private fun validateMInAndMaxValues() {
        ConfigurationException.check(
            minValue != _maxValue,
            "$Range min value $CONFIG_RANGE_MIN  can not be equal to max value $CONFIG_RANGE_MAX",
            "$CONFIG_RANGE_MIN $CONFIG_RANGE_MAX",
            this
        )

        ConfigurationException.check(
            minValue.toDouble() < maxValue.toDouble(),
            "$Range min value $CONFIG_RANGE_MIN  must be less than max value $CONFIG_RANGE_MAX",
            "$CONFIG_RANGE_MIN $CONFIG_RANGE_MAX",
            this
        )
    }

    private fun validateMInAndMaxAreSet() {
        ConfigurationException.check(
            _minValue != null,
            "$Range min value $CONFIG_RANGE_MIN must be set",
            CONFIG_RANGE_MIN,
            this
        )

        ConfigurationException.check(
            _maxValue != null,
            "$Range max value $CONFIG_RANGE_MAX must be set",
            CONFIG_RANGE_MAX,
            this
        )
    }

    private var _validated = false
    override var validated: Boolean
        get() = _validated
        set(value) {
            _validated = value
        }


    override fun toString(): String {
        return "[$minValue..$maxValue]"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Range) return false

        if (_minValue?.toInt() != other._minValue?.toInt()) return false
        if (_maxValue?.toInt() != other._maxValue?.toInt()) return false

        return true
    }

    override fun hashCode(): Int {
        var result = _minValue?.hashCode() ?: 0
        result = 31 * result + (_maxValue?.hashCode() ?: 0)
        return result
    }

    companion object {

        const val CONFIG_RANGE_MIN = "MinValue"
        const val CONFIG_RANGE_MAX = "MaxValue"

        private val default = Range()

        fun create(minValue: Number? = default._minValue, maxValue: Number? = default._maxValue): Range {

            val instance = Range()
            with(instance) {
                _minValue = minValue
                _maxValue = maxValue
            }
            return instance
        }
    }

}