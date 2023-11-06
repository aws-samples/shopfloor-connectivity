
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StrRangeTest {

    @Test
    fun `can create and validation`() {
        assertDoesNotThrow {
            StrRange.create(1, 2).validate()
            StrRange.create().validate()
            StrRange.create(start = 1).validate()
            StrRange.create(end = -1).validate()
        }

        assertDoesNotThrow {
            StrRange.create().validate()
        }
    }

    @Test
    fun `validate invalid range`() {
        Assertions.assertThrows(ConfigurationException::class.java) {
            Range.create().validate()
        }

        Assertions.assertThrows(ConfigurationException::class.java) {
            Range.create(minValue = 1).validate()
        }

        Assertions.assertThrows(ConfigurationException::class.java) {
            Range.create(maxValue = 1).validate()
        }

        Assertions.assertThrows(ConfigurationException::class.java) {
            Range.create(minValue = 1, maxValue = 1).validate()
        }

        Assertions.assertThrows(ConfigurationException::class.java) {
            Range.create(minValue = 1, maxValue = 0).validate()
        }
    }


    @Test
    fun testToString() {
        assertEquals("(Start=1, End=2)", StrRange.create(start = 1, end = 2).toString())
    }

    @Test
    fun testEquals() {

        val range1 = StrRange.create(start = 1, end = 2)
        val range2 = StrRange.create(start = 1, end = 2)
        val range3 = StrRange.create(start = 2, end = 3)
        val testValues = listOf<Triple<StrRange, Any?, Boolean?>>(
            // target, mod value, result
            Triple(range1, range1, true),
            Triple(range1, range2, true),
            Triple(range2, range1, true),
            Triple(range1, range3, false),
            Triple(range3, range1, false),
            Triple(range1, Object(), false),
            Triple(range1, null, false),
        )

        for (v in testValues) {
            assertEquals(v.third, v.first == v.second, "${v.first == v.second}")
        }
    }

    @Test
    fun testHashCode() {
        assertEquals(33, StrRange.create(start = 1, end = 2).hashCode())
    }
}