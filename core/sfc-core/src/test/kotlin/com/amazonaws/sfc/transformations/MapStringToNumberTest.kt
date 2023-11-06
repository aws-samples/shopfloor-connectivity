
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import io.mockk.every
import io.mockk.mockkConstructor
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MapStringToNumberTest {

    private val mapping =
        MapStringToNumber.Mapping(Map = mapOf(
            "One" to 1,
            "Two" to 2,
            "Three" to 3
        ) as HashMap<String, Int>,
            Default = 0
        )


    @Test
    fun `create and validate`() {

        Assertions.assertDoesNotThrow {
            MapStringToNumber.create(mapping).validate()
        }

        Assertions.assertThrows(ConfigurationException::class.java) {
            MapStringToNumber.create(MapStringToNumber.Mapping(Map = HashMap(), 0)).validate()
        }
    }

    @Test
    fun `create and validate mapping`() {

        Assertions.assertDoesNotThrow {
            MapStringToNumber.Mapping(Map = mapOf(
                "One" to 1,
                "Two" to 2,
                "Three" to 3
            ) as HashMap<String, Int>,
                Default = 0
            )
        }

        Assertions.assertDoesNotThrow {
            MapStringToNumber.Mapping(Map = mapOf(
                "One" to 1,
                "Two" to 2,
                "Three" to 3
            ) as HashMap<String, Int>)
        }

        Assertions.assertThrows(ConfigurationException::class.java) {
            MapStringToNumber.Mapping(hashMapOf<String, Int>(), 0).validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "MapStringToNumber",
                "Operand": {
                    "Map": {
                        "One Hundred": 100,
                        "null": 0
                    },
                    "Default": 1
                }
            }"""
        assertEquals(MapStringToNumber.create(MapStringToNumber.Mapping(Map = mapOf("One Hundred" to 100, "null" to 0) as HashMap, Default = 1)),
            MapStringToNumber.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `error reading operand from json`() {
        val json = """
            {
                "Operator": "MapStringToNumber",
                "Operand": {
                    "Map": {
                        "One Hundred": 100,
                        "null": 0
                    },
                    "Default": 1
                }
            }"""

        mockkConstructor(Gson::class)
        every { anyConstructed<Gson>().fromJson(any<String>(), MapStringToNumber.Mapping::class.java) } throws Exception("Mocked Exception")

        assertThrows<TransformationException> {
            MapStringToNumber.fromJson(Gson().fromJson(json, JsonObject::class.java))
        }

        every { anyConstructed<Gson>().fromJson(any<String>(), MapStringToNumber.Mapping::class.java) } throws JsonSyntaxException("Mocked Exception")

        assertThrows<TransformationException> {
            MapStringToNumber.fromJson(Gson().fromJson(json, JsonObject::class.java))
        }
    }

    @Test
    fun `empty map`() {
        Assertions.assertNull(MapStringToNumber(null).invoke(""), "Empty map")
    }

    @Test
    fun `operator logic`() {
        val o = MapStringToNumber.create(mapping)
        for (s in mapping.Map.keys) {
            val mapped = o.invoke(s)
            assertEquals(mapping.Map[s], mapped, "Mapping $s to ${mapping.Map[s]}")
        }

        assertEquals(0, o.invoke(""), "Default value")
    }

    @Test
    fun testHashCode() {
        val op = MapStringToNumber.create(mapping)
        assertEquals(-1785493395, op.hashCode())

    }

    @Test
    fun testToString() {
        val op = MapStringToNumber.create(mapping)
        assertEquals("""MapStringToNumber(["One":1, "Two":2, "Three":3], Default=0))""", op.toString())

    }
}