// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.filters

import io.burt.jmespath.Expression


class NoneConditionFilter(private val expressions: List<Expression<Any>>, private val conditions: List<String>) : Filter {


    override fun apply(value: Any): Boolean {
        return expressions.none { expr -> expr.search(value as Map<*, *>) != null }
    }

    override fun toString(): String {
        return "$OPERATOR_NONE_STR of [${conditions.joinToString()}]"
    }

    companion object {
        private const val OPERATOR_NONE = "!!"
        private const val OPERATOR_NONE_STR = "none"

        private fun create(filterBuilder: FilterBuilder, configuration: FilterConfiguration): Filter? {
            val expressions: List<Expression<Any>> = filterBuilder.buildExpressions(configuration)
            val c =
                if (configuration.conditionValue is Iterable<*>) (configuration.conditionValue as Iterable<*>).map { it.toString() } else listOf(configuration.conditionValue.toString())
            return if (expressions.isNotEmpty()) NoneConditionFilter(expressions, c) else null
        }

        /**
         * Registers operator as known type
         */
        fun register(filterBuilder: FilterBuilder) {
            filterBuilder.registerOperator(OPERATOR_NONE, OPERATOR_NONE_STR) { f: FilterBuilder, c -> create(f, c) }
        }
    }
}



