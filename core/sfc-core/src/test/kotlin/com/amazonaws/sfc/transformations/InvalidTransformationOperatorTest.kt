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

import com.amazonaws.sfc.config.ConfigurationException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class InvalidTransformationOperatorTest {

    private val operatorName = "Operator"
    private val message = "Message"
    private val item = "Item"

    @Test
    fun `can create instance with default operator name`() {
        val op = InvalidTransformationOperator(message = message, item = item)
        assertEquals("", op.operatorName)
        assertEquals(item, op.item)
        assertEquals(message, op.message)
    }

    @Test
    fun `validate should throw exception`() {

        assertThrows(ConfigurationException::class.java) {
            InvalidTransformationOperator(operatorName = "", message = message, item = item).validate()
        }
        assertThrows(ConfigurationException::class.java) {
            InvalidTransformationOperator(operatorName = operatorName, message = message, item = item).validate()
        }
    }


    @Test
    fun `test toString`() {
        val operatorName = "Operator"
        val message = "Message"
        val item = "Item"

        val op = InvalidTransformationOperator(operatorName = operatorName, message = message, item = item)
        assertEquals("InvalidTransformationOperator(operatorName=$operatorName, message='$message)", op.toString())
    }

    @Test
    fun `validate is always false`() {
        val op = InvalidTransformationOperator(operatorName = operatorName, message = message, item = item)
        assertFalse(op.validated)
        try {
            op.validate()
        } catch (_: ConfigurationException) {
            assertFalse(op.validated)
        }
    }

    @Test
    fun `invoke always returns null`() {
        val op = InvalidTransformationOperator(operatorName = operatorName, message = message, item = item)
        assertNull(op.invoke(""))
    }

    @Test
    fun `input and result types are always Nothing`() {
        val op = InvalidTransformationOperator(operatorName = operatorName, message = message, item = item)
        assertEquals(Nothing::class.java, op.inputType)
        assertEquals(Nothing::class.java, op.resultType)


    }
}