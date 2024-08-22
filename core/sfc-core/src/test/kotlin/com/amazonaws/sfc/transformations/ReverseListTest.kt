// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReverseListTest {

    val o = ReverseList.create()

    @Test
    fun `create`() {
        Assertions.assertDoesNotThrow {
            ReverseList()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "ReverseList"
            }"""
        assertEquals(ReverseList(), ReverseList.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun logic() {

        val testValues = listOf<List<*>>(
            listOf(1, 2, 3, 4),
            listOf(1),
            emptyList<Int>()
        )
        testValues.forEach { v ->
            val result = o.invoke(v)
            assertEquals(v.reversed(), result)
        }
    }


}