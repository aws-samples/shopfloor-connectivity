package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IsoDateTimeStrToEpocMilliSecondsTest {


    @Test
    fun testIsoDateTimeStrToEpocMilliSeconds() {
        val isoDateTimeStrToEpocMilliSeconds = IsoDateTimeStrToEpocMilliSeconds()
        val result = isoDateTimeStrToEpocMilliSeconds.apply("1970-01-01T00:00:00.123Z")
        assertEquals(123, result)
    }

    @Test
    fun testIsoDateTimeStrToEpocMilliSecondsWithNull() {
        val isoDateTimeStrToEpocMilliSeconds = IsoDateTimeStrToEpocMilliSeconds()
        val result = isoDateTimeStrToEpocMilliSeconds.apply(null)
        assertNull(result)
    }

    @Test
     fun testIsoDateTimeStrToEpocMilliSecondsWithIncompleteString() {
         assertThrows(java.time.format.DateTimeParseException::class.java){
             val isoDateTimeStrToEpocMilliSeconds = IsoDateTimeStrToEpocMilliSeconds()
             isoDateTimeStrToEpocMilliSeconds.apply("1970-01-01")
         }
     }

    @Test
    fun testIsoDateTimeStrToEpocMilliSecondsWithNullString() {
            val isoDateTimeStrToEpocMilliSeconds = IsoDateTimeStrToEpocMilliSeconds()
            val result = isoDateTimeStrToEpocMilliSeconds.apply(null)
            assertNull(result)
    }

    @Test
    fun testIsoDateTimeStrToEpocMilliSecondsWithInvalidString() {
        assertThrows(java.time.format.DateTimeParseException::class.java) {
            val isoDateTimeStrToEpocMilliSeconds = IsoDateTimeStrToEpocMilliSeconds()
            isoDateTimeStrToEpocMilliSeconds.apply("invalid")
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "IsoDateTimeStrToEpocMilliSeconds"
            }"""
        assertEquals(IsoDateTimeStrToEpocMilliSeconds.create(), IsoDateTimeStrToEpocMilliSeconds.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

}