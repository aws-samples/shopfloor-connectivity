// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.filters

/**
 * Implements NE filter operator
 * @property value Any Value to test against (can be String boolean or a number)
 * @constructor
 */
class NotEqualValueFilter(private val value: Any) : Filter {

    // used EQ operator to implement NE
    private val eq = EqualValueFilter(value)

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
            return NotEqualValueFilter(configuration.conditionValue!!)
        }

        /**
         * Registers operator as known instance
         */
        fun register(filterBuilder: FilterBuilder) {
            filterBuilder.registerOperator(OPERATOR_NE, OPERATOR_NE_STR) { _: FilterBuilder, c -> create(c) }
        }
    }
}