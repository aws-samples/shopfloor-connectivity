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

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test


class StrEqualsNoCaseTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            StrEqualsNoCase.create("").validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "StrEqualsNoCase",
                "Operand" : "ABC"
            }"""
        assertEquals(StrEqualsNoCase.create("ABC"), StrEqualsNoCase.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {

        val testValues = listOf<Triple<String?, String, Boolean>>(
            Triple("A", "A", true),
            Triple("A", "a", true),
            Triple("A", "B", false),
            Triple("", "", true)
        )

        for (v in testValues) {
            val strEqualsNoCase = StrEqualsNoCase(v.second)
            assertEquals(v.third, strEqualsNoCase.apply(v.first), "${v.first}, ${v.second}")
        }
    }

    @Test
    fun `handling invalid array operand`() {
        val json = """
            {
                "Operator": "StrEqualsNoCase",
                "Operand" : ["ABC", "DEF"]
            }"""

        assertThrows(TransformationException::class.java) {
            StrEqualsNoCase.fromJson(Gson().fromJson(json, JsonObject::class.java))
        }
    }

    @Test
    fun `handling invalid structure operand`() {
        val json = """
            {
                "Operator": "StrEqualsNoCase",
                "Operand" : { "A" : "B"}
            }"""

        assertThrows(TransformationException::class.java) {
            StrEqualsNoCase.fromJson(Gson().fromJson(json, JsonObject::class.java))
        }
    }
}