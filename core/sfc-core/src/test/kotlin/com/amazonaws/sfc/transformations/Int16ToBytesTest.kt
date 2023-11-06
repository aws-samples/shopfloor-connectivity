
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Int16ToBytesTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Fahrenheit.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
            "Operator": "Int16ToBytes"
            }"""
        assertEquals(Int16ToBytes.create(), Int16ToBytes.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {
        val o = Int16ToBytes()
        val result = o.invoke(0x0ff0)
        assertEquals(listOf(0x0f.toByte(), 0xf0.toByte()), result, "Split 16 bit int into bytes")
    }
}