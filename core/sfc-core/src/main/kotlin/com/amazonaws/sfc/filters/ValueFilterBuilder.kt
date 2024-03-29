// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.filters

/**
 * Build instances of known filter operators from Filter configuration data
 */
object ValueFilterBuilder : FilterBuilder() {

    init {
        registerFilters()
    }

    /**
     * Registers all know filters to filter-builder
     */
    override fun registerFilters() {
        AndFilter.register(this)
        OrFilter.register(this)
        EqualValueFilter.register(this)
        NotEqualValueFilter.register(this)
        ValueGreaterFilter.register(this)
        GreaterOrEqualValueFilter.register(this)
        LessValueFilter.register(this)
        LessOrEqualValueFilter.register(this)
    }

}

