
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OrTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Or.create(1).validate()
        }
    }

    @Test
    fun `deserialize from json`() {

        val jsonLong = """
            {
                "Operator": "Or", 
                "Operand" : 1
            }"""

        val jsonShort = """
            {
                "Operator": "|", 
                "Operand" : 1
            }"""

        for (json in listOf(jsonLong, jsonShort)) {
            assertEquals(Or(1), Or.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
        }
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Triple<Number?, Number, Number?>>(
            // target, and mask, result
            Triple(0, 1, 1),
            Triple(0.toByte(), 1.toByte(), 1.toByte()),
            Triple(0.toShort(), 1.toShort(), 1.toShort()),
            Triple(0.toLong(), 1.toLong(), 1.toLong()),
            Triple(1, 0, 1),
            Triple(1.toByte(), 0.toByte(), 1.toByte()),
            Triple(1.toShort(), 0.toShort(), 1.toShort()),
            Triple(1.toLong(), 0.toLong(), 1.toLong()),
            Triple(1.0, 1.0, 1.0),
            Triple(null, 1, null)
        )

        for (test in testValues) {
            val operand = Or.create(test.second)
            val result = operand.apply(test.first)
            assertEquals(test.third, result, if (test.first != null) test.first!!::class.java.name else "null")
            if (result != null && test.first != null) {
                assertEquals(test.first!!::class, result::class)
            }
        }
    }
}