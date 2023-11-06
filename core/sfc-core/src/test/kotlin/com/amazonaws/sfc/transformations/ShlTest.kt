
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ShlTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Shl.create(1).validate()
        }
    }

    @Test
    fun `deserialization from json`() {
        val jsonLong = """
            {
                "Operator": "Shl", 
                "Operand" : 1
            }"""

        val jsonShort = """
            {
                "Operator": "<<", 
                "Operand" : 1
            }"""

        for (json in listOf(jsonLong, jsonShort)) {
            assertEquals(Shl(1), Shl.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
        }
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Triple<Number, Int, Number?>>(
            // target, shl, result
            Triple(1, 1, 2),
            Triple(1.toByte(), 1, 2.toByte()),
            Triple(1.toShort(), 1, 2.toShort()),
            Triple(1.toLong(), 1, 2.toLong()),
            Triple(1.0, 1, null)
        )
        for (v in testValues) {
            val o = Shl.create(v.second)
            val result = o.invoke(v.first)
            assertEquals(v.third, result, v.first::class.java.name)
            if (result != null) {
                assertEquals(v.first::class, result::class)
            }
        }
    }
}