
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RangeTest {

    @Test
    fun `can create and validation`() {
        assertDoesNotThrow {
            Range.create(1, 2).validate()
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
        assertEquals("[1..2]", Range.create(minValue = 1, maxValue = 2).toString())
    }

    @Test
    fun testEquals() {

        val range1 = Range.create(minValue = 1, maxValue = 2)
        val range2 = Range.create(minValue = 1, maxValue = 2)
        val range3 = Range.create(minValue = 2, maxValue = 3)
        val testValues = listOf<Triple<Range, Any?, Boolean?>>(
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
        assertEquals(33, Range.create(minValue = 1, maxValue = 2).hashCode())
    }
}