
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NotTest {


    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Not.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val jsonLong = """
            {
                "Operator": "Not"
            }"""

        val jsonShort = """
            {
                "Operator": "!"
            }"""

        for (json in listOf(jsonLong, jsonShort)) {
            assertEquals(Not.create(), Not.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
        }
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Pair<Boolean?, Boolean?>>(
            // target, mod value, result
            Pair(false, true),
            Pair(true, false),
            Pair(null, null)
        )

        for (v in testValues) {
            val result = Not().apply(v.first)
            assertEquals(v.second, result, "${if (v.first != null) v.first!!::class.simpleName else "null"} ! ${v.second}")
            if (result != null && v.first != null) {
                assertEquals(v.first!!::class.java, result::class.java, "Check type")
            }
        }
    }
}