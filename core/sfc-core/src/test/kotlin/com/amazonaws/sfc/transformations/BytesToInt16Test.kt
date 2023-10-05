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

class BytesToInt16Test {

    val o = BytesToInt16.create()

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            BytesToInt16.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "BytesToInt16"
            }"""
        assertEquals(BytesToInt16.create(), BytesToInt16.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun logic() {
        val result2 = o.invoke(listOf(0xf0.toByte(), 0xf0.toByte()))
        assertEquals(result2, 0xf0f0.toShort(), "Two bytes")
        if (result2 != null) {
            assertEquals(result2::class, Short::class, "Two bytes type")
        }
    }

    @Test
    fun `invalid input logic`() {
        val result1 = o.invoke(listOf(0xf0.toByte()))
        assertEquals(null, result1, "Single byte")
    }
}