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

@Suppress("unused") class AtIndexTest {

    private val testData = arrayListOf(0, 1, 2, 3)

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            AtIndex.create(1).validate()
        }
    }

    @Test
    fun `deserialize from json`() {

        val jsonFull = """
            {
                "Operator": "AtIndex", 
                "Operand" : 0
            }"""

        val jsonShort = """
            {
                "Operator": "[]]", 
                "Operand" : 0
            }"""

        for (json in listOf(jsonFull, jsonShort)) {
            assertEquals(AtIndex.create(0), AtIndex.fromJson(Gson().fromJson(json, JsonObject::class.java)), "deserialize from $json")
        }
    }

    @Test
    fun `empty result logic`() {
        val oe = AtIndex.create(0)
        assertEquals(oe.invoke(emptyList<Int>()), null, "Empty set")
    }

    @Test
    fun `out of range logic`() {
        val ob = AtIndex.create(-100)
        assertEquals(ob.invoke(testData), null, "Out of range, before")

        val oa = AtIndex.create(testData.size + 1)
        assertEquals(oa.invoke(testData), null, "Out of range, after")
    }

    @Test
    fun `last entry logic`() {
        val ol = AtIndex.create(testData.size - 1)
        assertEquals(ol.invoke(testData), testData[testData.size - 1], "Last value")
        val oln = AtIndex.create(-100)
        assertEquals(oln.invoke(testData), null, "Last Value with neg index")
    }

    @Test
    fun `first entry logic`() {
        val o1 = AtIndex.create(1)
        assertEquals(o1.invoke(testData), testData[1], "In Range")

        val of = AtIndex.create(0)
        assertEquals(of.invoke(testData), testData[0], "First value")
    }
}