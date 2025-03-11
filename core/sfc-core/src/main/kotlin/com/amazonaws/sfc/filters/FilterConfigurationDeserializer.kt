package com.amazonaws.sfc.filters

import com.amazonaws.sfc.filters.FilterConfiguration.Companion.CONFIG_FILTER_OPERATOR
import com.amazonaws.sfc.filters.FilterConfiguration.Companion.CONFIG_FILTER_VALUE
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.lang.reflect.Type

/**
 * Custom deserializer for FilterConfiguration
 */
open class FilterConfigurationDeserializer : JsonDeserializer<FilterConfiguration> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): FilterConfiguration? {
        return json?.asJsonObject?.let { filterOperatorConfigurationFromJsonObject(it) }
    }

    internal fun filterOperatorConfigurationFromJsonObject(o: JsonObject): FilterConfiguration? {

        // Get the operator
        val operator = o.getAsJsonPrimitive(CONFIG_FILTER_OPERATOR).asString
            ?: throw IllegalStateException("FilterConfiguration operator name $CONFIG_FILTER_OPERATOR can not be null")

        // Get the value the operator is using for it's logic
        val operatorValue =
            o.get(CONFIG_FILTER_VALUE)
                ?: throw IllegalStateException("FilterConfiguration $CONFIG_FILTER_VALUE can not be null")

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

                operatorValue.isJsonArray -> {

                    operatorValue.asJsonArray.mapNotNull {

                        when {
                            // operator is a list of strings
                            it.isJsonPrimitive && it.asJsonPrimitive.isString -> it.asJsonPrimitive.asString
                            // operator has nested filters (e.g. AND, OR)
                            it as? JsonObject? != null -> filterOperatorConfigurationFromJsonObject(it)
                            else -> null
                        }
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

