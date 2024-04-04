package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class EpocSecondsToTimestampTest{


	@Test
	fun testEpocSecondsToTimestamp() {
		assertEquals(Instant.ofEpochSecond(1483228800), EpocSecondsToTimestamp().apply(1483228800))
	}

	@Test
	fun testEpocSecondsToTimestampWithNull() {
		assertNull( EpocSecondsToTimestamp().apply(null))
	}


	@Test
	fun testEpocSecondsToTimestampWithZero() {
		assertEquals(Instant.ofEpochSecond(0), EpocSecondsToTimestamp().apply(0))
	}


	@Test
	fun `deserialize from json`() {
		val json = """
            {
                "Operator": "EpocSecondsToTimestamp"
            }"""
		assertEquals(EpocSecondsToTimestamp.create(), EpocSecondsToTimestamp.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
	}



}