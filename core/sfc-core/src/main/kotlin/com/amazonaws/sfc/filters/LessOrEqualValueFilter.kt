// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.filters

import com.amazonaws.sfc.data.DataTypes.numericCompare


/**
 * Implements LE (<=) operator
 * @property value Any Tested value
 */
class LessOrEqualValueFilter(private val value: Any) : Filter {

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
            return LessOrEqualValueFilter(configuration.conditionValue!!)
        }

        /**
         * Registers operator as known type
         */
        fun register(filterBuilder: FilterBuilder) {
            filterBuilder.registerOperator(OPERATOR_LE, OPERATOR_LE_STR) { _: FilterBuilder, c -> create(c) }
        }
    }

}