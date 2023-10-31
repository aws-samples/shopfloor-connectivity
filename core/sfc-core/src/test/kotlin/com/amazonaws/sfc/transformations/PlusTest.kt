/*

Copyright (c) 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved. 
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
import org.junit.jupiter.api.Test

class PlusTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Plus.create(1).validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val jsonLong = """
            {
                "Operator": "Plus", 
                "Operand" : 1
            }"""

        val jsonShort = """
            {
                "Operator": "+", 
                "Operand" : 1
            }"""

        for (json in listOf(jsonLong, jsonShort)) {
            assertEquals(Plus(1), Plus.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
        }
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Triple<Number?, Number, Number?>>(
            // target, minus value, result
            Triple(2, 1, 3),
            Triple(2.toByte(), 1.toByte(), 3.toByte()),
            Triple(2.toShort(), 1.toShort(), 3.toShort()),
            Triple(2.toLong(), 1.toLong(), 3.toLong()),
            Triple(2.0, 1.0, 3.0),
            Triple(2.0.toFloat(), 1.0.toFloat(), 3.0.toFloat()),
            Triple(Double.MAX_VALUE, Double.MAX_VALUE, null),
            Triple(null, 0, null)

        )

        for (v in testValues) {
            val result = Plus(v.second).apply(v.first)
            assertEquals(v.third, result, "Test ${if (v.first != null) v.first!!::class.simpleName else "null"} + ${v.second}")
            if (result != null && v.first != null) {
                assertEquals(v.first!!::class.java, result::class.java, "Check type")
            }
        }
    }
}