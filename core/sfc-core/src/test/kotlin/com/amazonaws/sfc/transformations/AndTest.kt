
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AndTest {
    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            And.create(1).validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val jsonFull = """
            {
                "Operator": "And",
                "Operand": 1
            }"""

        val jsonShort = """
            {
                "Operator": "&",
                "Operand": 0x0F
            }"""

        for (json in listOf(jsonFull, jsonShort)) {
            assertEquals(And.create(1), And.fromJson(Gson().fromJson(jsonFull, JsonObject::class.java)), "from $json")
        }
    }

    @Test
    fun `operator logic and return types`() {
        val testValues = listOf<Triple<Number?, Number, Number?>>(
            // target, and mask, result
            Triple(1, 1, 1),
            Triple(1.toByte(), 1.toByte(), 1.toByte()),
            Triple(1.toShort(), 1.toShort(), 1.toShort()),
            Triple(1.toLong(), 1.toLong(), 1.toLong()),
            Triple(1, 0, 0),
            Triple(1.toByte(), 0.toByte(), 0.toByte()),
            Triple(1.toShort(), 0.toShort(), 0.toShort()),
            Triple(1.toLong(), 0.toLong(), 0.toLong()),
            Triple(1.0, 1.0, 1.0),
            Triple(null, 1, null)
        )

        for (test in testValues) {
            val operand = And.create(test.second)
            val result = operand.apply(test.first)
            assertEquals(test.third, result, if (test.first != null) test.first!!::class.java.name else "null")
            if (result != null && test.first != null) {
                assertEquals(test.first!!::class, result::class)
            }
        }
    }
}