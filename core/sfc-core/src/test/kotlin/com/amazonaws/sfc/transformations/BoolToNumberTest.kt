
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BoolToNumberTest {

    @Test
    fun `create and validate`() {
        Assertions.assertDoesNotThrow {
            BoolToNumber.create().validate()
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "BoolToNumber"
            }"""
        assertEquals(BoolToNumber.create(), BoolToNumber.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

    @Test
    fun logic() {
        assertEquals(BoolToNumber.create().invoke(false), 0, "False to 0")
        assertEquals(BoolToNumber.create().invoke(true), 1, "True to 1")
    }
}