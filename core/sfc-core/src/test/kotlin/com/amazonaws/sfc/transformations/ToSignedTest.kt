// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ToSignedTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            ToSigned.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {

        val json = """
            {
                "Operator": "ToSigned"
            }"""
        assertEquals(ToSigned.create(), ToSigned.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {
        val testValues =
            listOf(

                Pair(1.toUByte(), 1.toByte()),
                Pair(1.toUShort(), 1.toShort()),
                Pair(1.toUInt(), 1),
                Pair(1.toULong(), 1.toLong()),

                Pair(1.toByte(), 1.toByte()),
                Pair(1.toShort(), 1.toShort()),
                Pair(1, 1),
                Pair(1.toLong(), 1.toLong()),
                Pair(1.0f, 1.0f),
                Pair(1.0, 1.0),

                Pair(listOf(1.toUInt(), 2.toUInt()), listOf(1,2))
            )

        val o = ToSigned.create()
        for (v in testValues) {
            val target: Any = v.first
            val result = o.invoke(target)
            assertEquals(result, v.second, v::class.java.name)
            if (result != null && (result !is List<*>)) assertEquals(v.second::class, result::class)
        }
    }
}