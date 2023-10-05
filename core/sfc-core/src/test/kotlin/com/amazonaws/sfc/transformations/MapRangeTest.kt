/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


class MapRangeTest {

    @Test
    fun `create and validate`() {
        assertDoesNotThrow {
            MapRange.create(MapRange.Map.create(
                from = Range.create(minValue = 1, maxValue = 20),
                to = Range.create(minValue = 1, maxValue = 10)
            )).validate()
        }

        assertThrows(ConfigurationException::class.java) {
            MapRange.create(MapRange.Map.create(
                from = Range.create(minValue = 1, maxValue = 1),
                to = Range.create(minValue = 1, maxValue = 10)
            )).validate()
        }

        assertDoesNotThrow {
            MapRange()
        }
    }

    @Test
    fun `error reading operand from json`() {
        val json = """
            {     
                "Operator": "MapRange",      
                "Operand": {}            
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
                "Operator": "MapRange",      
                "Operand": {        
                    "To": {          
                        "MaxValue": 100,          
                        "MinValue": 0        
                    },        
                    "From": {          
                        "MaxValue": 200,          
                        "MinValue": 0        
                    }      
                }    
            }"""
        assertEquals(MapRange.create(MapRange.Map.create(
            from = Range.create(minValue = 0, maxValue = 200),
            to = Range.create(minValue = 0, maxValue = 100)
        )), MapRange.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }


    @Test
    fun `invalid ranges`() {
        Assertions.assertNull(
            MapRange.create(MapRange.Map.create(
                from = Range.create(minValue = 1, maxValue = 1),
                to = Range.create(minValue = 1, maxValue = 10)
            )
            ).invoke(1), "Invalid input range")

        Assertions.assertNull(
            MapRange(
                MapRange.Map.create(
                    from = Range.create(minValue = 1, maxValue = 10),
                    to = Range.create(minValue = 1, maxValue = 1)
                )
            ).invoke(1), "Invalid output range")
    }

    @Test
    fun `empty map`() {
        Assertions.assertNull(MapRange(null).invoke(1), "Empty range map")
    }

    @Test
    fun `scale down logic`() {
        val scaleDownMap = MapRange.Map.create(
            from = Range.create(minValue = 0, maxValue = 1024),
            to = Range.create(minValue = 1, maxValue = 10)
        )

        val scaleDownValues = listOf(
            Pair(0, 1L),
            Pair(1024, 10L),
            Pair(0, 1L),
            Pair(0.toByte(), 1L),
            Pair(0.0, 1.0),
            Pair(0L, 1L),
            Pair(-1, null),
            Pair(1025, null)
        )

        val oDown = MapRange.create(scaleDownMap)
        for (v in scaleDownValues) {
            assertEquals(v.second, oDown.invoke(v.first), "Mapping ${v.first} down to ${v.second}")
        }
    }

    @Test
    fun `scale up logic`() {
        val scaleUpMap = MapRange.Map.create(
            from = Range.create(minValue = 1, maxValue = 10),
            to = Range.create(minValue = 0, maxValue = 1024)
        )

        val scaleUpValues = listOf(
            Pair(1, 0L),
            Pair(10, 1024L),
            Pair(0, null),
            Pair(11, null)
        )

        val oUp = MapRange.create(scaleUpMap)
        for (v in scaleUpValues) {
            assertEquals(v.second, oUp.invoke(v.first), "Mapping ${v.first} up to ${v.second}")
        }
    }

    @Test
    fun `test toString`() {
        val map = MapRange.Map.create(
            from = Range.create(minValue = 0, maxValue = 1024),
            to = Range.create(minValue = 1, maxValue = 10)
        )

        assertEquals("(From=[0..1024], To=[1..10])", map.toString())
    }

}