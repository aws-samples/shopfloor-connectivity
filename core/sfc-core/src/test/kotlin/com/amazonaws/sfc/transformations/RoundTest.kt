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

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoundTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Round.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "Round"
            }"""
        assertEquals(Round.create(), Round.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Pair<Number, Number>>(
            // target, result
            Pair(2, 2),
            Pair(2.toByte(), 2.toByte()),
            Pair(2.toShort(), 2.toShort()),
            Pair(2.toLong(), 2.toLong()),
            Pair(2.6, 3.0),
            Pair(2.4, 2.0),
            Pair(2.6.toFloat(), 3.0.toFloat()),
            Pair(2.4.toFloat(), 2.0.toFloat())
        )

        for (v in testValues) {
            val result = Round.create().invoke(v.first)
            assertEquals(v.second, result, "Test round ${v.first::class}")
            if (result != null) assertEquals(v.first::class.java, result::class.java, "Check type")

        }
    }
}