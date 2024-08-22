
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ChunkedTest {

    val o = Chunked.create(2)

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Chunked.create(2).validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "Chunked",
                "Operand" : 2
            }"""
        val j =  Chunked.fromJson(Gson().fromJson(json, JsonObject::class.java))
        println(j)
        assertEquals(Chunked.create(2), Chunked.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun logic() {


        val testValues = listOf<Pair<List<*>, Int>>(
            Pair(listOf(1,2,3,4), 2),
            Pair(listOf(1,2,3,4,5), 3),
            Pair(listOf(1,2,3,4,5), 1),
            Pair(emptyList<Int>(), 2)

        )
        testValues.forEach { v->
            val result = Chunked.create( v.second).invoke(v.first)
            assertEquals(v.first.chunked(v.second), result)
            assertEquals(v.first, (result as List<*>).flatMap { it as List<*> })
        }
    }


}