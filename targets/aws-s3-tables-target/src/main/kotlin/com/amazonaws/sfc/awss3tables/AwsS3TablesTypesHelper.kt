// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.awss3tables

import org.apache.iceberg.types.Type
import org.apache.iceberg.types.Types
import org.apache.iceberg.util.DateTimeUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.time.*
import java.time.format.DateTimeParseException
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt

object AwsS3TablesTypesHelper {

    fun Type.from(value: Any?): Any? = when (this) {
        is Types.BooleanType -> toBoolean(value)
        is Types.IntegerType -> toInteger(value)
        is Types.LongType -> toLong(value)
        is Types.FloatType -> toFloat(value)
        is Types.DoubleType -> toDouble(value)
        is Types.DateType -> toDate(value)
        is Types.TimeType -> toTime(value)
        is Types.TimestampType -> if (this.shouldAdjustToUTC()) toTimestampWithZone(value) else toTimestampWithoutZone(value)
        is Types.StringType -> toString(value)
        is Types.UUIDType -> toUUID(value)
        is Types.BinaryType -> toBinary(value)
        is Types.FixedType -> toFixed(value, this.length())
        is Types.DecimalType -> toDecimal(value, this.precision(), this.scale())
        is Types.ListType -> toList(this.elementType(), value)
        is Types.MapType -> toMap(this.keyType(), this.valueType(), value)
        else -> null
    }


    fun toBoolean(value: Any?): Boolean? {

        fun intToBoolean(value: Int): Boolean? {
            return when (value) {
                0 -> false
                1 -> true
                else -> null
            }
        }

        return when (value) {
            is Boolean -> value

            is Char -> when (value) {
                '0' -> true
                '1' -> false
                else -> null
            }

            is String -> when (value.lowercase()) {
                "true", "1" -> true
                "false", "0" -> false
                else -> null
            }

            is Byte -> intToBoolean(value.toInt())
            is UByte -> intToBoolean(value.toInt())
            is Short -> intToBoolean(value.toInt())
            is UShort -> intToBoolean(value.toInt())
            is Int -> intToBoolean(value)
            is UInt -> intToBoolean(value.toInt())
            is Long -> intToBoolean(value.toInt())
            is ULong -> intToBoolean(value.toInt())
            is Float -> intToBoolean(value.toInt())
            is Double -> intToBoolean(value.toInt())
            else -> null
        }
    }

    fun toInteger(value: Any?): Int? {

        if (value == null) return null

        return when (value) {
            is Int -> return value
            is Long -> return value.toInt()
            is Float -> return value.toInt()
            is Double -> return value.toInt()
            is String -> try {
                Integer.decode(value)
            } catch (e: NumberFormatException) {
                null
            }

            is Boolean -> return if (value) 1 else 0
            is Char -> try {
                Integer.decode(value.toString())
            } catch (e: NumberFormatException) {
                null
            }

            is Byte -> return value.toInt()
            is UByte -> return value.toInt()
            is Short -> return value.toInt()
            is UShort -> return value.toInt()
            is UInt -> return value.toInt()
            is ULong -> return value.toInt()
            else -> return null
        }
    }

    fun toLong(value: Any?): Long? {

        if (value == null) return null

        return when (value) {
            is Int -> return value.toLong()
            is Long -> return value
            is Float -> return value.toLong()
            is Double -> return value.toLong()
            is String -> try {
                java.lang.Long.decode(value)
            } catch (e: NumberFormatException) {
                null
            }

            is Boolean -> return if (value) 1 else 0
            is Char -> try {
                java.lang.Long.decode(value.toString())
            } catch (e: NumberFormatException) {
                null
            }

            is Byte -> return value.toLong()
            is UByte -> return value.toLong()
            is Short -> return value.toLong()
            is UShort -> return value.toLong()
            is UInt -> return value.toLong()
            is ULong -> return value.toLong()
            is Instant -> return value.epochSecond
            else -> return null
        }
    }

    fun toFloat(value: Any?): Float? {

        if (value == null) return null

        return when (value) {
            is Int -> value.toFloat()
            is Long -> value.toFloat()
            is Float -> value
            is Double -> value.toFloat()
            is String -> try {
                java.lang.Float.parseFloat(value)
            } catch (e: NumberFormatException) {
                null
            }

            is Boolean -> if (value) 1.0f else 0.0f
            is Char -> try {
                java.lang.Float.parseFloat(value.toString())
            } catch (e: NumberFormatException) {
                null
            }

            is Byte -> value.toFloat()
            is UByte -> value.toFloat()
            is Short -> value.toFloat()
            is UShort -> value.toFloat()
            is UInt -> value.toFloat()
            is ULong -> value.toFloat()
            is Instant -> value.epochSecond.toFloat()
            else -> null
        }
    }

