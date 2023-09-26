/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

class ParseNumberTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            ParseNumber.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "ParseParseNumber"
            }"""
        assertEquals(ParseNumber.create(), ParseNumber.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {
        assertEquals(1.0, ParseNumber.create().invoke("1.0"), "String to number")
        Assertions.assertNull(ParseNumber.create().invoke(""), "No valid number")
        assertEquals(1.0, ParseNumber().invoke("1"), "Valid int number")
    }
}