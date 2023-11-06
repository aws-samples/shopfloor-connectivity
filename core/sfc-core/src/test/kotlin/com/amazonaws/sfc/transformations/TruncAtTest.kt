
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TruncAtTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            TruncAt.create(1).validate()
        }
    }

    @Test
    fun `deserialization from json`() {
        val json = """
            {
                "Operator": "TruncAt", 
                "Operand" : 1
            }"""

        assertEquals(TruncAt.create(1), TruncAt.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Triple<Number, Int, Number?>>(
            // target, truncAt result
            Triple(12, 1, 12),
            Triple(12, 0, 12),
            Triple(12, -1, 10),
            Triple(12, -2, 0),
            Triple(12.toByte(), 1, 12.toByte()),
            Triple(12.toByte(), 0, 12.toByte()),
            Triple(12.toByte(), -1, 10.toByte()),
            Triple(12.toShort(), 1, 12.toShort()),
            Triple(12.toShort(), 0, 12.toShort()),
            Triple(12.toShort(), -1, 10.toShort()),
            Triple(12.toLong(), 1, 12.toLong()),
            Triple(12.toLong(), 0, 12.toLong()),
            Triple(12.toLong(), -1, 10.toLong()),
            Triple(12.123, 0, 12.0),
            Triple(12.123, 1, 12.1),
            Triple(12.123, 2, 12.12),
            Triple(12.123, 3, 12.123),
            Triple(12.123, 4, 12.123),
            Triple(12.123, -1, 10.0),
            Triple(12.123.toFloat(), 0, 12.0.toFloat()),
            Triple(12.123.toFloat(), 1, 12.1.toFloat()),
            Triple(12.123.toFloat(), 2, 12.12.toFloat()),
            Triple(12.123.toFloat(), 3, 12.123.toFloat()),
            Triple(12.123.toFloat(), 4, 12.123.toFloat()),
            Triple(12.123.toFloat(), -1, 10.0.toFloat())
        )

        for (v in testValues) {
            val result = TruncAt.create(v.second).invoke(v.first)
            assertEquals(v.third, result, "Test trunc at ${v.second} ${v.first::class}")
            if (result != null) {
                assertEquals(v.first::class.java, result::class.java, "Check type")
            }

        }
    }
}