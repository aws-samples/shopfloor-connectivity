// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.filters

import com.amazonaws.sfc.config.ConfigurationClass

/**
 * Data filter operator configuration
 * NOTE this configuration is using a custom deserializer to handle nested filter configurations
 * @see  ValueFilterConfigurationDeserializer
 */
@ConfigurationClass
class ValueFilterConfiguration : FilterConfiguration() {

    companion object {

        private val default = ValueFilterConfiguration()

        fun from(filterConfiguration: FilterConfiguration?): ValueFilterConfiguration? {
            if (filterConfiguration == null) return null
            val c = ValueFilterConfiguration()
            c._conditionValue = filterConfiguration.conditionValue
            c._operator = filterConfiguration.operator
            return c
        }

        fun create(
            operator: String = default._operator,
            value: Any? = default._conditionValue
        ): ValueFilterConfiguration {

            val instance = ValueFilterConfiguration()
            with(instance) {
                _operator = operator
                _conditionValue = value
            }
            return instance
        }
    }


}