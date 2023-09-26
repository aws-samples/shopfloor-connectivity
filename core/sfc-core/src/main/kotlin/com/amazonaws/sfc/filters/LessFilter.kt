package com.amazonaws.sfc.filters

import com.amazonaws.sfc.data.DataTypes.numericCompare

/**
 * Implements LT filter operator
 * @property value Any Value to test against
 * @constructor
 */
class LessFilter(private val value: Any) : Filter {

    /**
     * Tests if value is < operator condition value
     * @param value Any
     * @return Boolean True if value < operator value
     */
    override fun apply(value: Any): Boolean {
        return numericCompare(value, this.value) < 0
    }

    /**
     * String representation of LT operator
     * @return String
     */
    override fun toString(): String {
        return "$OPERATOR_LT_STR($value)"
    }

    companion object {
        private const val OPERATOR_LT_STR = "lt"
        private const val OPERATOR_LT = "<"

        /**
         * Creates instance of operator
         * @param configuration FilterConfiguration
         * @return Filter
         */
        private fun create(configuration: FilterConfiguration): Filter {
            return LessFilter(configuration.conditionValue!!)
        }

        /**
         * Registers operator as known type
         */
        fun register() {
            FilterBuilder.registerOperator(OPERATOR_LT, OPERATOR_LT_STR) { c -> create(c) }
        }
    }

}