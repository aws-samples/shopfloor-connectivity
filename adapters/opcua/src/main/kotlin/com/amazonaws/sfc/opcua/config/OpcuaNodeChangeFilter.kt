
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.opcua.config

import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName

class OpcuaNodeChangeFilter : Validate {
    @SerializedName(CONFIG_FILTER_VALUE)
    private var _filterValue: Double = 0.0
    val filterValue: Double
        get() = _filterValue

    @SerializedName(CONFIG_FILTER_TYPE)
    private var _filterType: OpcuaChangeFilterType = OpcuaChangeFilterType.ABSOLUTE
    val filterType: OpcuaChangeFilterType
        get() = _filterType

    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    override fun validate() {
        if (validated) return
        ConfigurationException.check(
            (filterType == OpcuaChangeFilterType.ABSOLUTE || filterType == OpcuaChangeFilterType.PERCENT),
            "Filter $CONFIG_FILTER_TYPE must be \"Absolute\" or \"Percent\"",
            CONFIG_FILTER_TYPE,
            this
        )
        validated = true


    }

    companion object {
        private const val CONFIG_FILTER_VALUE = "Value"
        private const val CONFIG_FILTER_TYPE = "Type"

        private val default = OpcuaNodeChangeFilter()

        fun create(value: Double = default._filterValue,
                   type: OpcuaChangeFilterType = default._filterType): OpcuaNodeChangeFilter {

            val instance = OpcuaNodeChangeFilter()
            with(instance) {
                _filterValue = value
                _filterType = type
            }
            return instance
        }

    }


}


