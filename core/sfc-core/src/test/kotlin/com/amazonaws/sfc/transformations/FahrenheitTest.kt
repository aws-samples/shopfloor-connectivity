
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FahrenheitTest {


    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Fahrenheit.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "Fahrenheit"
            }"""
        assertEquals(Fahrenheit.create(), Fahrenheit.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Pair<Number, Number>>(
            // target in Celsius, result in Fahrenheit


            Pair(-40, -40.0),
            Pair((-40).toByte(), -40.0),
            Pair((-40).toShort(), -40.0),
            Pair((-40).toLong(), -40.0),

            // Double
            Pair(-40.0, -40.0),

            //Float
            Pair(-40.0f, -40.0f)
        )

        val o = Fahrenheit.create()
        for (v in testValues) {
            val target: Number = v.first
            val result = o.invoke(target)
            assertEquals(v.second, result, v.first::class.java.name + " : " + v.first)
        }
    }
}