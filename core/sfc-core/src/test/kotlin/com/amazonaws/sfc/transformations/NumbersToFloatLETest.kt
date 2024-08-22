// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.util.asHexString
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NumbersToFloatLETest {

    private val o = NumbersToFloatLE.create()

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            NumbersToFloatLE.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "NumbersToFloatLE"
            }"""
        assertEquals(NumbersToFloatLE.create(), NumbersToFloatLE.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {

        val bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(0, 123.456f).array()
        println(bytes.asHexString())
        val testValues = listOf<List<Any>>(
             listOf(0x79E9, 0xF642),
            listOf("31209", "63042"),
            listOf( "0x79E9", "0xF642"),
            listOf(0x79E9.toShort(), 0xF642.toShort()),
            listOf(0x79E9.toUShort(), 0xF642.toUShort()),
            listOf(0x79E9.toUShort(), 0xF642.toUShort()),
            listOf(0x79E9.toUInt(), 0xF642.toUInt()),
            listOf(0x79E9.toLong(), 0xF642.toLong()),
            listOf(0x79E9.toULong(), 0xF642.toULong())
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


