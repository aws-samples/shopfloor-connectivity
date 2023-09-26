package com.amazonaws.sfc.filters

import com.amazonaws.sfc.data.DataTypes.numericCompare

/**
 * Implements GE (>=) operator
 * @property value Any Tested value
 */
class GreaterOrEqualFilter(private val value: Any) : Filter {


    /**
     * Tests if value is >= operator condition value
     * @param value Any
     * @return Boolean True if value >= operator value
     */
    override fun apply(value: Any): Boolean {
        return numericCompare(value, this.value) >= 0
    }

    /**
     * String representation of GE operator
     * @return String
     */
    override fun toString(): String {
        return "$OPERATOR_GE_STR($value)"
    }

    companion object {
        private const val OPERATOR_GE_STR = "ge"
        private const val OPERATOR_GE = ">="

        /**
         * Creates instance of operator
         * @param configuration FilterConfiguration
         * @return Filter
         */
        private fun create(configuration: FilterConfiguration): Filter {
            return GreaterOrEqualFilter(configuration.conditionValue!!)
        }

        /**
         * Registers operator as known type
         */
        fun register() {
            FilterBuilder.registerOperator(OPERATOR_GE, OPERATOR_GE_STR) { c -> create(c) }
        }
    }

}