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

class TanhTest {

    @Test
    fun `create and validation`() {
        Assertions.assertDoesNotThrow {
            Tanh.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "Tanh"
            }"""
        assertEquals(Tanh.create(), Tanh.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic an return types`() {
        val testValues = listOf<Pair<Number?, Number?>>(
            Pair(0, kotlin.math.tanh(0.0)),
            Pair(0.toByte(), kotlin.math.tanh(0.0)),
            Pair(0.toShort(), kotlin.math.tanh(0.0)),
            Pair(0.toLong(), kotlin.math.tanh(0.0)),
            Pair(0.0f, kotlin.math.tanh(0.0f)),
            Pair(0.0, kotlin.math.tanh(0.0)),
            Pair(null, null),
            Pair(Double.NaN, null)
        )

        val tanh = Tanh.create()
        for (v in testValues) {
            val target = v.first
            val result = tanh.apply(target)
            assertEquals(v.second, result, if (v.first != null) v.first!!::class.java.name else "null")

        }
    }
}