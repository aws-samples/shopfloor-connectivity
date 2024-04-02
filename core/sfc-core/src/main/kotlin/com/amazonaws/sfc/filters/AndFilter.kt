package com.amazonaws.sfc.filters


/**
 * Implements AND / && data filter
 * @property conditions List<Filter> List of conditions
 */
class AndFilter private constructor(private val filters: List<Filter>) : Filter {

    /**
     * Returns true is all conditions are true
     * @param value Any
     * @return Boolean
     */
    override fun apply(value: Any): Boolean {
        return filters.find { !it.apply(value) } == null
    }

    /**
     * String representation of operator
     * @return String
     */
    override fun toString(): String = filters.joinToString(separator = " $OPERATOR_AND ")


    companion object {

        private const val OPERATOR_AND_STR = "and"
        private const val OPERATOR_AND = "&&"

        /**
         * Creates AND operator
         * @param configuration FilterConfiguration
         * @return Filter
         */
        private fun create(filterBuilder: FilterBuilder, configuration: FilterConfiguration): Filter {
            val conditions = filterBuilder.buildFilterList(configuration.conditionValue!!)
            return AndFilter(conditions)
        }

        /**
         * Registers AND operator as known operator to FilterOperatorFactory
         */
        fun register(filterBuilder: FilterBuilder) {
            filterBuilder.registerOperator(OPERATOR_AND, OPERATOR_AND_STR) { f: FilterBuilder, c: FilterConfiguration -> create(f, c) }
        }

    }

}