/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Log2Test {

    @Test
    fun `create and validation`() {
        Assertions.assertDoesNotThrow {
            Log2.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {

        val json = """
            {
               "Operator": "Log2"
            }"""
        assertEquals(Log2.create(), Log2.fromJson(Gson().fromJson(json, JsonObject::class.java)))
    }

    @Test
    fun `operator logic and return types`() {
        val testValues = listOf<Pair<Number?, Number?>>(
            Pair(0.0.toFloat(), kotlin.math.log2(0.0.toFloat())),
            Pair(1.0.toFloat(), kotlin.math.log2(1.0.toFloat())),
            Pair(0.0, kotlin.math.log2(0.0)),
            Pair(1.0, kotlin.math.log2(1.0)),
            Pair(0, kotlin.math.log2(0.0)),
            Pair(1, kotlin.math.log2(1.0)),
            Pair(null, null),
            Pair(Double.NaN, null),
            Pair(-1, null))


        val log2 = Log2.create()
        for (v in testValues) {
            val target: Number? = v.first
            val result = log2.apply(target)
            assertEquals(v.second, result, if (v.first != null) v.first!!::class.java.name else "null")
            if (result != null) {
                assertTrue(result::class == if (target is Float) Float::class else Double::class)
            }
        }
    }

}

