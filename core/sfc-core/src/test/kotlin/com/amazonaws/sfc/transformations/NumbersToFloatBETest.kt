// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NumbersToFloatBETest {

    private val o = NumbersToFloatBE.create()

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            NumbersToFloatBE.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "NumbersToFloatBE"
            }"""
        assertEquals(NumbersToFloatBE.create(), NumbersToFloatBE.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {

        val testValues = listOf<List<Any>>(
            listOf(0x42F6, 0xE979),
            listOf("17142", "59769"),
            listOf("0x42F6", "0xE979"),
            listOf(0x42F6.toShort(), 0xE979.toShort()),
            listOf(0x42F6.toUShort(), 0xE979.toUShort()),
            listOf(0x42F6.toUShort(), 0xE979.toUShort()),
            listOf(0x42F6.toUInt(), 0xE979.toUInt()),
            listOf(0x42F6.toLong(), 0xE979.toLong()),
            listOf(0x42F6.toULong(), 0xE979.toULong())
        )

        for (target in testValues) {
            val result = o.invoke(target)
            assertEquals(123.456f, result, target.first()::class.java.name)
            if (result != null) {
                assertTrue(result::class == Float::class, "Test for Float Type")
            }
        }
    }

    @Test
    fun `invalid numeric input`() {
        val result1 = o.invoke(listOf(0xFF.toByte(), 0xFF.toByte()))
        assertEquals(null, result1, "Bad numeric input")
    }

    @Test
    fun `invalid type input`() {
        val result1 = o.invoke(listOf("AA", "BB"))
        assertEquals(null, result1, "Bad input type")
    }

    @Test
    fun `invalid size input`() {
        val result1 = o.invoke(emptyList<Short>())
        assertEquals(null, result1, "Bad size input")
    }
}


