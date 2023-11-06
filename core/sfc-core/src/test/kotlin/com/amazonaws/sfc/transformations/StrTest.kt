
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StrTest {

    @Test
    fun `create and Validate`() {
        Assertions.assertDoesNotThrow {
            Str.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "Str"
            }"""
        assertEquals(Str.create(), Str.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Number>(
            1, 1.toByte(),
            1.toShort(),
            1.toLong(),
            1.0,
            1.0.toFloat())

        val o = Str.create()
        for (v in testValues) {
            val s = o.invoke(v) as String?
            Assertions.assertNotNull(s, "${v::class.java.name} not null")
            if (s != null) Assertions.assertTrue(s.isNotEmpty(), "Not Empty ${v::class.java.name}")
        }
    }
}