
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test


class StrEqualsTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            StrEquals.create("").validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "StrEquals",
                "Operand" : "ABC"
            }"""
        assertEquals(StrEquals.create("ABC"), StrEquals.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {

        val testValues = listOf<Triple<String?, String, Boolean>>(
            Triple("A", "A", true),
            Triple("A", "a", false),
            Triple("A", "B", false),
            Triple("", "", true)
        )

        for (v in testValues) {
            val strEquals = StrEquals(v.second)
            assertEquals(v.third, strEquals.apply(v.first))
        }
    }

    @Test
    fun `handling invalid array operand`() {
        val json = """
            {
                "Operator": "StrEquals",
                "Operand" : ["ABC", "DEF"]
            }"""

        assertThrows(TransformationException::class.java) {
            StrEquals.fromJson(Gson().fromJson(json, JsonObject::class.java))
        }
    }

    @Test
    fun `handling invalid structure operand`() {
        val json = """
            {
                "Operator": "StrEquals",
                "Operand" : { "A" : "B"}
            }"""

        assertThrows(TransformationException::class.java) {
            StrEquals.fromJson(Gson().fromJson(json, JsonObject::class.java))
        }
    }
}