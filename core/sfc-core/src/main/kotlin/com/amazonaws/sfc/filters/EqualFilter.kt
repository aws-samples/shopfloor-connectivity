package com.amazonaws.sfc.filters

import com.amazonaws.sfc.data.DataTypes.numericCompare


/**
 * Implements EQ filter operator
 * @property value Any Value to test against (can be String boolean or a number)
 * @constructor
 */
class EqualFilter(private val value: Any) : Filter {

    /**
     * Tests strings, booleans and numbers for equality
     * @param value Any
     * @return Boolean
     */
    override fun apply(value: Any): Boolean =
        when (value) {
            is String -> value == this.value.toString()
            is Boolean -> if (this.value is Boolean) value == this.value else throw IllegalArgumentException(
                "Can not compare boolean value $value with ${this.value}"
            )

            else -> numericCompare(value, this.value) == 0
        }


    /**
     * String representation of operator
     * @return String
     */
    override fun toString(): String {
        return "$OPERATOR_EQ_STR($value)"
    }

    companion object {
        private const val OPERATOR_EQ_STR = "eq"
        private const val OPERATOR_EQ = "=="


        /**
         * Creates operator instance
         * @param configuration FilterConfiguration
         * @return Filter
         */
        private fun create(configuration: FilterConfiguration): Filter {
            return EqualFilter(configuration.conditionValue!!)
        }

        /**
         * Registers operator as known instance
         */
        fun register() {
            FilterBuilder.registerOperator(OPERATOR_EQ, OPERATOR_EQ_STR) { c -> create(c) }
        }
    }

}