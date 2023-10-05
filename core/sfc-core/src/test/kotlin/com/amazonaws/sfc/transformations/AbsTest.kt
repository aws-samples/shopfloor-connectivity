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

class AbsTest {

    @Test
    fun `create and validation`() {
        Assertions.assertDoesNotThrow {
            Abs.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "Abs"
            }"""
        assertEquals(Abs.create(), Abs.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic an return types`() {
        val testValues = listOf<Pair<Number?, Number?>>(
            Pair(1, 1),
            Pair(1.toByte(), 1.toByte()),
            Pair(1.toShort(), 1.toShort()),
            Pair(1.toLong(), 1.toLong()),
            Pair(1.0f, 1.0f),
            Pair(1.0, 1.0),
            Pair(-1, 1),
            Pair((-1).toByte(), 1.toByte()),
            Pair((-1).toShort(), 1.toShort()),
            Pair((-1).toLong(), 1.toLong()),
            Pair(-1.0f, 1.0f),
            Pair(null, null)
        )

        val o = Abs.create()
        for (v in testValues) {
            val target: Number? = v.first
            val result = o.apply(target)
            assertEquals(v.second, result, if (v.first != null) v.first!!::class.java.name else "null")
            if (result != null && target != null) assertEquals(target::class, result::class)
        }
    }
}