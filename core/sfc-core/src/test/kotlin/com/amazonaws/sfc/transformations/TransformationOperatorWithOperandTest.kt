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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TransformationOperatorWithOperandTest {

    class TestOperand(operand: Any) : TransformationImpl<Any>(operand) {
    }

    @Test
    fun `deserialize operand`() {
        val json = """
            {
                "Operand" : ""
            }"""

        val jso = Gson().fromJson(json, JsonObject::class.java)

        var op: TransformationOperator? = null
        assertDoesNotThrow {
            op = TransformationOperatorWithOperand.fromJson<TestOperand, Any>(jso) { o -> o.asString }
        }

        assertNotNull(op)
        assertTrue(op is TestOperand)

    }

    @Test
    fun `handle deserialization error`() {
        val json = """
            {
            }"""

        val jso = Gson().fromJson(json, JsonObject::class.java)

        assertThrows(TransformationException::class.java) {
            TransformationOperatorWithOperand.fromJson<TestOperand, Any>(jso) { o -> o.asString }
        }

    }

    @Test
    fun `deserialize numeric operand as string`() {
        val json = """
            {
                "Operand" : "1"
            }"""

        val jso = Gson().fromJson(json, JsonObject::class.java)
        val op: TestOperand? = TransformationOperatorNumericOperand.fromJson<TestOperand>(jso) as TestOperand?
        assertNotNull(op)
        assertEquals(1, op!!.operand)
    }

    @Test
    fun `deserialize numeric operand as hex`() {
        val json = """
            {
                "Operand" : 0xF
            }"""

        val jso = Gson().fromJson(json, JsonObject::class.java)
        val op: TestOperand? = TransformationOperatorNumericOperand.fromJson<TestOperand>(jso) as TestOperand?
        assertNotNull(op)
        assertEquals(0xF, op!!.operand)
    }

    @Test
    fun `deserialize numeric operand as hex string`() {
        val json = """
            {
                "Operand" : 0xF
            }"""

        val jso = Gson().fromJson(json, JsonObject::class.java)
        val op: TestOperand? = TransformationOperatorNumericOperand.fromJson<TestOperand>(jso) as TestOperand?
        assertNotNull(op)
        assertEquals(0xF, op!!.operand)
    }

    @Test
    fun `deserialize numeric operand as octal`() {
        val json = """
            {
                "Operand" : 012
            }"""

        val jso = Gson().fromJson(json, JsonObject::class.java)
        val op: TestOperand? = TransformationOperatorNumericOperand.fromJson<TestOperand>(jso) as TestOperand?
        assertNotNull(op)
        assertEquals(10, op!!.operand)
    }

    @Test
    fun `deserialize numeric operand as octal string`() {
        val json = """
            {
                "Operand" : 012
            }"""

        val jso = Gson().fromJson(json, JsonObject::class.java)
        val op: TestOperand? = TransformationOperatorNumericOperand.fromJson<TestOperand>(jso) as TestOperand?
        assertNotNull(op)
        assertEquals(10, op!!.operand)
    }

    @Test
    fun `throws exception for empty operand`() {
        val json = """
            {
                Operand : ""
            }"""

        val jso = Gson().fromJson(json, JsonObject::class.java)
        assertThrows(TransformationException::class.java) { TransformationOperatorNumericOperand.fromJson<TestOperand>(jso) as TestOperand? }
    }

    @Test
    fun `throws exception for non numeric operand`() {
        val json = """
            {
                Operand : "abc"
            }"""

        val jso = Gson().fromJson(json, JsonObject::class.java)
        assertThrows(TransformationException::class.java) { TransformationOperatorNumericOperand.fromJson<TestOperand>(jso) as TestOperand? }
    }
}