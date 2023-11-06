
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UpperCaseTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            UpperCase.create().validate()
        }
    }

    @Test
    fun `deserialization from json`() {
        val json = """
            {
                "Operator": "UpperCase"
            }"""
        assertEquals(UpperCase.create(), UpperCase.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {
        val testString = "abcdef"
        assertEquals(testString.uppercase(), UpperCase.create().invoke(testString), "to Uppercase")
    }
}