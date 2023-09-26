package com.amazonaws.sfc.filters

import com.amazonaws.sfc.data.DataTypes.numericCompare


/**
 * Implements LE (<=) operator
 * @property value Any Tested value
 */
class LessOrEqualFilter(private val value: Any) : Filter {

    /**
     * Tests if value is <= operator condition value
     * @param value Any
     * @return Boolean True if value <= operator value
     */
    override fun apply(value: Any): Boolean {
        return numericCompare(value, this.value) <= 0
    }

    /**
     * String representation of LE operator
     * @return String
     */
    override fun toString(): String {
        return "$OPERATOR_LE_STR($value)"
    }

    companion object {
        private const val OPERATOR_LE_STR = "le"
        private const val OPERATOR_LE = "<="


        /**
         * Creates instance of operator
         * @param configuration FilterConfiguration
         * @return Filter
         */
        private fun create(configuration: FilterConfiguration): Filter {
            return LessOrEqualFilter(configuration.conditionValue!!)
        }

        /**
         * Registers operator as known type
         */
        fun register() {
            FilterBuilder.registerOperator(OPERATOR_LE, OPERATOR_LE_STR) { c -> create(c) }
        }
    }

}