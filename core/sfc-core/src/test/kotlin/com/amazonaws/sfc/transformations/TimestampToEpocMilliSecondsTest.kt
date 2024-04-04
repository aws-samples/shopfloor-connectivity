package com.amazonaws.sfc.transformations


import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class TimestampToEpocMilliSecondsTest{


	@Test
	fun testTimestampToEpocMilliSeconds(){
		val timestampToEpocMilliSeconds = TimestampToEpocMilliSeconds()
		val result = timestampToEpocMilliSeconds.apply(Instant.ofEpochMilli(1514764800000))
		assertEquals(1514764800000, result)
	}

	@Test
	fun testTimestampToEpocMilliSecondsWithNull(){
		val timestampToEpocMilliSeconds = TimestampToEpocMilliSeconds()
		val result = timestampToEpocMilliSeconds.apply(null)
		assertEquals(null, result)
	}


	@Test
	fun `deserialize from json`() {
		val json = """
            {
                "Operator": "TimestampToEpocMilliSeconds"
            }"""
		assertEquals(TimestampToEpocMilliSeconds.create(), TimestampToEpocMilliSeconds.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
	}


}