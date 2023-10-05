/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.mockk.every
import io.mockk.mockkConstructor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


class OutsideRangeInclusiveTest {


    private val range = Range.create(minValue = 1, maxValue = 10)

    @Test
    fun `create and validate`() {
        assertDoesNotThrow {
            val testRange = Range.create(minValue = 1, maxValue = 10)
            OutsideRangeInclusive.create(testRange).validate()
        }

        assertThrows(ConfigurationException::class.java) {
            OutsideRangeInclusive.create(Range.create()).validate()
        }

    }

    @Test
    fun `error reading operand from json`() {
        val json = """
            {     
                "Operator": "OutsideRangeInclusive",      
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
                "Operator": "OutsideRangeInclusive",      
                "Operand": { 
                    "MinValue" : 1, 
                    "MaxValue" : 10 
                }            
            }"""

        assertEquals(
            OutsideRangeInclusive.create(range),
            OutsideRangeInclusive.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }


    @Test
    fun logic() {

        val testValues = listOf(
            Pair(0, true),
            Pair(0.toByte(), true),
            Pair(0.toShort(), true),
            Pair(0L, true),
            Pair(0.0, true),
            Pair(0.0.toFloat(), true),
            Pair(0.9, true),
            Pair(0.9.toFloat(), true),
            Pair(1, true),
            Pair(1.toByte(), true),
            Pair(1.toShort(), true),
            Pair(1L, true),
            Pair(1.0, true),
            Pair(1.0.toFloat(), true),
            Pair(1.1, false),
            Pair(1.1.toFloat(), false),
            Pair(2, false),
            Pair(2.toByte(), false),
            Pair(2.toShort(), false),
            Pair(2L, false),
            Pair(2.0, false),
            Pair(2.0.toFloat(), false),
            Pair(9, false),
            Pair(9.toByte(), false),
            Pair(9.toShort(), false),
            Pair(9L, false),
            Pair(9.0, false),
            Pair(9.0.toFloat(), false),
            Pair(9.9, false),
            Pair(9.9.toFloat(), false),
            Pair(10, true),
            Pair(10.toByte(), true),
            Pair(10.toShort(), true),
            Pair(10L, true),
            Pair(10.0, true),
            Pair(10.0.toFloat(), true),
            Pair(10.1, true),
            Pair(10.1.toFloat(), true),
            Pair(11, true),
            Pair(11.toByte(), true),
            Pair(11.toShort(), true),
            Pair(11L, true),
            Pair(11.0, true),
            Pair(11.0.toFloat(), true)
        )

        val op = OutsideRangeInclusive(range)
        for (v in testValues) {
            assertEquals(v.second, op.apply(v.first), "${v.first}")
        }
    }

    @Test
    fun `test toString`() {
        assertEquals("OutsideRangeInclusive([1..10])", OutsideRangeInclusive(range).toString())
    }

}