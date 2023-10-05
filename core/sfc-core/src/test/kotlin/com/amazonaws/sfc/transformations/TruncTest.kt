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

class TruncTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Trunc.create().validate()
        }
    }

    @Test
    fun `deserialization from json`() {

        val json = """
            {
                "Operator": "Trunc"
            }"""
        assertEquals(Trunc.create(), Trunc.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Pair<Number, Number?>>(
            // target, result
            Pair(12, 12),
            Pair(12.toByte(), 12.toByte()),
            Pair(12.toShort(), 12.toShort()),
            Pair(12.toLong(), 12.toLong()),
            Pair(12.123, 12.0),
            Pair(12.123.toFloat(), 12.0.toFloat())
        )

        for (v in testValues) {
            val result = Trunc.create().invoke(v.first)
            assertEquals(v.second, result, "Test trunc  ${v.first::class}")
            if (result != null) assertEquals(v.first::class.java, result::class.java, "Check type")

        }
    }
}