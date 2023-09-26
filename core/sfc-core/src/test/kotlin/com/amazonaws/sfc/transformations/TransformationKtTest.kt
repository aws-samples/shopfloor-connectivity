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

import com.amazonaws.sfc.log.ConsoleLogWriter
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.log.Logger
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TransformationKtTest {

    private val first = Plus(1)
    private val last = Minus(1)
    private val transformation = listOf<TransformationOperator>(
        first, last
    )

    @Test
    fun `invoke single value`() {
        assertNotNull(transformation(1))
        assertNotNull(transformation(1, valueName = "single", throwsException = false))
    }

    @Test
    fun `apply single value`() {

        assertNotNull(transformation.applyOnSingleValue(target = 1))

        val logger = Logger(LogLevel.INFO,
            source = null,
            sourceFilter = null,
            secretNames = null,
            writer = ConsoleLogWriter())

        assertNotNull(transformation.applyOnSingleValue(
            target = 1,
            valueName = "single",
            checkType = true,
            throwsException = false,
            logger = logger
        ))
    }


    @Test
    fun `invoke on list value`() {
        assertNotNull(transformation(listOf(1, 2, 3)))
    }

    @Test
    fun `invoke on nested lists value`() {
        assertNotNull(transformation(listOf(listOf(1, 2), listOf(3, 4))))
    }

    @Test
    fun `invoke null value`() {
        assertNull(transformation(null))
    }


    @Test
    fun getInputType() {
        assertEquals(first.resultType, transformation.resultType)
        assertNull(emptyList<TransformationOperator>().resultType)
    }


    @Test
    fun getResultType() {
        assertEquals(last.resultType, transformation.resultType)
        assertNull(emptyList<TransformationOperator>().resultType)
    }

    @Test
    fun `valid input type`() {
        assertTrue(transformation.isValidInputType(Int::class))
    }

    @Test
    fun `invalid input type`() {
        assertFalse(transformation.isValidInputType(String::class))
    }

    @Test
    fun `valid input value`() {
        assertTrue(transformation.isValidInput(1))
    }

    @Test
    fun `invalid input value`() {
        assertFalse(transformation.isValidInput(""))
    }

    @Test
    fun `validate valid transformation`() {
        assertNull(transformation.validateOperatorTypes())
    }


    @Test
    fun `validate empty transformation`() {
        assertNull(emptyList<TransformationOperator>().validateOperatorTypes())
    }

    @Test
    fun `validate transformation with single invalid operator`() {
        val error = listOf<TransformationOperator>(
            InvalidTransformationOperator(message = "", item = ""),
        ).validateOperatorTypes()

        assertNotNull(error)
        assertEquals(0, error?.Order)
    }


    @Test
    fun `validate transformation starting with an invalid operator`() {
        val error = listOf<TransformationOperator>(
            InvalidTransformationOperator(message = "", item = ""),
            Minus(1)
        ).validateOperatorTypes()

        assertNotNull(error)
        assertEquals(1, error?.Order)
    }


    @Test
    fun `validate transformation containing an invalid operator`() {
        val error = listOf<TransformationOperator>(
            Plus(1),
            InvalidTransformationOperator(message = "", item = ""),
            Minus(1)
        ).validateOperatorTypes()

        assertNotNull(error)
        assertEquals(2, error?.Order)
    }


    @Test
    fun `validate transformation ending with an invalid operator`() {
        val error = listOf<TransformationOperator>(
            Minus(1),
            InvalidTransformationOperator(message = "", item = "")
        ).validateOperatorTypes()

        assertNotNull(error)
        assertEquals(2, error?.Order)
    }

    @Test
    fun `validate invalid transformations first  set`() {
        val error = listOf<TransformationOperator>(
            Plus(1),
            UpperCase(),
            Minus(1)
        ).validateOperatorTypes()

        assertNotNull(error)
        assertEquals(1, error?.Order)

    }

    @Test
    fun `validate invalid transformations for middle set`() {
        val error = listOf<TransformationOperator>(
            Plus(1),
            Minus(1),
            UpperCase(),
            Plus(1)
        ).validateOperatorTypes()

        assertNotNull(error)
        assertEquals(2, error?.Order)

    }


    @Test
    fun `validate invalid transformations for last set`() {
        val error = listOf<TransformationOperator>(
            Plus(1),
            Minus(1),
            Plus(1),
            UpperCase(),
        ).validateOperatorTypes()

        assertNotNull(error)
        assertEquals(3, error?.Order)

    }
}