/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class QueryTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Query.create("@").validate()
        }

        Assertions.assertThrows(ConfigurationException::class.java) {
            Query.create("%#*").validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "Query", 
                "Operand" : "@"
            }"""
        assertEquals(Query("@"), Query.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {
        val testData = mapOf<String, Any?>(
            "A" to 1,
            "B" to mapOf<String, Any?>("C" to 3),
            "D" to listOf(0, 1, 2, 3),
            "E" to listOf(mapOf("F" to 1, "G" to 2), mapOf("F" to 3, "G" to 4)),
            "H" to listOf<Any>(1, "str")
        )

        val testValues = listOf(
            Triple("@", testData, testData),
            Triple("@.A", testData, testData["A"]),
            Triple("@.B.C", testData, 3),
            Triple("@.X", testData, null),
            Triple("", testData, null),
            Triple("$%#*", testData, null),
            Triple("@.D[1]", testData, 1),
            Triple("@.D[1:]", testData, listOf(1, 2, 3)),
            Triple("@.E[?F==`3`]|[0].G", testData, 4),
            Triple("@.H[1]", testData, "str"),
            Triple("@", 1, 1),
            Triple("@[0]", listOf(0, 1, 2, 3), 0),
            Triple("@[99]", listOf(0, 1, 2, 3), null)
        )

        testValues.forEach { (query, input_data, expected_output) ->
            assertEquals(expected_output, Query(query).invoke(input_data), query)
        }
    }

    @Test
    fun `handling query throwing exception`() {
        assertNull(Query("@.z").apply(Any()))
    }

    @Test
    fun `handling loading invalid query throwing exception`() {
        val json = """
            {
                "Operator": "Query",
                "Operand": "$%!^"
            }"""

        assertThrows<TransformationException> {
            Query.fromJson(Gson().fromJson(json, JsonObject::class.java))
        }
    }
}