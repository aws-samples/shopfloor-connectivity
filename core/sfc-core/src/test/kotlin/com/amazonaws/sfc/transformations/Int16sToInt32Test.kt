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

class Int16sToInt32Test {

    private val o = Int16sToInt32.create()

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Int16sToInt32.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
            "Operator": "Int16sToInt32"
            }"""
        assertEquals(Int16sToInt32.create(), Int16sToInt32.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {
        val result2 = o.invoke(listOf(0xf0f0.toShort(), 0xf0f0.toShort()))
        assertEquals(result2, 0xf0f0f0f0.toInt(), "Two int16s")
        if (result2 != null) {
            assertEquals(result2::class, Int::class, "Two Int16 type")
        }
    }

    @Test
    fun `invalid input`() {
        val result1 = o.invoke(listOf(0x0000))
        assertEquals(null, result1, "Single int16")
    }
}


