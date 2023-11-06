
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.reflect.InvocationTargetException

class TransformationsDeserializerTest {

    @TransformerOperator(["Test"])
    open class TestTransformationOperator : TransformationImpl<Nothing>() {
        companion object {

            fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<TestTransformationOperator>(o)
            fun create() = TestTransformationOperator()
        }

    }

    @TransformerOperator(["Test"])
    class TestTransformationOperator2 : TestTransformationOperator() {
        companion object {
            fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<TestTransformationOperator2>(o)
            fun create() = TestTransformationOperator2()
        }
    }

    init {
        TransformationsDeserializer.registerTransformation(TestTransformationOperator::class)
    }


    @Test
    fun `deserialize an operator`() {
        val json = """
            {
                "Operator": "Test"
            }"""
        val jsonObject = Gson().fromJson(json, JsonObject::class.java)

        assertNotNull(TransformationsDeserializer().deserialize(jsonObject, null, null))
    }

    @Test
    fun `deserializing unknown operator`() {
        val json = """
            {
                "Operator": "Unknown"
            }"""
        val jsonObject = Gson().fromJson(json, JsonObject::class.java)

        assertTrue(TransformationsDeserializer().deserialize(jsonObject, null, null) is InvalidTransformationOperator)
    }

    @Test
    fun `deserializing no operator name specified`() {
        val json = """
            {
            }"""
        val jsonObject = Gson().fromJson(json, JsonObject::class.java)

        assertTrue(TransformationsDeserializer().deserialize(jsonObject, null, null) is InvalidTransformationOperator)
    }

    @Test
    fun `deserialize an operator with failing create method`() {

        try {
            val json = """
            {
                "Operator": "Test"
            }"""
            mockkObject(TestTransformationOperator.Companion)
            every { TestTransformationOperator.fromJson(any()) } throws InvocationTargetException(Exception("Mocked Exception"))

            val jsonObject = Gson().fromJson(json, JsonObject::class.java)
            assertTrue(TransformationsDeserializer().deserialize(jsonObject, null, null) is InvalidTransformationOperator)
        } finally {
            unmockkObject(TestTransformationOperator.Companion)
        }
    }


    @Test
    fun `registering an operator with a name that is already used`() {

        TransformationsDeserializer.registerTransformation(TestTransformationOperator::class)

        assertThrows(TransformationException::class.java) {
            TransformationsDeserializer.registerTransformation(TestTransformationOperator2::class)
        }

    }
}