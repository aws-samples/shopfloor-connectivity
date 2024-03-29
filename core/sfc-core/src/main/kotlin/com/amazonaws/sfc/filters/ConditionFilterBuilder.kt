// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.filters

/**
 * Build instances of known filter operators from Filter configuration data
 */
object ConditionFilterBuilder : FilterBuilder() {

    init {
        registerFilters()
    }

    /**
     * Registers all know filters to filter-builder
     */
    override fun registerFilters() {
        AndFilter.register(this)
        OrFilter.register(this)
        AnyConditionFilter.register(this)
        AllConditionFilter.register(this)
        NoneConditionFilter.register(this)
        PresentConditionFilter.register(this)
        AbsentConditionFilter.register(this)
        OnlyValueConditionFilter.register(this)
        NotOnlyValueConditionFilter.register(this)

    }

}

