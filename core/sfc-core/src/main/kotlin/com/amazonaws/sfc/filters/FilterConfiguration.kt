// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.filters

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.annotations.SerializedName

/**
 * Data filter operator configuration
 * NOTE this configuration is using a custom deserializer to handle nested filter configurations
 * @see  ValueFilterConfigurationDeserializer
 */
@ConfigurationClass
open class FilterConfiguration {

    override fun toString(): String {
        return "${this::class.java.simpleName}(operator='$conditionValue', value=$_conditionValue)"
    }

    @SerializedName(CONFIG_FILTER_OPERATOR)
    protected var _operator: String = ""
    val operator: String
        get() = _operator

    @SerializedName(CONFIG_FILTER_VALUE)
    protected var _conditionValue: Any? = null
    val conditionValue: Any?
        get() = _conditionValue


    fun validate(filterBuilder: FilterBuilder) {

        ConfigurationException.check(
            filterBuilder.filterOperators.contains(operator),
            "$CONFIG_FILTER_OPERATOR \"$operator\" is not a valid filter operator, valid operators are ${filterBuilder.filterOperators}",
            CONFIG_FILTER_OPERATOR,
            this
        )
    }

    fun validate() {
        if (validated) return

        ConfigurationException.check(
            operator.isNotEmpty(),
            "$CONFIG_FILTER_OPERATOR must be set",
            CONFIG_FILTER_OPERATOR,
            this
        )
        ConfigurationException.check(
            conditionValue != null,
            "$CONFIG_FILTER_VALUE must be specified",
            CONFIG_FILTER_VALUE,
            this
        )

        validated = true
    }


    private var _validated = false
    var validated: Boolean
        get() = _validated
        set(value) {
            _validated = value
        }


    companion object {

        fun from(operator: String, value: Any): FilterConfiguration {
            val c = FilterConfiguration()
            c._conditionValue = value
            c._operator = operator
            return c
        }

        internal const val CONFIG_FILTER_VALUE = "Value"
        internal const val CONFIG_FILTER_OPERATOR = "Operator"

    }


}