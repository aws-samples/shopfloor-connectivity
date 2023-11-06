
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.filters

import com.amazonaws.sfc.data.DataTypes.asDoubleValue
import com.amazonaws.sfc.data.DataTypes.isNumeric
import com.amazonaws.sfc.system.DateTime.systemDateTime
import com.amazonaws.sfc.system.DateTime.systemDateTimeUTC
import java.time.Instant


/**
 * Class to apply configured change filter
 * @property configuration ChangeFilterConfiguration
 */
class ChangeFilter(private val configuration: ChangeFilterConfiguration) : Filter {

    // Last value to compare with
    private var _lastPassedValue: Any? = null
    private var lastPassedValue: Any?
        get() = _lastPassedValue
        set(value) {
            _lastPassedValue = value
            val newValue = asDoubleValue(value)
            if (newValue != null) {
                // calculate low-high range at change of last reported value to avoid re-calculation for every time a value is tested
                when (configuration.filterType) {
                    ChangeFilterType.ABSOLUTE -> {
                        low = newValue - configuration.filterValue
                        high = newValue + configuration.filterValue
                    }

                    ChangeFilterType.PERCENT -> {
                        val dif = newValue * (configuration.filterValue / 100)
                        low = newValue - dif
                        high = newValue + dif
                    }

                    else -> {
                        low = newValue
                        high = newValue
                    }
                }
            }
        }

    // Last time value was passed through filter
    private var lastPassedValueTime: Instant? = null

    private var low: Double? = null
    private var high: Double? = null


    /**
     * Apply filter to a value
     * @param value Any? If value passes the filter the input values is returned, else null
     * @return Any?
     */
    override fun apply(value: Any): Boolean {

        // First value always passes filter
        val noPreviousValue = (lastPassedValue == null)
        if (noPreviousValue) {
            lastPassedValue = value
            // Store datetime if atLeast criteria is used
            if (configuration.atLeast != null) {
                lastPassedValueTime = systemDateTimeUTC()
            }
            return true
        }

        // Value did not change
        val sameValue = (lastPassedValue == value)
        if (sameValue) {
            // Test if atLeast criteria is specified
            if (configuration.atLeast != null) {
                val now = systemDateTime()
                val atLeastPeriodExpired = (now.toEpochMilli() - lastPassedValueTime!!.toEpochMilli() >= configuration.atLeast!!)
                // Pass value as time since last value that passed filter was longer than specified for atLeas criteria
                if (atLeastPeriodExpired) {
                    lastPassedValueTime = now
                    return true
                }
                return false
            }
            return false
        }

        // Value is different from previous value, pass if always filter type is used
        if (configuration.filterType == ChangeFilterType.ALWAYS) {
            return true
        }

        // Test is value is a numeric
        if (!isNumeric(value::class)) {
            throw IllegalArgumentException("Can not apply change filter of type $configuration.filterType on value \"$value\" (${value::class.java.simpleName}")
        } else {

            // Calculate values and difference as double values
            val valueAsDouble: Double = asDoubleValue(value) ?: return false

            if (isOutsideOfFilterRange(valueAsDouble)) {
                lastPassedValueTime = systemDateTime()
                lastPassedValue = value
                return true
            }
            return false
        }
    }

    private fun isOutsideOfFilterRange(valueAsDouble: Double) =
        (high == null || low == null) || (valueAsDouble <= low!!) || (valueAsDouble >= high!!)

    override fun toString(): String {
        return "ChangeFilter(Type: ${configuration.filterType}, " +
               "Value: ${configuration.filterValue}${if (configuration.filterType == ChangeFilterType.PERCENT) "%" else ""}" +
               "${if (low != null && high != null) ", Exclusion range: $low...$high" else ", datatype of value not supported by filter"})"
    }


}


