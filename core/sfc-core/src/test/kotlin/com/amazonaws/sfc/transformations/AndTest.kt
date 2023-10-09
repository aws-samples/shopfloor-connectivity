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
import org.junit.jupiter.api.Test

class AndTest {
    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            And.create(1).validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val jsonFull = """
            {
                "Operator": "And",
                "Operand": 1
            }"""

        val jsonShort = """
            {
                "Operator": "&",
                "Operand": 0x0F
            }"""

        for (json in listOf(jsonFull, jsonShort)) {
            assertEquals(And.create(1), And.fromJson(Gson().fromJson(jsonFull, JsonObject::class.java)), "from $json")
        }
    }

    @Test
    fun `operator logic and return types`() {
        val testValues = listOf<Triple<Number?, Number, Number?>>(
            // target, and mask, result
            Triple(1, 1, 1),
            Triple(1.toByte(), 1.toByte(), 1.toByte()),
            Triple(1.toShort(), 1.toShort(), 1.toShort()),
            Triple(1.toLong(), 1.toLong(), 1.toLong()),
            Triple(1, 0, 0),
            Triple(1.toByte(), 0.toByte(), 0.toByte()),
            Triple(1.toShort(), 0.toShort(), 0.toShort()),
            Triple(1.toLong(), 0.toLong(), 0.toLong()),
            Triple(1.0, 1.0, 1.0),
            Triple(null, 1, null)
        )

        for (test in testValues) {
            val operand = And.create(test.second)
            val result = operand.apply(test.first)
            assertEquals(test.third, result, if (test.first != null) test.first!!::class.java.name else "null")
            if (result != null && test.first != null) {
                assertEquals(test.first!!::class, result::class)
            }
        }
    }
}