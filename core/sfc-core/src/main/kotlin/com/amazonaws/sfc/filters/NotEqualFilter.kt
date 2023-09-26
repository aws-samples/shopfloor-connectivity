package com.amazonaws.sfc.filters

/**
 * Implements NE filter operator
 * @property value Any Value to test against (can be String boolean or a number)
 * @constructor
 */
class NotEqualFilter(private val value: Any) : Filter {

    // used EQ operator to implement NE
    private val eq = EqualFilter(value)

    /**
     * Tests strings, booleans and numbers for non equality
     * @param value Any
     * @return Boolean
     */
    override fun apply(value: Any): Boolean {
        return !eq.apply(value)
    }

    /**
     * String representation of operator
     * @return String
     */
    override fun toString(): String {
        return "$OPERATOR_NE_STR($value)"
    }

    companion object {
        private const val OPERATOR_NE_STR = "ne"
        private const val OPERATOR_NE = "!="

        /**
         * Creates operator instance
         * @param configuration FilterConfiguration
         * @return Filter
         */
        private fun create(configuration: FilterConfiguration): Filter {
            return NotEqualFilter(configuration.conditionValue!!)
        }

        /**
         * Registers operator as known instance
         */
        fun register() {
            FilterBuilder.registerOperator(OPERATOR_NE, OPERATOR_NE_STR) { c -> create(c) }
        }
    }
}