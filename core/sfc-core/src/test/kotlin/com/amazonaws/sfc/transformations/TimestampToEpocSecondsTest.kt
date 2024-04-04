package com.amazonaws.sfc.transformations


import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class TimestampToEpocSecondsTest{


	@Test
	fun testTimestampToEpocSeconds(){
		val timestampToEpocSeconds = TimestampToEpocSeconds()
		val result = timestampToEpocSeconds.apply(Instant.ofEpochSecond(1514764800))
		assertEquals(1514764800, result)
	}

	@Test
	fun testTimestampToEpocSecondsWithNull(){
		val timestampToEpocSeconds = TimestampToEpocSeconds()
		val result = timestampToEpocSeconds.apply(null)
		assertEquals(null, result)
	}


	@Test
	fun `deserialize from json`() {
		val json = """
            {
                "Operator": "TimestampToEpocSeconds"
            }"""
		assertEquals(TimestampToEpocSeconds.create(), TimestampToEpocSeconds.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
	}


}