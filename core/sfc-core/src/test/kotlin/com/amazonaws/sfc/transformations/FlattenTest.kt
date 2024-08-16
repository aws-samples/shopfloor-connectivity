
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FlattenTest {


    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Flatten.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "Flatten"
            }"""
        assertEquals(Flatten.create(), Flatten.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {

        val testValues = listOf<Pair<Any, Any>>(
            // target in Celsius, result in Fahrenheit

            Pair(emptyList<Int>(), emptyList<Int>()),
            Pair(1, listOf(1)),
            Pair(listOf(1, 2, 3), listOf(1, 2, 3)),
            Pair(listOf(listOf(1, 2, 3), listOf(4, 5, 6)), listOf(1,2,3,4,5,6)),
            Pair(listOf(listOf(
                listOf(1, 2, 3), listOf(4, 5, 6)), listOf(listOf(7, 8, 9), listOf(10, 11, 12))), listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12))
        )


        val o = Flatten.create()
        for (v in testValues) {
            val target: Any = v.first
            val result = o.invoke(target)
            assertEquals(v.second, result, v.first::class.java.name + " : " + v.first)
        }
    }
}