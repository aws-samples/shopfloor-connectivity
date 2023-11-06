
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.filters

import com.amazonaws.sfc.config.ConfigurationClass

/**
 * Caches constructed value filters
 * @property filters Mapping<String, Filter?>
 * @constructor
 */
@ConfigurationClass
class ValueFiltersCache(filterConfigurations: Map<String, FilterConfiguration>) {
    val filters = filterConfigurations.map {
        it.key to FilterBuilder.build(it.value)
    }.filter { it.second != null }.toMap()

    operator fun get(key: String): Filter? = filters[key]

}