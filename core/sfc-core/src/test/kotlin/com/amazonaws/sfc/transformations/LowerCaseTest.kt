
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LowerCaseTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            LowerCase.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "LowerCase"
            }"""
        assertEquals(LowerCase.create(), LowerCase.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {
        val testString = "ABCDEF"
        assertEquals(testString.lowercase(), LowerCase.create().invoke(testString), "to Lowercase")
    }
}