    fun toDouble(value: Any?): Double? {

        if (value == null) return null

        return when (value) {
            is Int -> value.toDouble()
            is Long -> value.toDouble()
            is Float -> value.toDouble()
            is Double -> value
            is String -> try {
                java.lang.Double.parseDouble(value)
            } catch (e: NumberFormatException) {
                null
            }

            is Boolean -> if (value) 1.0 else 0.0
            is Char -> try {
                java.lang.Double.parseDouble(value.toString())
            } catch (e: NumberFormatException) {
                null
            }

            is Byte -> value.toDouble()
            is UByte -> value.toDouble()
            is Short -> value.toDouble()
            is UShort -> value.toDouble()
            is UInt -> value.toDouble()
            is ULong -> value.toDouble()
            is Instant -> value.epochSecond.toDouble()
            else -> return null
        }
    }

    fun toDate(value: Any?): java.time.LocalDate? {

        if (value == null) return null

        return when (value) {
            is Int -> LocalDate.ofEpochDay(value.toLong())
            is UInt -> LocalDate.ofEpochDay(value.toLong())
            is Long -> LocalDate.ofEpochDay(value)
            is ULong -> LocalDate.ofEpochDay(value.toLong())
            is Float -> LocalDate.ofEpochDay(value.toLong())
            is Double -> LocalDate.ofEpochDay(value.toLong())
            is String -> try {
                java.time.LocalDate.parse(value)
            } catch (e: Exception) {
                null
            }

            is Instant -> LocalDate.ofEpochDay(DateTimeUtil.daysFromInstant(value).toLong())
            else -> return null
        }
    }

    fun toTime(value: Any?): LocalTime? {

        if (value == null) return null

        return when (value) {
            is Int -> LocalTime.ofSecondOfDay(value.toLong())
            is UInt -> LocalTime.ofSecondOfDay(value.toLong())
            is Long -> LocalTime.ofSecondOfDay(value)
            is ULong -> LocalTime.ofSecondOfDay(value.toLong())
            is Float -> LocalTime.ofSecondOfDay(value.toLong())
            is Double -> LocalTime.ofSecondOfDay(value.toLong())
            is String -> try {
                LocalTime.parse(value)
            } catch (e: DateTimeParseException) {
                null
            }

            is Instant -> value.atZone(ZoneId.systemDefault()).toLocalTime()
            else -> return null
        }
    }


    fun createInstantFromNanos(nanosSinceEpoch: Long): Instant {
        val seconds = nanosSinceEpoch / 1_000_000_000
        val nanos = (nanosSinceEpoch % 1_000_000_000).toInt()
        return Instant.ofEpochSecond(seconds, nanos.toLong())
    }

    fun toTimestampWithoutZone(value: Any?): LocalDateTime? {

        if (value == null) return null

        return when (value) {
            is Int -> LocalDateTime.ofEpochSecond(value.toLong(), 0, ZoneOffset.UTC)
            is UInt -> LocalDateTime.ofEpochSecond(value.toLong(), 0, ZoneOffset.UTC)
            is Long -> LocalDateTime.ofEpochSecond(value, 0, ZoneOffset.UTC)
            is ULong -> LocalDateTime.ofEpochSecond(value.toLong(), 0, ZoneOffset.UTC)
            is Float -> LocalDateTime.ofEpochSecond(value.toLong(), 0, ZoneOffset.UTC)
            is Double -> LocalDateTime.ofEpochSecond(value.toLong(), 0, ZoneOffset.UTC)
            is String -> try {
                LocalDateTime.parse(value)
            } catch (e: DateTimeParseException) {
                null
            }

            is Instant -> LocalDateTime.ofEpochSecond(value.epochSecond, 0, ZoneOffset.UTC)
            else -> return null
        }

    }

    fun toTimestampNanoWithoutZone(value: Any?): LocalDateTime? {

        if (value == null) return null

        return when (value) {
            is Long -> LocalDateTime.ofInstant(createInstantFromNanos(value), ZoneOffset.UTC)
            is ULong -> LocalDateTime.ofInstant(createInstantFromNanos(value.toLong()), ZoneOffset.UTC)
            is Float -> LocalDateTime.ofInstant(createInstantFromNanos(value.toLong()), ZoneOffset.UTC)
            is Double -> LocalDateTime.ofInstant(createInstantFromNanos(value.toLong()), ZoneOffset.UTC)
            is String -> try {
                LocalDateTime.parse(value)
            } catch (e: DateTimeParseException) {
                null
            }

            is Instant -> LocalDateTime.ofInstant(value, ZoneOffset.UTC)
            else -> return null
        }

    }

    fun toTimestampWithZone(value: Any?): OffsetDateTime? {

        if (value == null) return null

        return when (value) {
            is Int -> OffsetDateTime.ofInstant(createInstantFromNanos(value.toLong()), ZoneOffset.UTC)
            is UInt -> OffsetDateTime.ofInstant(createInstantFromNanos(value.toLong()), ZoneOffset.UTC)
            is Long -> OffsetDateTime.ofInstant(createInstantFromNanos(value), ZoneOffset.UTC)
            is ULong -> OffsetDateTime.ofInstant(createInstantFromNanos(value.toLong()), ZoneOffset.UTC)
            is Float -> OffsetDateTime.ofInstant(createInstantFromNanos(value.toLong()), ZoneOffset.UTC)
            is Double -> OffsetDateTime.ofInstant(createInstantFromNanos(value.toLong()), ZoneOffset.UTC)
            is String -> try {
                OffsetDateTime.parse(value)
            } catch (e: DateTimeParseException) {
                null
            }

            is Instant -> OffsetDateTime.ofInstant(value, ZoneOffset.UTC)
            else -> return null
        }

    }

