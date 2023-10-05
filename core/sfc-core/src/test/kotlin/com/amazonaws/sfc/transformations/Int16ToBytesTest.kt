/*

Copyright (c) 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved. 
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

class Int16ToBytesTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Fahrenheit.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
            "Operator": "Int16ToBytes"
            }"""
        assertEquals(Int16ToBytes.create(), Int16ToBytes.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {
        val o = Int16ToBytes()
        val result = o.invoke(0x0ff0)
        assertEquals(listOf(0x0f.toByte(), 0xf0.toByte()), result, "Split 16 bit int into bytes")
    }
}