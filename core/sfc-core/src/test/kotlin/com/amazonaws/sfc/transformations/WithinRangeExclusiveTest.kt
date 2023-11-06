
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.mockk.every
import io.mockk.mockkConstructor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


class WithinRangeExclusiveTest {


    private val range = Range.create(minValue = 1, maxValue = 10)

    @Test
    fun `create and validate`() {
        assertDoesNotThrow {
            val testRange = Range.create(minValue = 1, maxValue = 10)
            WithinRangeExclusive.create(testRange).validate()
        }

        assertThrows(ConfigurationException::class.java) {
            WithinRangeExclusive.create(Range.create()).validate()
        }

    }

    @Test
    fun `error reading operand from json`() {
        val json = """
            {     
                "Operator": "WithinRangeExclusive",      
                "Operand": { }            
            }"""

        mockkConstructor(Gson::class)
        every { anyConstructed<Gson>().fromJson(any<String>(), MapRange.Map::class.java) } throws Exception("Mocked Exception")

        assertThrows<TransformationException> {
            MapRange.fromJson(Gson().fromJson(json, JsonObject::class.java))
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {     
                "Operator": "WithinRangeExclusive",      
                "Operand": { 
                    "MinValue" : 1, 
                    "MaxValue" : 10 
                }            
            }"""

        assertEquals(
            WithinRangeExclusive.create(range),
            WithinRangeExclusive.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }


    @Test
    fun logic() {

        val testValues = listOf(
            Pair(0, false),
            Pair(0.toByte(), false),
            Pair(0.toShort(), false),
            Pair(0L, false),
            Pair(0.0, false),
            Pair(0.0.toFloat(), false),
            Pair(0.9, false),
            Pair(0.9.toFloat(), false),
            Pair(1, false),
            Pair(1.toByte(), false),
            Pair(1.toShort(), false),
            Pair(1L, false),
            Pair(1.0, false),
            Pair(1.0.toFloat(), false),
            Pair(1.1, true),
            Pair(1.1.toFloat(), true),
            Pair(2, true),
            Pair(2.toByte(), true),
            Pair(2.toShort(), true),
            Pair(2L, true),
            Pair(2.0, true),
            Pair(2.0.toFloat(), true),
            Pair(9, true),
            Pair(9.toByte(), true),
            Pair(9.toShort(), true),
            Pair(9L, true),
            Pair(9.0, true),
            Pair(9.0.toFloat(), true),
            Pair(9.9, true),
            Pair(9.9.toFloat(), true),
            Pair(10, false),
            Pair(10.toByte(), false),
            Pair(10.toShort(), false),
            Pair(10L, false),
            Pair(10.0, false),
            Pair(10.0.toFloat(), false),
            Pair(10.1, false),
            Pair(10.1.toFloat(), false),
            Pair(11, false),
            Pair(11.toByte(), false),
            Pair(11.toShort(), false),
            Pair(11L, false),
            Pair(11.0, false),
            Pair(11.0.toFloat(), false)
        )

        val op = WithinRangeExclusive(range)
        for (v in testValues) {
            assertEquals(v.second, op.apply(v.first), "${v.first}")
        }
    }


    @Test
    fun `test toString`() {
        assertEquals("WithinRangeExclusive([1..10])", WithinRangeExclusive(range).toString())
    }

}