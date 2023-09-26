package com.amazonaws.sfc.filters

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.annotations.SerializedName

/**
 * Data filter operator configuration
 * NOTE this configuration is using a custom deserializer to handle nested filter configurations
 * @see  FilterConfigurationDeserializer
 */
@ConfigurationClass
class FilterConfiguration {

    @SerializedName(CONFIG_FILTER_OPERATOR)
    private var _operator: String = ""
    val operator: String
        get() = _operator

    @SerializedName(CONFIG_FILTER_VALUE)
    private var _conditionValue: Any? = null
    val conditionValue: Any?
        get() = _conditionValue


    override fun toString(): String {
        return "FilterConfiguration(operator='$conditionValue', value=$_conditionValue)"
    }

    fun validate() {
        if (validated) return

        ConfigurationException.check(
            FilterBuilder.build(this) != null,
            "$CONFIG_FILTER_OPERATOR \"$operator\" is not a valid filter operator",
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

        private val default = FilterConfiguration()

        fun create(operator: String = default._operator,
                   value: Any? = default._conditionValue): FilterConfiguration {

            val instance = FilterConfiguration()
            with(instance) {
                _operator = operator
                _conditionValue = value
            }
            return instance
        }
    }


}