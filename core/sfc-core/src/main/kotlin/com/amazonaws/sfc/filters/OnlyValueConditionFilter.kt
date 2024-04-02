// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.filters


class OnlyValueConditionFilter(private val b: Boolean) : Filter {


    override fun apply(value: Any): Boolean {
        return ((value as Map<*, *>).size == 1) == b
    }

    override fun toString(): String {
        return "$OPERATOR_ONLY_STR == $b"
    }


    companion object {
        private const val OPERATOR_ONLY = "^"
        private const val OPERATOR_ONLY_STR = "only"


        private fun create(configuration: FilterConfiguration): Filter {

            val b: Boolean = when (configuration.conditionValue) {
                is Boolean -> configuration.conditionValue as Boolean
                is String -> (configuration.conditionValue as String).toBoolean()
                is Number -> (configuration.conditionValue as Number).toInt() == 1
                else -> throw IllegalArgumentException("Condition value must be a boolean")
            }

            return OnlyValueConditionFilter(b)
        }

        /**
         * Registers operator as known type
         */
        fun register(filterBuilder: FilterBuilder) {
            filterBuilder.registerOperator(OPERATOR_ONLY, OPERATOR_ONLY_STR) { _: FilterBuilder, c -> create(c) }
        }
    }
}



