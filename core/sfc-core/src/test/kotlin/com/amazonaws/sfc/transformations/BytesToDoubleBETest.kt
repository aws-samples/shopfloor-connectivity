
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

class BytesToDoubleBETest {

    val o = BytesToDoubleBE.create()

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
                "Operator": "BytesToDoubleBE"
            }"""
        assertEquals(BytesToDoubleBE.create(), BytesToDoubleBE.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun logic() {
        val bytes = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putDouble(123.456).array().map{it}
        val result = o.invoke(bytes)
        assertEquals(123.456, result, "To Double")
        if (result != null) {
            assertEquals(result::class, Double::class, "Type check")
        }
    }


}