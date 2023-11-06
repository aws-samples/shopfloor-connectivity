
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.log.LogWriter
import com.amazonaws.sfc.log.Logger
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TransformerImplTest {

    // Test classes
    class NoOperandTest : TransformationImpl<Nothing>() {

        @TransformerMethod
        fun apply(target: Any?): Any? = target

        override fun validate() {
            if (validated) return
            validated = true
        }

        companion object {
            fun create() = NoOperandTest()
        }
    }

    class NoApplyMethodTest : TransformationImpl<Nothing>() {
        companion object {
            fun create() = NoOperandTest()
        }
    }


    @Test
    fun `invoke with invalid input type should throw exception`() {

        val transformation = spyk<NoOperandTest>()
        every { transformation.inputType } returns String::class.java

        assertThrows(TransformationException::class.java) {
            transformation.invoke(1, "IntButShouldBeString", checkType = true, throwsException = true, logger = null)
        }
    }

    @Test
    fun `invoke with invalid input type should return null`() {

        val transformation = spyk<NoOperandTest>(recordPrivateCalls = true)
        every { transformation.inputType } returns String::class.java

        assertNull(
            transformation.invoke(1, "IntButShouldBeString", checkType = true, throwsException = false, logger = null)
        )

    }


    @Test
    fun `invoke method throwing exception being handled`() {

        val transformation = spyk<NoOperandTest>(recordPrivateCalls = true)
        every { transformation.apply(any()) } throws Exception("Mocked exception")

        assertThrows(TransformationException::class.java) {
            transformation.invoke(1, "Int", checkType = true, throwsException = false, logger = null)
        }
    }


    @Test
    fun `invoking an operand without apply method should throw exception`() {
        assertThrows(TransformationException::class.java) {
            val op = NoApplyMethodTest()
            op.invoke(1)

        }
    }

    @Test
    fun `return type must be equal to input type`() {

        val testValues = listOf(
            1.toByte(),
            1.toShort(),
            1,
            1.toLong(),
            1.toUByte(),
            1.toUShort(),
            1.toUInt(),
            1.toULong(),
            1.0,
            1.0F
        )


        val operand = NoOperandTest()
        for (v in testValues) {

            val result = operand(v)
            assertEquals(v::class, result!!::class, v::class.simpleName)
        }
    }


    @Test
    fun `can create and validate`() {

        assertDoesNotThrow {
            val op = NoOperandTest()
            op.validate()
        }

    }


    @Test
    fun `invoke method with trace output`() {

        var loggedTraceMessage = ""
        val message = slot<String>()

        val logWriter = mockk<LogWriter>(relaxed = true)
        every { logWriter.write(any(), any(), message = capture(message), source = any()) } answers {
            loggedTraceMessage += message.captured
        }

        val logger = spyk(Logger(level = LogLevel.TRACE, writer = logWriter))

        val transformation = NoOperandTest()

        transformation.invoke(1, "Int", checkType = true, throwsException = false, logger = logger)
        verify(atLeast = 1) { logWriter.write(LogLevel.TRACE, any(), source = any(), message = any()) }
        assertTrue(loggedTraceMessage.isNotEmpty())

    }

    @Test
    fun `test toString`() {
        assertEquals("NoOperandTest()", NoOperandTest().toString())
    }

    @Test
    fun `test type input and result type properties`() {
        val op = NoOperandTest()
        assertEquals(op.inputType, op.resultType)
    }

    @Test
    fun `test checking for numeric types`() {

        val testValues = listOf(
            1.toByte() to true,
            1.toShort() to true,
            1 to true,
            1.toLong() to true,
            1.toUByte() to true,
            1.toUShort() to true,
            1.toUInt() to true,
            1.toULong() to true,
            1.0 to true,
            1.0F to true,
            null to false,
            "" to false
        )

        val op = NoOperandTest()
        for (v in testValues) {
            assertEquals(op.isNumeric(if (v.first != null) v.first!!::class else null), v.second)
        }
    }

    @Test
    fun `test equality`() {

        val op1 = NoOperandTest()
        val op2 = NoOperandTest()
        val op3 = NoApplyMethodTest

        assertTrue(op1 == op2)
        assertFalse(op1.equals(op3))
    }


}
