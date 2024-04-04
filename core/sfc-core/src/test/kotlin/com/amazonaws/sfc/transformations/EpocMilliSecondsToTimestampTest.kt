package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class EpocMilliSecondsToTimestampTest{


	@Test
	fun testEpocMilliSecondsToTimestamp() {
		assertEquals("2017-01-01T00:00:00Z", EpocMilliSecondsToTimestamp().apply(1483228800000).toString())
	}

	@Test
	fun testEpocMilliSecondsToTimestampWithNull() {
		assertNull( EpocMilliSecondsToTimestamp().apply(null))
	}


	@Test
	fun testEpocMilliSecondsToTimestampWithZero() {
		assertEquals(Instant.ofEpochMilli(0), EpocMilliSecondsToTimestamp().apply(0))
	}


	@Test
	fun `deserialize from json`() {
		val json = """
            {
                "Operator": "EpocMilliSecondsToTimestamp"
            }"""
		assertEquals(EpocMilliSecondsToTimestamp.create(), EpocMilliSecondsToTimestamp.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
	}



}