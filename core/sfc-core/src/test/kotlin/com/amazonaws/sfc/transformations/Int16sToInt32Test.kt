
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Int16sToInt32Test {

    private val o = Int16sToInt32.create()

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Int16sToInt32.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
            "Operator": "Int16sToInt32"
            }"""
        assertEquals(Int16sToInt32.create(), Int16sToInt32.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {
        val result2 = o.invoke(listOf(0xf0f0.toShort(), 0xf0f0.toShort()))
        assertEquals(result2, 0xf0f0f0f0.toInt(), "Two int16s")
        if (result2 != null) {
            assertEquals(result2::class, Int::class, "Two Int16 type")
        }
    }

    @Test
    fun `invalid input`() {
        val result1 = o.invoke(listOf(0x0000))
        assertEquals(null, result1, "Single int16")
    }
}


