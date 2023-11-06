
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.system

import kotlinx.coroutines.delay
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.*
import java.util.*

/**
 * Helper to avoid repeated code and get system datetime in a consistent way
 */
object DateTime {
    /**
     * Gets the current local time as an instance of the Instant class
     * @return Instant
     */
    fun systemDateTime(): Instant = Instant.now()

    /**
     * Gets the current local time as an instance of a Calendar class
     * @return Calendar
     */
    fun systemCalendar(): Calendar = Calendar.getInstance()

    /**
     * Gets the current UTC time as an instance of the Instant class
     * @return Instant
     */
    fun systemDateTimeUTC(): Instant = Instant.now(Clock.systemUTC())

    /**
     * Gets the current UTC time as an instance of a Calendar class
     * @return Calendar
     */
    fun systemCalendarUTC(): Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

    fun systemDateUTC(): Date {
        val now = LocalDate.now()
        val zonedDateTime = ZonedDateTime.of(now, LocalTime.MIDNIGHT, ZoneOffset.UTC)
        val instant = zonedDateTime.toInstant()
        return Date.from(instant)
    }

    fun dateFromString(s: String): Date? =
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'")
            dateFormat.parse(s)
        } catch (ex: ParseException) {
            null
        }

    fun Date.add(p: Period): Date {
        val zoneId = ZoneId.systemDefault()
        val localDate = this.toInstant().atZone(zoneId).toLocalDate()
        val newLocalDate = localDate.plus(p)
        return Date.from(newLocalDate.atStartOfDay(zoneId).toInstant())
    }

    suspend fun delayUntilNextMidnightUTC() {
        val now = Instant.now()
        val nextMidnightUTC = ZonedDateTime.now(ZoneOffset.UTC).with(LocalTime.MIDNIGHT).plusDays(1).toInstant()

        val delaySeconds = java.time.Duration.between(now, nextMidnightUTC).seconds
        delay(delaySeconds * 1000)
    }


}