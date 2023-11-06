
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BytesToInt16Test {

    val o = BytesToInt16.create()

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            BytesToInt16.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "BytesToInt16"
            }"""
        assertEquals(BytesToInt16.create(), BytesToInt16.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun logic() {
        val result2 = o.invoke(listOf(0xf0.toByte(), 0xf0.toByte()))
        assertEquals(result2, 0xf0f0.toShort(), "Two bytes")
        if (result2 != null) {
            assertEquals(result2::class, Short::class, "Two bytes type")
        }
    }

    @Test
    fun `invalid input logic`() {
        val result1 = o.invoke(listOf(0xf0.toByte()))
        assertEquals(null, result1, "Single byte")
    }
}