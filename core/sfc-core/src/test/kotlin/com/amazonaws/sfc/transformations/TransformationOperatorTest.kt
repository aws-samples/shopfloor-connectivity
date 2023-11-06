
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations


import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test


internal class TransformationOperatorTest {

    @Test
    fun testToString() {

        val s = And(0).toString()
        assertNotEquals("", s, "Empty output string")
    }

    @Test
    fun getInputTypeTest() {
        val o = And(0)
        assertEquals(o.inputType, Number::class.java, "Expected input type")
    }

    @Test
    fun getOutputTypeTest() {
        val o = And(0)
        assertEquals(o.resultType, Number::class.java, "Expected output type")
    }


    @Test
    fun applyOperatorTest() {

        val o = And(0)

        assertDoesNotThrow({
            val out = o.invoke(target = 1, checkType = true, throwsException = false)
            assertNotNull(out, "Operator applied")
        }, "Success, checked, no logging")

        assertDoesNotThrow({
            val out = o.invoke(target = 1, checkType = false, throwsException = false)
            assertNotNull(out, "Operator applied")
        }, "Success, unchecked no logging")

        assertThrows(TransformationException::class.java, {
            o.invoke(target = "", checkType = false, throwsException = true)
        }, "Failed, unchecked parameter, exception raised, no logging")

        assertThrows(TransformationException::class.java, {
            o.invoke(target = "")
        }, "Failed, unchecked parameter, exception raised, no logging, default parameters")

        assertThrows(TransformationException::class.java, {
            val logged = ""
            o.invoke(target = "", checkType = false, throwsException = false)
            assertNotEquals(logged, "", "No exception, error logged")
        }, "Failed, unchecked, exception raised, error logged")

        assertThrows(TransformationException::class.java, {
            o.invoke(target = "", checkType = true, throwsException = true)
        }, "Failed, checked, exception raised")

        assertThrows(TransformationException::class.java, {
            val logged = ""
            o.invoke(target = "", checkType = true, throwsException = true)
            assertNotEquals(logged, "", "Exception, error logged")
        }, "Failed, checked, exception raised, error logged")


    }
}


