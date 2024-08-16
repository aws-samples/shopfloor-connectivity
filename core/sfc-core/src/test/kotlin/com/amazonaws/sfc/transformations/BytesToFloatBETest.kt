
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BytesToFloatBETest {

    val o = BytesToFloatBE.create()

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            BytesToFloatBE.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "BytesToFloatBE"
            }"""
        assertEquals(BytesToFloatBE.create(), BytesToFloatBE.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun logic() {
        val bytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putFloat(123.456f).array().map{it}
        val result = o.invoke(bytes)
        assertEquals(123.456f, result, "To Float")
        if (result != null) {
            assertEquals(result::class, Float::class, "Type check")
        }
    }


}