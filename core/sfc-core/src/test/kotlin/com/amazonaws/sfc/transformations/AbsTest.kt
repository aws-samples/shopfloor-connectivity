
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AbsTest {

    @Test
    fun `create and validation`() {
        Assertions.assertDoesNotThrow {
            Abs.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "Abs"
            }"""
        assertEquals(Abs.create(), Abs.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic an return types`() {
        val testValues = listOf<Pair<Number?, Number?>>(
            Pair(1, 1),
            Pair(1.toByte(), 1.toByte()),
            Pair(1.toShort(), 1.toShort()),
            Pair(1.toLong(), 1.toLong()),
            Pair(1.0f, 1.0f),
            Pair(1.0, 1.0),
            Pair(-1, 1),
            Pair((-1).toByte(), 1.toByte()),
            Pair((-1).toShort(), 1.toShort()),
            Pair((-1).toLong(), 1.toLong()),
            Pair(-1.0f, 1.0f),
            Pair(null, null)
        )

        val o = Abs.create()
        for (v in testValues) {
            val target: Number? = v.first
            val result = o.apply(target)
            assertEquals(v.second, result, if (v.first != null) v.first!!::class.java.name else "null")
            if (result != null && target != null) assertEquals(target::class, result::class)
        }
    }
}