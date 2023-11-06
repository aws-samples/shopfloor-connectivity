
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ParseNumberTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            ParseNumber.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "ParseParseNumber"
            }"""
        assertEquals(ParseNumber.create(), ParseNumber.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun `operator logic`() {
        assertEquals(1.0, ParseNumber.create().invoke("1.0"), "String to number")
        Assertions.assertNull(ParseNumber.create().invoke(""), "No valid number")
        assertEquals(1.0, ParseNumber().invoke("1"), "Valid int number")
    }
}