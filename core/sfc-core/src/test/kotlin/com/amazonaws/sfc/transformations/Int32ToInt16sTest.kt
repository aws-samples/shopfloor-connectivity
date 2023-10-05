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

class Int32ToInt16sTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Int32ToInt16s.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
             "Operator": "Int32ToInt16s"
            }"""
        assertEquals(Int32ToInt16s.create(), Int32ToInt16s.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {
        val o = Int32ToInt16s.create()
        val result = o.invoke(0x0ff00ff0)
        assertEquals(listOf(0x0ff0.toShort(), 0x0ff0.toShort()), result, "Split 32 bits int into 2 16 bits")
    }
}