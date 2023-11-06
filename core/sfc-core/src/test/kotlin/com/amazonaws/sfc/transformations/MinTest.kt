
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MinTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Min.create(1).validate()
        }
    }

    @Test
    fun `deserialize from json`() {

        val json = """
            {
                "Operator": "Min", 
                "Operand" : 1
            }"""

        assertEquals(Min(1), Min.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Triple<Number?, Number, Number?>>(
            // target, test value, result
            Triple(1, 2, 1),
            Triple(1.toByte(), 1.toByte(), 1.toByte()),
            Triple(1.toShort(), 2.toShort(), 1.toShort()),
            Triple(1.toLong(), 2.toLong(), 1.toLong()),
            Triple(1.0, 2.0, 1.0),
            Triple(1.0.toFloat(), 2.0.toFloat(), 1.0.toFloat()),
            Triple(null, 1, null)
        )

        for (v in testValues) {
            val result = Min.create(v.second).apply(v.first)
            assertEquals(v.third, result, "Test min of ${if (v.first != null) v.first!!::class.simpleName else "null"} with value ${v.first} and ${v.second}")
            if (result != null && v.first != null) {
                assertEquals(v.first!!::class.java, result::class.java, "Check type")
            }
        }
    }
}