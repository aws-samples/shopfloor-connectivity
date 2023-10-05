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

class NotTest {


    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            Not.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val jsonLong = """
            {
                "Operator": "Not"
            }"""

        val jsonShort = """
            {
                "Operator": "!"
            }"""

        for (json in listOf(jsonLong, jsonShort)) {
            assertEquals(Not.create(), Not.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
        }
    }

    @Test
    fun `operator logic`() {
        val testValues = listOf<Pair<Boolean?, Boolean?>>(
            // target, mod value, result
            Pair(false, true),
            Pair(true, false),
            Pair(null, null)
        )

        for (v in testValues) {
            val result = Not().apply(v.first)
            assertEquals(v.second, result, "${if (v.first != null) v.first!!::class.simpleName else "null"} ! ${v.second}")
            if (result != null && v.first != null) {
                assertEquals(v.first!!::class.java, result::class.java, "Check type")
            }
        }
    }
}