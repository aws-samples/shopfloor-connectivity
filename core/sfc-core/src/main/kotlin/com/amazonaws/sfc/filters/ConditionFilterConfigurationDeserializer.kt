// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.filters

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class ConditionFilterConfigurationDeserializer : JsonDeserializer<ConditionFilterConfiguration> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ConditionFilterConfiguration? {
        val d = FilterConfigurationDeserializer()
        return ConditionFilterConfiguration.from(json?.asJsonObject?.let { d.filterOperatorConfigurationFromJsonObject(it) })
    }
}