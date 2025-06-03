// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.awss3tables.config

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class FieldConfigurationDeserializer(): JsonDeserializer<FieldConfiguration> {

        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): FieldConfiguration? {

            return if (json == null || json.isJsonNull || !json.isJsonObject)
                null
            else
                FieldConfiguration.fromJson(json.asJsonObject)
        }
    }