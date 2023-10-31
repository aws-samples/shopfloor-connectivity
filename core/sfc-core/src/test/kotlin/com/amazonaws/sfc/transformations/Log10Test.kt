/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, Log10ress or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Log10Test {

    @Test
    fun `create and validation`() {
        Assertions.assertDoesNotThrow {
            Log10.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {

        val json = """
            {
               "Operator": "Log10"
            }"""
        assertEquals(Log10.create(), Log10.fromJson(Gson().fromJson(json, JsonObject::class.java)))
    }

    @Test
    fun `operator logic and return types`() {
        val testValues = listOf<Pair<Number?, Number?>>(
            Pair(0.0.toFloat(), kotlin.math.log10(0.0.toFloat())),
            Pair(1.0.toFloat(), kotlin.math.log10(1.0.toFloat())),
            Pair(0.0, kotlin.math.log10(0.0)),
            Pair(1.0, kotlin.math.log10(1.0)),
            Pair(0, kotlin.math.log10(0.0)),
            Pair(1, kotlin.math.log10(1.0)),
            Pair(null, null),
            Pair(Double.NaN, null),
            Pair(-1, null))


        val log10 = Log10.create()
        for (v in testValues) {
            val target: Number? = v.first
            val result = log10.apply(target)
            assertEquals(v.second, result, if (v.first != null) v.first!!::class.java.name else "null")
            if (result != null) {
                assertTrue(result::class == if (target is Float) Float::class else Double::class)
            }
        }
    }

}
