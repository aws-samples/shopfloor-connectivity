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

class ToLongTest {


    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            ToLong.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "ToLong"
            }"""
        assertEquals(ToLong(), ToLong.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Number>(1, 1.toByte(), 1.toShort(), 1.toShort(), 1.toLong(), 1.toLong(), 1.0f, 1.0)


        val o = ToLong.create()
        for (v in testValues) {
            val target: Number = v
            val result = o.invoke(target)
            assertEquals(result, v.toLong(), v::class.java.name)
            if (result != null) assertEquals(Long::class, result::class)
        }
    }
}