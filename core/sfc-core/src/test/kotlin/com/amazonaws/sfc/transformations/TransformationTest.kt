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

internal class TransformationTest {

    @Test
    fun `invoke invalid input type throws exception`() {

        val tr = listOf<TransformationOperator>(And.create(0), Or.create(0))
        assertThrows(TransformationException::class.java) {
            val result = tr.invoke(target = "", throwsException = true)
            assertNull(result)
        }
    }

    @Test
    fun `invoke invalid type return null`() {
        val tr = listOf<TransformationOperator>(And.create(0), Or.create(0))

        assertDoesNotThrow {
            val result = tr.invoke(target = "", throwsException = false)
            assertNull(result)
        }

    }

    @Test fun `invoke on single value`() {
        val tr = listOf<TransformationOperator>(And.create(0), Or.create(0))
        assertDoesNotThrow {
            tr.invoke(target = 100, throwsException = true)
        }
    }

    @Test fun `invoke on array value`() {
        val tr = listOf<TransformationOperator>(And.create(0), Or.create(0))
        assertDoesNotThrow {
            tr.invoke(target = listOf(1, 2, 3), throwsException = true)
        }
    }

    @Test fun `invoke on 2D array value`() {
        val tr = listOf<TransformationOperator>(And.create(0), Or.create(0))
        assertDoesNotThrow {
            tr.invoke(target = listOf(listOf(100, 101, 102), listOf(200, 201, 202)), throwsException = true)
        }
    }


    @Test fun `invoke on indexed value`() {
        val tr = listOf<TransformationOperator>(AtIndex.create(0), AtIndex.create(0))
        assertDoesNotThrow {
            tr.invoke(target = listOf(listOf(0), listOf(1)), throwsException = true)
        }
    }

    @Test fun `invoke on struct value with list member`() {
        val tr = listOf<TransformationOperator>(Query.create("@.A"), AtIndex.create(1))
        assertDoesNotThrow {
            tr.invoke(target = listOf(mapOf("A" to listOf(0, 1))), throwsException = true)
        }
    }

    @Test fun `invoke on indexed struct value`() {
        val tr = listOf<TransformationOperator>(AtIndex.create(0), Query.create("@.A"))
        assertDoesNotThrow {
            val aa = tr.invoke(target = listOf(mapOf("A" to 0)), throwsException = true)
            print(aa)
        }
    }


    @Test
    fun `input type`() {
        val tr = listOf<TransformationOperator>(And.create(0), Or.create(0))
        assertEquals(Number::class.java, tr.inputType, "Get input type")
    }

    @Test
    fun `output type`() {
        val tr = listOf<TransformationOperator>(And.create(0), Or.create(0))
        assertEquals(Number::class.java, tr.resultType, "Get result type")
    }

    @Test
    fun `input value type`() {
        val tr = listOf<TransformationOperator>(And.create(0), Or.create(0))
        assertTrue(tr.isValidInput(1), "Int is valid input")
        assertTrue(tr.isValidInput(1L), "Long is valid input")
        assertFalse(tr.isValidInput(""), "String is not a valid input")
        assertFalse(tr.isValidInput(false), "Boolean is not a valid input")
    }

    @Test
    fun `validate input type`() {
        val tr = listOf<TransformationOperator>(And.create(0), Or.create(0))
        assertTrue(tr.isValidInputType(Int::class), "Int is valid input type")
        assertTrue(tr.isValidInputType(Long::class), "Long is valid input type")
        assertFalse(tr.isValidInputType(String::class), "String is not a valid input type")
        assertFalse(tr.isValidInputType(Boolean::class), "Boolean is not a valid input type")
    }

    @Test
    fun `invalid operator type`() {

        val invalidThreeOperators = listOf(And.create(0), LowerCase(), And.create(0))
        val errType = invalidThreeOperators.validateOperatorTypes()
        assertNotNull(errType)
        assertEquals(1, errType?.Order ?: -1, "Output type of first operator not valid for second operator")
    }

    @Test
    fun `invalid transformation type`() {

        val invalidOneOperator = listOf(InvalidTransformationOperator("", "", ""))
        assertNotNull(invalidOneOperator.validateOperatorTypes())

        val invalidFirstOperator = listOf(InvalidTransformationOperator("", "", ""), And.create(0))
        val errFirst = invalidFirstOperator.validateOperatorTypes()
        assertNotNull(errFirst)
        assertEquals(1, errFirst?.Order ?: -1, "First operator invalid ")

        val invalidMiddleOperator = listOf(And.create(0), InvalidTransformationOperator("", "", ""), And.create(0))
        val errSecond = invalidMiddleOperator.validateOperatorTypes()
        assertNotNull(errSecond, "Second operator is invalid")
        assertEquals(2, errSecond?.Order ?: -1, "Second operator invalid ")

    }

    @Test
    fun `valid transformation`() {
        val validMultipleOperators = listOf(And.create(0), Or.create(0))
        assertNull(validMultipleOperators.validateOperatorTypes())

        val validMultipleOperatorsFirstAny = listOf(Query.create("@"), Or.create(0))
        assertNull(validMultipleOperatorsFirstAny.validateOperatorTypes())

        val validMultipleOperatorsSecondAny = listOf(Or.create(0), Query.create("@"))
        assertNull(validMultipleOperatorsSecondAny.validateOperatorTypes())
    }

    @Test
    fun `single operator`() {
        val validSingleOperator = listOf(And.create(0))
        assertNull(validSingleOperator.validateOperatorTypes())
    }

    @Test
    fun `empty transformation`() {
        val validNoOperator = listOf<TransformationOperator>()
        assertNull(validNoOperator.validateOperatorTypes())
    }
}