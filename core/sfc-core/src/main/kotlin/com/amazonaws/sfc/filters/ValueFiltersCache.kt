// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.filters

class ValueFiltersCache(filterConfigurations: Map<String, ValueFilterConfiguration>) {
    val filters = filterConfigurations.map {
        it.key to ValueFilterBuilder.build(it.value)
    }.filter { it.second != null }.toMap()

    operator fun get(key: String): Filter? = filters[key]

}