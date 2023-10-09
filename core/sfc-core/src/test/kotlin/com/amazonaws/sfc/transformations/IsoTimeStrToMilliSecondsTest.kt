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
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IsoTimeStrToMilliSecondsTest {

    @Test
    fun `create and validate`() {
        assertDoesNotThrow {
            IsoTimeStrToMilliSeconds.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "IsoTimeStrToMilliSeconds"
            }"""
        assertEquals(IsoTimeStrToMilliSeconds.create(), IsoTimeStrToMilliSeconds.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {

        val testValues = listOf<Pair<String, Double?>>(
            // target in Celsius, result in Fahrenheit
            Pair("PT1H02M3S", 3723000.0),
            Pair("PT1H02M3.456S", 3723456.0),
            Pair("BAD-FORMAT", null)
        )
        val o = IsoTimeStrToMilliSeconds.create()
        for (v in testValues) {
            val target: String? = v.first
            val result = o.invoke(target as Any)
            assertEquals(v.second, result)
        }
    }
}