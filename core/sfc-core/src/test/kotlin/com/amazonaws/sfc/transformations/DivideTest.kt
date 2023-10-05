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

import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DivideTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Divide.create(1).validate()
        }

        Assertions.assertThrows(ConfigurationException::class.java) {
            Divide.create(0).validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val jsonLong = """
            {
                "Operator": "Divide", 
                "Operand" : 1
            }"""

        val jsonShort = """
            {
                "Operator": "/",
                "Operand" : 1
            }"""

        for (json in listOf(jsonLong, jsonShort)) {
            assertEquals(Divide.create(1), Divide.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
        }
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Triple<Number, Number, Number?>>(
            // target, and multiplier, result
            Triple(6, 2, 3.0),
            Triple(5, 2, 2.5),
            Triple(6.toByte(), 2.toByte(), 3.0),
            Triple(5.toByte(), 2.toByte(), 2.5),
            Triple(6.toShort(), 2.toShort(), 3.0),
            Triple(5.toShort(), 2.toShort(), 2.5),
            Triple(6.toLong(), 2.toLong(), 3.0),
            Triple(5.toLong(), 2.toLong(), 2.5),
            Triple(6.0, 2.0, 3.0),
            Triple(5.0, 2.0, 2.5),
            Triple(6.0.toFloat(), 2.0.toFloat(), 3.0.toFloat()),
            Triple(5.0.toFloat(), 2.0.toFloat(), 2.5.toFloat()),
            Triple(2, 0, null),
            Triple(Double.MAX_VALUE, 0.5, null)
        )
        for (v in testValues) {
            val o = Divide.create(v.second)
            val result = o.invoke(v.first)
            assertEquals(v.third, result, v.first::class.java.name + " " + v.first + "/" + v.second)
        }
    }
}