
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DecodeToStringTest {

    @Test
    fun `can create`() {
        assertDoesNotThrow {
            DecodeToString()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "DecodeToString"
            }"""
        assertEquals(DecodeToString.create(), DecodeToString.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun decoding() {
        val str = "Hello"
        val bytes = str.encodeToByteArray().toList()
        val decodeToString = DecodeToString()

        assertEquals(str, decodeToString(bytes))

        assertNull(null, decodeToString.apply(null))
    }

}