// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.filters

import com.amazonaws.sfc.data.JmesPathExtended
import io.burt.jmespath.Expression

typealias CreateFilterFunction = (f: FilterBuilder, c: FilterConfiguration) -> Filter?

abstract class FilterBuilder {

    abstract fun registerFilters()

    // Mapping of known operators, indexed by the function name and alternative name. The
    // stored value is the method to call to create the instance of the operator.
    //   private val knownFilterOperators = mutableMapOf<String, CreateFilterFunction>()
    private var _knownFilterOperators = mutableMapOf<String, CreateFilterFunction>()

    val filterOperators: Set<String>
        get() = _knownFilterOperators.keys

    /**
     * Registers a known filter
     * @param filterName String Name of the filter, e.g. eq, ne
     * @param filterAltName String? Alternative name eg. ==, !=
     * @param fn Function1<[@kotlin.ParameterName] FilterConfiguration, Filter>
     */
    fun registerFilter(filterName: String, filterAltName: String? = null, fn: CreateFilterFunction) {
        if (filterName in _knownFilterOperators && _knownFilterOperators[filterName] != fn) {
            throw IllegalArgumentException("Filter with filter name \"$filterName\" is already in use")
        }
        _knownFilterOperators[filterName] = fn

        if (filterAltName != null) {
            if (filterAltName in _knownFilterOperators && _knownFilterOperators[filterAltName] != fn) {
                throw IllegalArgumentException("Filter with alt filter name \"$filterName\" is already in use")
            }
            _knownFilterOperators[filterAltName] = fn
        }
    }

    /**
     * Registers a known operator
     * @param operatorName String Name of the operator, e.g. eq, ne
     * @param operatorAltName String? Alternative name eg. ==, !=
     * @param fn Function1<[@kotlin.ParameterName] FilterConfiguration, Filter>
     */
    fun registerOperator(operatorName: String, operatorAltName: String? = null, fn: CreateFilterFunction) {
        if (operatorName in _knownFilterOperators && _knownFilterOperators[operatorName] != fn) {
            throw IllegalArgumentException("Operator with filter name \"$operatorName\" is already in use")
        }
        _knownFilterOperators[operatorName] = fn

        if (operatorAltName != null) {
            if (operatorAltName in _knownFilterOperators && _knownFilterOperators[operatorAltName] != fn) {
                throw IllegalArgumentException("Operator with alt filter name \"$operatorName\" is already in use")
            }
            _knownFilterOperators[operatorAltName] = fn
        }
    }

    /**
     * Builds list of filer operators for operator that have nested filters (AND,OR)
     * @param v Any
     * @return List<Filter>
     */
    internal fun buildFilterList(v: Any): List<Filter> =
        when (v) {

            // Handle slightly incorrect configurations where an AND or OR filters only have a single nested condition
            is FilterConfiguration -> {
                listOfNotNull(build(v))
            }

            // Build a list of filters
            is Iterable<*> -> v.mapNotNull {
                if (it is FilterConfiguration)
                    build(it)
                else
                    null
            }.toList()

            // No nested filters
            else -> emptyList()
        }

    /**
     * Build a filter operator from configuration data. The operator in the configuration must
     * be a known registered operator, if this nis not the case the method returns null
     * @param configuration FilterConfiguration Filter configuration
     * @return Filter? Instance of the filter
     */
    fun build(configuration: FilterConfiguration): Filter? {
        val createFilterFunction = _knownFilterOperators[configuration.operator]
        return if (createFilterFunction != null) createFilterFunction(this, configuration) else null
    }

    fun buildExpressions(configuration: FilterConfiguration): List<Expression<Any>> {
        val expressions: List<Expression<Any>> = when (configuration.conditionValue) {
            is String -> listOf(jmesPath.compile(configuration.conditionValue.toString()))
            is Iterable<*> -> (configuration.conditionValue as Iterable<*>).map {
                jmesPath.compile(it.toString())
            }

            else -> emptyList()
        }
        return expressions
    }

    companion object {

        private val jmesPath = JmesPathExtended.create()


    }

}