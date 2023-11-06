
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MinusTest {


    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Minus.create(1).validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val jsonLong = """
            {
                "Operator": "Minus", 
                "Operand" : 1
            }"""

        val jsonShort = """
            {
                "Operator": "-", 
                "Operand" : 1
            }"""

        for (json in listOf(jsonLong, jsonShort)) {
            assertEquals(Minus(1), Minus.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
        }
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Triple<Number?, Number?, Number?>>(
            // target, minus value, result
            Triple(2, 1, 1),
            Triple(2.toByte(), 1.toByte(), 1.toByte()),
            Triple(2.toShort(), 1.toShort(), 1.toShort()),
            Triple(2.toLong(), 1.toLong(), 1.toLong()),
            Triple(2.0, 1.0, 1.0),
            Triple(2.0.toFloat(), 1.0.toFloat(), 1.0.toFloat()),
            Triple(2, null, null),
            Triple(Double.MAX_VALUE * -1.0, Double.MAX_VALUE, null),
            Triple(null, 0, null)

        )

        for (v in testValues) {
            val result = Minus(v.second).apply(v.first)
            assertEquals(v.third, result, "Test ${if (v.first != null) v.first!!::class.simpleName else "null"} - ${v.second}")
            if (result != null && v.first != null) {
                assertEquals(v.first!!::class.java, result::class.java, "Check type")
            }
        }
    }
}