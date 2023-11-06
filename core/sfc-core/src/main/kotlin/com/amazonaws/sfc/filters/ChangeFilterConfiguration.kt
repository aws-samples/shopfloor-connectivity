
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.filters

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName

/**
 * Configuration for filtering data based on change of value
 */
@ConfigurationClass
class ChangeFilterConfiguration : Validate {

    @SerializedName(CONFIG_FILTER_VALUE)
    private var _filterValue: Double = 0.0
    val filterValue: Double
        get() = _filterValue

    @SerializedName(CONFIG_FILTER_TYPE)
    private var _filterType: ChangeFilterType = ChangeFilterType.ALWAYS
    val filterType: ChangeFilterType
        get() = _filterType

    @SerializedName(CONFIG_FILTER_AT_LEAST)
    private var _atLeast: Long? = null
    val atLeast: Long?
        get() = _atLeast

    /**
     * Validates configuration of a data change filter
     */
    override fun validate() {
        if (validated) return
        validateAtLeast()
        validateFilterValue()
        validated = true
    }

    private fun validateFilterValue() {
        ConfigurationException.check(
            (filterValue >= 0),
            "$CONFIG_FILTER_VALUE must be 0 or more",
            CONFIG_FILTER_VALUE,
            this
        )
    }

    private fun validateAtLeast() {
        if (atLeast != null) {
            ConfigurationException.check(
                (atLeast!! >= 0),
                "$CONFIG_FILTER_AT_LEAST must be 0 or more",
                CONFIG_FILTER_AT_LEAST,
                this
            )
        }
    }

    private var _validated = false
    override var validated: Boolean
        get() = _validated
        set(value) {
            _validated = value
        }


    companion object {
        private const val CONFIG_FILTER_VALUE = "Value"
        private const val CONFIG_FILTER_TYPE = "Type"
        private const val CONFIG_FILTER_AT_LEAST = "AtLeast"


        private val default = ChangeFilterConfiguration()

        fun create(value: Double = default._filterValue,
                   type: ChangeFilterType = default._filterType,
                   atLeast: Long? = default._atLeast): ChangeFilterConfiguration {

            val instance = ChangeFilterConfiguration()

            with(instance) {
                _filterValue = value
                _filterType = type
                _atLeast = atLeast
            }
            return instance
        }
    }

}


