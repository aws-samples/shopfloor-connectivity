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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TransformationOperatorNoOperandTest {

    class TestTransformation : TransformationImpl<Nothing>() {
    }

    @Test
    fun `can create instance`() {
        assertDoesNotThrow {
            TransformationOperatorNoOperand()
        }
    }

    @Test
    fun deserialize() {
        val json = """
            {
            }"""

        val jso = Gson().fromJson(json, JsonObject::class.java)
        val op: TestTransformation? = TransformationOperatorNoOperand.fromJson<TestTransformation>(jso) as TestTransformation?
        assertNotNull(op)
    }


    @Test
    fun `should return Invalid operator when using an operand`() {
        val json = """
            {
                Operand : "anything"
            }"""

        val jso = Gson().fromJson(json, JsonObject::class.java)
        val op = TransformationOperatorNoOperand.fromJson<TestTransformation>(jso)
        assertNotNull(op)
        assertTrue(op is InvalidTransformationOperator)
    }

    @Test
    fun `should fail if deserialize an operator without the required parameterless constructor`() {
        val json = """
            {
            }"""

        val jso = Gson().fromJson(json, JsonObject::class.java)
        val op = TransformationOperatorNoOperand.fromJson<InvalidTransformationOperator>(jso)
        assertNotNull(op)
        assertTrue(op is InvalidTransformationOperator)
    }

}