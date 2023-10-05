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

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import io.mockk.every
import io.mockk.mockkConstructor
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SubStringTest {


    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            SubString.create(StrRange.create(start = 0, end = 1)).validate()
        }

        Assertions.assertDoesNotThrow {
            SubString.create(StrRange.create()).validate()
        }
    }

    @Test
    fun `deserialization from json`() {
        val json = """
            {
                "Operator": "SubString", 
                "Operand": {
                    "Start": 2, 
                    "End": 4
                }
            }"""
        assertEquals(SubString.create(StrRange.create(start = 2, end = 4)), SubString.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `empty result`() {
        Assertions.assertNull(SubString(null).invoke(""), "empty substring range")
    }

    @Test
    fun `operator logic`() {
        val testString = "012345"
        val testValues = listOf(
            Triple(0, 2, testString.substring(0, 2)),
            Triple(testString.length, 1, ""),
            Triple(0, testString.length + 1, testString),
            Triple(3, 3, ""),
            Triple(4, 3, ""),
            Triple(-2, -1, "4"),
            Triple(-10, -12, ""),
            Triple(-1, -1, "")
        )

        for (v in testValues) {
            val o = SubString(StrRange.create(start = v.first, end = v.second))
            assertEquals(v.third, o.invoke(testString), "Substring start $={v.first}, length = ${v.second}")
        }
    }

    @Test
    fun `error reading operand from json`() {
        val json = """
            {
                "Operator": "SubString",
                "Operand": {}
            }"""

        mockkConstructor(Gson::class)
        every { anyConstructed<Gson>().fromJson(any<String>(), StrRange::class.java) } throws Exception("Mocked Exception")

        assertThrows<TransformationException> {
            SubString.fromJson(Gson().fromJson(json, JsonObject::class.java))
        }

        every { anyConstructed<Gson>().fromJson(any<String>(), StrRange::class.java) } throws JsonSyntaxException("Mocked Exception")

        assertThrows<TransformationException> {
            SubString.fromJson(Gson().fromJson(json, JsonObject::class.java))
        }
    }

}