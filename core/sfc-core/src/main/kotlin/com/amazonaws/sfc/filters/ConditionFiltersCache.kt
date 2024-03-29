// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.filters

import com.amazonaws.sfc.log.Logger


class ConditionFiltersCache(filterConfigurations: Map<String, ConditionFilterConfiguration>, logger: Logger) {
    private val className = this.javaClass.name

    val filters = filterConfigurations.map {
        val log = logger.getCtxLoggers(className, "filters")
        try {
            it.key to ConditionFilterBuilder.build(it.value)
        } catch (e: Exception) {
            log.error("Error building filter ${it.key} from ${it.value}, $e")
            it.key to null
        }
    }.filter { it.second != null }.toMap()

    operator fun get(key: String): Filter? = filters[key]

}