
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ShrTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Shr.create(1).validate()
        }
    }

    @Test
    fun `deserialize from json`() {

        val jsonLong = """
            {
                "Operator": "Shr", 
                "Operand" : 1
            }"""

        val jsonShort = """
            {
                "Operator": ">>", 
                "Operand" : 1
            }"""

        for (json in listOf(jsonLong, jsonShort)) {
            assertEquals(Shr(1), Shr.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
        }
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Triple<Number, Int, Number?>>(
            // target, shl, result
            Triple(2, 1, 1),
            Triple(2.toByte(), 1, 1.toByte()),
            Triple(2.toShort(), 1, 1.toShort()),
            Triple(2.toLong(), 1, 1.toLong()),
            Triple(2.0, 1, null)
        )
        for (v in testValues) {
            val o = Shr.create(v.second)
            val result = o.invoke(v.first)
            assertEquals(v.third, result, v.first::class.java.name)
            if (result != null) {
                assertEquals(v.first::class, result::class)
            }
        }
    }
}