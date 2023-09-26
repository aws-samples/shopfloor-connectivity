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

class ModTest {


    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Mod.create(1).validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val jsonLong = """
            {
                "Operator": "Mod", 
                "Operand" : 1
            }"""

        val jsonShort = """
            {
                "Operator": "%", 
                "Operand" : 1
            }"""

        for (json in listOf(jsonLong, jsonShort)) {
            assertEquals(Mod.create(1), Mod.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
        }
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Triple<Number?, Number, Number?>>(
            // target, mod value, result
            Triple(5, 2, 1),
            Triple(5.toByte(), 2.toByte(), 1.toByte()),
            Triple(5.toShort(), 2.toShort(), 1.toShort()),
            Triple(5.toLong(), 2.toLong(), 1.toLong()),
            Triple(5.0, 2.0, 1.0),
            Triple(5.0.toFloat(), 2.0.toFloat(), 1.0.toFloat()),
            Triple(null, 0, null)

        )

        for (v in testValues) {
            val result = Mod(v.second).apply(v.first)
            assertEquals(v.third, result, "Test ${if (v.first != null) v.first!!::class.simpleName else "null"} mod ${v.second}")
            if (result != null && v.first != null) {
                assertEquals(v.first!!::class.java, result::class.java, "Check type")
            }
        }
    }
}