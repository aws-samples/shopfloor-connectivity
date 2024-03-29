// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.filters

import io.burt.jmespath.Expression


class PresentConditionFilter(private val expression: Expression<Any>, private val condition: String) : Filter {


    override fun apply(value: Any): Boolean {
        return expression.search(value as Map<*, *>) != null
    }

    override fun toString(): String {
        return "$OPERATOR_PRESENT_STR  $condition"
    }


    companion object {
        private const val OPERATOR_PRESENT = "#"
        private const val OPERATOR_PRESENT_STR = "present"


        private fun create(filterBuilder: FilterBuilder, configuration: FilterConfiguration): Filter? {

            val expressions: List<Expression<Any>> = filterBuilder.buildExpressions(configuration)
            val c = if (configuration.conditionValue is Iterable<*>) (configuration.conditionValue as Iterable<*>).first()
                .toString() else configuration.conditionValue.toString()
            return if (expressions.isNotEmpty()) PresentConditionFilter(expressions.first(), c) else null
        }

        /**
         * Registers operator as known type
         */
        fun register(filterBuilder: FilterBuilder) {
            filterBuilder.registerOperator(OPERATOR_PRESENT, OPERATOR_PRESENT_STR) { f: FilterBuilder, c -> create(f, c) }
        }
    }
}



