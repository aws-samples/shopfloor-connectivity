package com.amazonaws.sfc.filters

typealias CreateFilterFunction = (c: FilterConfiguration) -> Filter

/**
 * Build instances of known filter operators from Filter configuration data
 */
object FilterBuilder {

    private var knownFilterOperators = mutableMapOf<String, CreateFilterFunction>()

    init {
        registerFilters()
    }

    // Mapping of known operators, indexed by the function name and alternative name. The
    // stored value is the method to call to create the instance of the operator.
    //   private val knownFilterOperators = mutableMapOf<String, CreateFilterFunction>()

    /**
     * Registers a known operator
     * @param operatorName String Name of the operator, e.g. eq, ne
     * @param operatorAltName String? Alternative name eg. ==, !=
     * @param fn Function1<[@kotlin.ParameterName] FilterConfiguration, Filter>
     */
    fun registerOperator(operatorName: String, operatorAltName: String? = null, fn: CreateFilterFunction) {
        if (operatorName in knownFilterOperators && knownFilterOperators[operatorName] != fn) {
            throw IllegalArgumentException("Operator with filter name \"$operatorName\" is already in use")
        }
        knownFilterOperators[operatorName] = fn

        if (operatorAltName != null) {
            if (operatorAltName in knownFilterOperators && knownFilterOperators[operatorAltName] != fn) {
                throw IllegalArgumentException("Operator with alt filter name \"$operatorName\" is already in use")
            }
            knownFilterOperators[operatorAltName] = fn
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
        val op = knownFilterOperators[configuration.operator]
        return if (op != null) op(configuration) else null
    }

}

/**
 * Registers all know filters to filter-builder
 */
fun registerFilters() {
    AndFilter.register()
    OrFilter.register()
    EqualFilter.register()
    NotEqualFilter.register()
    GreaterFilter.register()
    GreaterOrEqualFilter.register()
    LessFilter.register()
    LessOrEqualFilter.register()
}