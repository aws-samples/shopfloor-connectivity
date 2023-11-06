
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SinTest {

    @Test
    fun `create and validation`() {
        Assertions.assertDoesNotThrow {
            Sin.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "Sin"
            }"""
        assertEquals(Sin.create(), Sin.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic an return types`() {
        val testValues = listOf<Pair<Number?, Number?>>(
            Pair(0, kotlin.math.sin(0.0)),
            Pair(0.toByte(), kotlin.math.sin(0.0)),
            Pair(0.toShort(), kotlin.math.sin(0.0)),
            Pair(0.toLong(), kotlin.math.sin(0.0)),
            Pair(0.0f, kotlin.math.sin(0.0f)),
            Pair(0.0, kotlin.math.sin(0.0)),
            Pair(null, null),
            Pair(Double.NaN, null)
        )

        val sin = Sin.create()
        for (v in testValues) {
            val target = v.first
            val result = sin.apply(target)
            assertEquals(v.second, result, if (v.first != null) v.first!!::class.java.name else "null")

        }
    }
}