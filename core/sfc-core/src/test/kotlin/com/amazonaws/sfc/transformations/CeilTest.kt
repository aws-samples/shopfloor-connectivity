
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CeilTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Ceil.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "Ceil"
            }"""

        assertEquals(Ceil.create(), Ceil.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Pair<Number, Number>>(
            // target, result

            // Integer variants positive
            Pair(1, 1),
            Pair(1.toByte(), 1.toByte()),
            Pair(1.toShort(), 1.toShort()),
            Pair(1.toLong(), 1.toLong()),

            // Integer variants negative
            Pair(-1, -1),
            Pair((-1).toByte(), (-1).toByte()),
            Pair((-1).toShort(), (-1).toShort()),
            Pair((-1).toLong(), (-1).toLong()),

            // Double
            Pair(1.0, 1.0),
            Pair(-1.0, -1.0),
            Pair(1.1, 2.0),
            Pair(-1.1, -1.0),

            //Float
            Pair(1.0f, 1.0f),
            Pair(-1.0f, -1.0f),
            Pair(1.1f, 2.0f),
            Pair(-1.1f, -1.0f)

        )

        val o = Ceil.create()
        for (v in testValues) {
            val target: Number = v.first
            val result = o.invoke(target)
            assertEquals(v.second, result, v.first::class.java.name)
            if (result != null) assertEquals(target::class, result::class)
        }
    }
}