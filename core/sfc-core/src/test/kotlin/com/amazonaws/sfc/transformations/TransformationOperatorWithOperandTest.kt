
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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