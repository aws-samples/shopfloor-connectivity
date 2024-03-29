// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.filters

import com.amazonaws.sfc.config.ConfigurationClass

/**
 * Data filter operator configuration
 * NOTE this configuration is using a custom deserializer to handle nested filter configurations
 * @see  ConditionFilterConfigurationDeserializer
 */
@ConfigurationClass
class ConditionFilterConfiguration : FilterConfiguration() {

    companion object {

        fun from(filterConfiguration: FilterConfiguration?): ConditionFilterConfiguration? {
            if (filterConfiguration == null) return null
            val c = ConditionFilterConfiguration()
            c._conditionValue = filterConfiguration.conditionValue
            c._operator = filterConfiguration.operator
            return c
        }

        private val default = ConditionFilterConfiguration()

        fun create(
            operator: String = default._operator,
            value: Any? = default._conditionValue
        ): ConditionFilterConfiguration {

            val instance = ConditionFilterConfiguration()
            with(instance) {
                _operator = operator
                _conditionValue = value
            }
            return instance
        }
    }


}