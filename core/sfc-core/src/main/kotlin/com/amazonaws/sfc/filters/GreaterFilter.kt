package com.amazonaws.sfc.filters

import com.amazonaws.sfc.data.DataTypes.numericCompare


/**
 * Implements GT (>) operator
 * @property value Any Tested value
 */
class GreaterFilter(private val value: Any) : Filter {

    /**
     * Tests if value is > operator condition value
     * @param value Any
     * @return Boolean True if value > operator value
     */
    override fun apply(value: Any): Boolean {
        return numericCompare(value, this.value) > 0
    }

    /**
     * String representation of GT operator
     * @return String
     */
    override fun toString(): String {
        return "OPERATOR_GT_STR($value)"
    }

    companion object {
        private const val OPERATOR_GT_STR = "gt"
        private const val OPERATOR_GT = ">"

        /**
         * Creates instance of operator
         * @param configuration FilterConfiguration
         * @return Filter
         */
        private fun create(configuration: FilterConfiguration): Filter {
            return GreaterFilter(configuration.conditionValue!!)
        }

        /**
         * Registers operator as known type
         */
        fun register() {
            FilterBuilder.registerOperator(OPERATOR_GT, OPERATOR_GT_STR) { c -> create(c) }
        }
    }
}