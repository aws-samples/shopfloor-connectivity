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

class ShlTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Shl.create(1).validate()
        }
    }

    @Test
    fun `deserialization from json`() {
        val jsonLong = """
            {
                "Operator": "Shl", 
                "Operand" : 1
            }"""

        val jsonShort = """
            {
                "Operator": "<<", 
                "Operand" : 1
            }"""

        for (json in listOf(jsonLong, jsonShort)) {
            assertEquals(Shl(1), Shl.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
        }
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Triple<Number, Int, Number?>>(
            // target, shl, result
            Triple(1, 1, 2),
            Triple(1.toByte(), 1, 2.toByte()),
            Triple(1.toShort(), 1, 2.toShort()),
            Triple(1.toLong(), 1, 2.toLong()),
            Triple(1.0, 1, null)
        )
        for (v in testValues) {
            val o = Shl.create(v.second)
            val result = o.invoke(v.first)
            assertEquals(v.third, result, v.first::class.java.name)
            if (result != null) {
                assertEquals(v.first::class, result::class)
            }
        }
    }
}