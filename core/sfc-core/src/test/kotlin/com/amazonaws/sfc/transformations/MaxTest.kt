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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MaxTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Max.create(1).validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "Max", 
                "Operand" : 1
            }"""
        assertEquals(Max(1), Max.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Triple<Number?, Number, Number?>>(
            // target, test value, result
            Triple(1, 2, 2),
            Triple(1.toByte(), 2.toByte(), 2.toByte()),
            Triple(1.toShort(), 2.toShort(), 2.toShort()),
            Triple(1.toLong(), 2.toLong(), 2.toLong()),
            Triple(1.0, 2.0, 2.0),
            Triple(1.0.toFloat(), 2.0.toFloat(), 2.0.toFloat()),
            Triple(null, 1, null)
        )

        for (v in testValues) {
            val result = Max.create(v.second).apply(v.first)
            assertEquals(v.third, result, "Test max of ${if (v.first != null) v.first!!::class.simpleName else "null"} with value ${v.first} and ${v.second}")
            if (result != null && v.first != null) {
                assertEquals(v.first!!::class.java, result::class.java, "Check type")
            }
        }
    }
}