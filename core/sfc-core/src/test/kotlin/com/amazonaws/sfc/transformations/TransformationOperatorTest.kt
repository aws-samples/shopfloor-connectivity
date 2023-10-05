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


