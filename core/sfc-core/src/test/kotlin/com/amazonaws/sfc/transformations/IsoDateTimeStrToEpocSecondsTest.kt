package com.amazonaws.sfc.transformations

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IsoDateTimeStrToEpocSecondsTest {


    @Test
    fun testIsoDateTimeStrToEpocSeconds() {
        val isoDateTimeStrToEpocSeconds = IsoDateTimeStrToEpocSeconds()
        val result = isoDateTimeStrToEpocSeconds.apply("1970-01-01T00:00:01.000Z")
        assertEquals(1, result)
    }

    @Test
    fun testIsoDateTimeStrToEpocSecondsWithNull() {
        val isoDateTimeStrToEpocSeconds = IsoDateTimeStrToEpocSeconds()
        val result = isoDateTimeStrToEpocSeconds.apply(null)
        assertNull(result)
    }

    @Test
     fun testIsoDateTimeStrToEpocSecondsWithIncompleteString() {
         assertThrows(java.time.format.DateTimeParseException::class.java){
             val isoDateTimeStrToEpocSeconds = IsoDateTimeStrToEpocSeconds()
             isoDateTimeStrToEpocSeconds.apply("1970-01-01")
         }
     }

    @Test
    fun testIsoDateTimeStrToEpocSecondsWithNullString() {
            val isoDateTimeStrToEpocSeconds = IsoDateTimeStrToEpocSeconds()
            val result = isoDateTimeStrToEpocSeconds.apply(null)
            assertNull(result)
    }

    @Test
    fun testIsoDateTimeStrToEpocSecondsWithInvalidString() {
        assertThrows(java.time.format.DateTimeParseException::class.java) {
            val isoDateTimeStrToEpocSeconds = IsoDateTimeStrToEpocSeconds()
            isoDateTimeStrToEpocSeconds.apply("invalid")
        }
    }

    @Test
    fun `deserialize from json`() {
        val json = """
            {
                "Operator": "IsoDateTimeStrToEpocSeconds"
            }"""
        assertEquals(IsoDateTimeStrToEpocSeconds.create(), IsoDateTimeStrToEpocSeconds.fromJson(Gson().fromJson(json, JsonObject::class.java)), "from $json")
    }

}