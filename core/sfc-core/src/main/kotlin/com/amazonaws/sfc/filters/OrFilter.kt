// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.filters

/**
 * Implements OR / || data filter
 * @property conditions List<Filter> List of conditions
 */
class OrFilter(
    private val conditions: List<Filter>
) : Filter {

    /**
     * Returns true is any conditions is true
     * @param value Any
     * @return Boolean
     */
    override fun apply(value: Any): Boolean {
        return conditions.isEmpty() || (conditions.find { it.apply(value) } != null)

    }

    /**
     * String representation of operator
     * @return String
     */
    override fun toString(): String = "(${conditions.joinToString(separator = " $OPERATOR_OR ")})"

    companion object {

        private const val OPERATOR_OR_STR = "or"
        private const val OPERATOR_OR = "||"

        /**
         * Creates OR operator
         * @param configuration FilterConfiguration
         * @return Filter
         */
        private fun create(filterBuilder: FilterBuilder, configuration: FilterConfiguration): Filter {
            val conditions = filterBuilder.buildFilterList(configuration.conditionValue!!)
            return OrFilter(conditions)
        }

        /**
         * Registers OR operator as known operator to FilterOperatorFactory
         */
        fun register(filterBuilder: FilterBuilder) {
            filterBuilder.registerOperator(OPERATOR_OR, OPERATOR_OR_STR) { f: FilterBuilder, c -> create(f, c) }
        }

    }
}