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