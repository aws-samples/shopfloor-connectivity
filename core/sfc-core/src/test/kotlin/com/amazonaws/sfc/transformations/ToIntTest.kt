
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ToIntTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            ToInt.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {

        val json = """
            {
                "Operator": "ToInt"
            }"""
        assertEquals(ToInt.create(), ToInt.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Number>(1, 1.toByte(), 1.toShort(), 1.toShort(), 1.toLong(), 1.toLong(), 1.0f, 1.0)


        val o = ToInt.create()
        for (v in testValues) {
            val target: Number = v
            val result = o.invoke(target)
            assertEquals(result, v.toInt(), v::class.java.name)
            if (result != null) assertEquals(Int::class, result::class)
        }
    }
}