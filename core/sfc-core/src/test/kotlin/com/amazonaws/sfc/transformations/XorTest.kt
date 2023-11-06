
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class XorTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Xor.create(1).validate()
        }
    }

    @Test
    fun `deserialize from json`() {

        val jsonLong = """
            {
                "Operator": "Xor", 
                "Operand" : 1
            }"""

        val jsonShort = """
            {
                "Operator": "^", 
                "Operand" : 1
            }"""

        for (json in listOf(jsonLong, jsonShort)) {
            assertEquals(Xor.create(1), Xor.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
        }
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Triple<Number, Number, Number?>>(
            // target, xor mask, result
            Triple(1, 1, 0),
            Triple(1.toByte(), 1.toByte(), 0.toByte()),
            Triple(1.toShort(), 1.toShort(), 0.toShort()),
            Triple(1.toLong(), 1.toLong(), 0.toLong()),
            Triple(1, 0, 1),
            Triple(1.toByte(), 0.toByte(), 1.toByte()),
            Triple(1.toShort(), 0.toShort(), 1.toShort()),
            Triple(1.toLong(), 0.toLong(), 1.toLong()),
            Triple(1.0, 1.0, null)
        )
        for (v in testValues) {
            val o = Xor.create(v.second)
            val result = o.invoke(v.first)
            assertEquals(v.third, result, v.first::class.java.name)
            if (result != null) {
                assertEquals(v.first::class, result::class)
            }
        }
    }
}