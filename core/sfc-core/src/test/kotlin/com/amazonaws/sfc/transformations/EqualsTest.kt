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

class EqualsTest {


    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Equals.create(1).validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val jsonLong = """
            {
                "Operator": "Equals", 
                "Operand" : 1
            }"""

        val jsonShort = """
            {
                "Operator": "==", 
                "Operand" : 1
            }"""

        for (json in listOf(jsonLong, jsonShort)) {
            assertEquals(Equals.create(1), Equals.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
        }
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Triple<Number?, Number?, Boolean>>(
            Triple(1, 1, true),
            Triple(1, 0, false),
            Triple(1.toByte(), 1.toByte(), true),
            Triple(1.toByte(), 0.toByte(), false),
            Triple(1.toShort(), 1.toShort(), true),
            Triple(1.toShort(), 0.toShort(), false),
            Triple(1.toLong(), 1.toLong(), true),
            Triple(1.toLong(), 0.toLong(), false),
            Triple(1.0, 1.0, true),
            Triple(1.0, 0.0, false),
            Triple(1.0.toFloat(), 1.0.toFloat(), true),
            Triple(1.0.toFloat(), 0.0.toFloat(), false),
            Triple(2.0, null, false),
            Triple(2, 0, false),
            Triple(null, null, true),
            Triple(null, 1, false),
            Triple(1, null, false)
        )

        for (v in testValues) {
            assertEquals(v.third, Equals.create(v.second).apply(v.first), "${v.first} == ${v.second}")
        }
    }
}