    fun toTimestampNanoWithZone(value: Any?): OffsetDateTime? {

        if (value == null) return null

        return when (value) {
            is Long -> DateTimeUtil.timestamptzFromMicros(Instant.ofEpochSecond(value).toEpochMilli())
            is ULong -> DateTimeUtil.timestamptzFromMicros(Instant.ofEpochSecond(value.toLong()).toEpochMilli())
            is Float -> DateTimeUtil.timestamptzFromMicros(Instant.ofEpochSecond(value.toLong()).toEpochMilli())
            is Double -> DateTimeUtil.timestamptzFromMicros(Instant.ofEpochSecond(value.toLong()).toEpochMilli())
            is String -> try {
                java.time.OffsetDateTime.parse(value)
            } catch (e: DateTimeParseException) {
                null
            }

            is Instant -> DateTimeUtil.timestamptzFromMicros(value.toEpochMilli())
            else -> return null
        }

    }

    fun toUUID(value: Any?): UUID? {

        if (value == null) return null

        return when (value) {
            is String -> try {
                UUID.fromString(value)
            } catch (e: IllegalArgumentException) {
                null
            }

            is UUID -> value
            else -> return null
        }
    }

    fun toBinary(value: Any?): ByteArray? {

        if (value == null) return null

        return when (value) {
            is Boolean -> return byteArrayOf(if (value) 1 else 0)
            is Char -> ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putChar(value).array()
            is Byte -> return byteArrayOf(value)
            is UByte -> return byteArrayOf(value.toByte())
            is Short -> ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
            is UShort -> ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value.toShort()).array()
            is Int -> ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
            is UInt -> ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value.toInt()).array()
            is Long -> ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array()
            is ULong -> ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value.toLong()).array()
            is Float -> ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array()
            is Double -> ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(value).array()
            is String -> value.toByteArray(StandardCharsets.UTF_8)
            is Instant -> ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putLong(value.epochSecond).putInt(value.nano).array()
            is UUID -> ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN).putLong(value.leastSignificantBits).putLong(value.mostSignificantBits).array()
            else -> return null
        }
    }


    fun toFixed(value: Any?, precision: Int): Float? {

        fun asFixed(value: Float): Float {
            val factor = 10.0.pow(precision.toDouble()).toFloat()
            return (value * factor).roundToInt() / factor
        }

        if (value == null) return null

        return when (value) {
            is Boolean -> if (value) 1.0.toFloat() else 0.0.toFloat()
            is Byte -> value.toFloat()
            is Short -> value.toFloat()
            is UShort -> value.toFloat()
            is Int -> value.toFloat()
            is UInt -> value.toFloat()
            is Long -> value.toFloat()
            is ULong -> value.toFloat()
            is Float -> asFixed(value)
            is Double -> asFixed(value.toFloat())
            is String -> try {
                asFixed(java.lang.Float.parseFloat(value))
            } catch (e: NumberFormatException) {
                null
            }

            else -> null
        }
    }

    fun toDecimal(value: Any?, precision: Int, scale: Int): Double? {
        if (value == null) return null

        fun asDecimal(value: Double): Double? {

            val factor = 10.0.pow(scale)
            val scaled = (value * factor).toLong()
            return scaled / factor
        }


        return when (value) {
            is Boolean -> if (value) 1.0 else 0.0
            is Byte -> asDecimal(value.toDouble())
            is Short -> asDecimal(value.toDouble())
            is UShort -> asDecimal(value.toDouble())
            is Int -> asDecimal(value.toDouble())
            is UInt -> asDecimal(value.toDouble())
            is Long -> asDecimal(value.toDouble())
            is ULong -> asDecimal(value.toDouble())
            is Float -> asDecimal(value.toDouble())
            is Double -> asDecimal(value)
            is String -> try {
                asDecimal(java.lang.Double.parseDouble(value))
            } catch (e: NumberFormatException) {
                null
            }

            else -> null
        }
    }

    fun toString(value: Any?): String? = value?.toString()

    fun toList(type: Type, value: Any?): List<Any>? {
        if (value == null) return null

        return if (value !is List<*>) {
            val singleValue = type.from(value)
            if (singleValue == null) null else listOf(singleValue)

        } else {
            val list = value.map { type.from(it) }
            @Suppress("UNCHECKED_CAST")
            try {
                if (list.none { it == null }) list as List<Any> else null
            } catch (_: Exception) {
                null
            }
        }

    }

    fun toMap(keyType: Type, valueType: Type, value: Any?): Map<Any, Any>? {
        if (value == null || (value !is Map<*, *>)) return null

        return try {
            value.map { (key, value) ->
                val k = keyType.from(key) ?: return null
                val v = valueType.from(value) ?: return null
                k to v
            }.toMap()
        } catch (_: Exception) {
            null
        }


    }




}