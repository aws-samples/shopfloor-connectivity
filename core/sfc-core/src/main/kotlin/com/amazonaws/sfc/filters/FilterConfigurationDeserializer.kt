package com.amazonaws.sfc.filters

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

/**
 * Custom deserializer for FilterConfiguration
 */
class FilterConfigurationDeserializer : JsonDeserializer<FilterConfiguration> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): FilterConfiguration? {
        return json?.asJsonObject?.let { filterOperatorConfigurationFromJsonObject(it) }
    }

    private fun filterOperatorConfigurationFromJsonObject(o: JsonObject): FilterConfiguration? {

        // Get the operator
        val operator = o.getAsJsonPrimitive(FilterConfiguration.CONFIG_FILTER_OPERATOR).asString
                       ?: throw IllegalStateException("FilterConfiguration operator name ${FilterConfiguration.CONFIG_FILTER_OPERATOR} can not be null")

        // Get the value the operator is using for it's logic
        val operatorValue =
            o.get(FilterConfiguration.CONFIG_FILTER_VALUE)
            ?: throw IllegalStateException("FilterConfiguration ${FilterConfiguration.CONFIG_FILTER_VALUE} can not be null")

        // Build the Configuration instance
        val filter = FilterConfiguration.from(
            operator = operator,
            value = when {
                // operator that tests against a value
                operatorValue.isJsonPrimitive -> {
                    when {
                        operatorValue.asJsonPrimitive.isNumber -> operatorValue.asDouble
                        operatorValue.asJsonPrimitive.isBoolean -> operatorValue.asBoolean
                        else -> operatorValue.asJsonPrimitive.asString
                    }
                }
                // operator has nested filters (eg. AND, OR)
                operatorValue.isJsonArray -> {
                    operatorValue.asJsonArray.mapNotNull {
                        // Build a nested filter for all config items in the value
                        val valueJsonObjectArray = it as? JsonObject?
                        if (valueJsonObjectArray != null) {
                            filterOperatorConfigurationFromJsonObject(valueJsonObjectArray)
                        } else null
                    }
                }
                // handle nested single filters
                operatorValue.isJsonObject -> {
                    listOfNotNull(filterOperatorConfigurationFromJsonObject(operatorValue.asJsonObject))
                }

                else -> {
                }
            }
        )
        return if (filter.conditionValue != null) filter else null
    }
}