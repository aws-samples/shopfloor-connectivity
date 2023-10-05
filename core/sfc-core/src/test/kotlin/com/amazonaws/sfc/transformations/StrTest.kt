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

class StrTest {

    @Test
    fun `create and Validate`() {
        Assertions.assertDoesNotThrow {
            Str.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "Str"
            }"""
        assertEquals(Str.create(), Str.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Number>(
            1, 1.toByte(),
            1.toShort(),
            1.toLong(),
            1.0,
            1.0.toFloat())

        val o = Str.create()
        for (v in testValues) {
            val s = o.invoke(v) as String?
            Assertions.assertNotNull(s, "${v::class.java.name} not null")
            if (s != null) Assertions.assertTrue(s.isNotEmpty(), "Not Empty ${v::class.java.name}")
        }
    }
}