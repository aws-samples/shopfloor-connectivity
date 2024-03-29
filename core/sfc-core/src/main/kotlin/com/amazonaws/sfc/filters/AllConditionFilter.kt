// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.filters

import io.burt.jmespath.Expression


class AllConditionFilter(private val expressions: List<Expression<Any>>, private val conditions: List<String>) : Filter {


    override fun apply(value: Any): Boolean {
        return expressions.all { expr -> expr.search(value as Map<*, *>) != null }
    }

    override fun toString(): String {
        return "$OPERATOR_ALL_STR of [${conditions.joinToString()}]"
    }


    companion object {
        private const val OPERATOR_ALL = "##"
        private const val OPERATOR_ALL_STR = "all"


        private fun create(filterBuilder: FilterBuilder, configuration: FilterConfiguration): Filter? {
            val expressions: List<Expression<Any>> = filterBuilder.buildExpressions(configuration)
            val c =
                if (configuration.conditionValue is Iterable<*>) (configuration.conditionValue as Iterable<*>).map { it.toString() } else listOf(configuration.conditionValue.toString())
            return if (expressions.isNotEmpty()) AllConditionFilter(expressions, c) else null
        }

        /**
         * Registers operator as known type
         */
        fun register(filterBuilder: FilterBuilder) {
            filterBuilder.registerOperator(OPERATOR_ALL, OPERATOR_ALL_STR) { f: FilterBuilder, c -> create(f, c) }
        }
    }
}



