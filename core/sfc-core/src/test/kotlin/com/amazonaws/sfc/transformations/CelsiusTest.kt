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

class CelsiusTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Celsius.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "Celsius"
            }"""
        assertEquals(Celsius.create(), Celsius.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }


    @Test
    fun `operator logic`() {
        val testValues = listOf<Pair<Number, Number>>(
            // target in Fahrenheit, result in Celsius
            Pair(-40, -40.0),
            Pair((-40).toByte(), -40.0),
            Pair((-40).toShort(), -40.0),
            Pair((-40).toLong(), -40.0),

            // Double
            Pair(-40.0, -40.0),

            //Float
            Pair(-40.0f, -40.0f)
        )

        val o = Celsius.create()
        for (v in testValues) {
            val target: Number = v.first
            val result = o.invoke(target)
            assertEquals(v.second, result, v.first::class.java.name + " : " + v.first)
        }
    }
}