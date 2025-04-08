//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.simulator.simulations

import com.amazonaws.sfc.simulator.simulations.SimulationDeserializer.Companion.asDouble
import com.google.gson.JsonPrimitive
import java.security.InvalidParameterException
import kotlin.reflect.KClass

object SimulationHelper {
    fun asNumericType(value: Double, dataType: DataType): Any? =
        when (dataType) {
            DataType.BYTE -> value.toInt().toByte()
            DataType.SHORT -> value.toInt().toShort()
            DataType.INT -> value.toInt()
            DataType.LONG -> value.toLong()
            DataType.FLOAT -> value.toFloat()
            DataType.DOUBLE -> value
            DataType.STRING -> value.toString()
            DataType.UBYTE -> value.toInt().toUByte()
            DataType.USHORT -> value.toInt().toUShort()
            DataType.UINT -> value.toUInt()
            DataType.ULONG -> value.toULong()
            else -> null
        }

    fun asAnyType(value: Any?, type: DataType): Any? {
        return if (value != null) when (type) {
            DataType.BOOLEAN -> when (value) {
                is Boolean -> value
                is String -> {
                    when (value.toString().lowercase()) {
                        "true" -> true
                        "false" -> false
                        else -> null
                    }
                }

                is Double -> value == 1.0
                else -> null

            }

            DataType.STRING -> value.toString()
            DataType.CHAR -> value.toString().firstOrNull()
            else ->  asNumericType(asDouble(value), type)
        } else null
    }


    fun asByte(value: Any): Any {
        return try {
            asDouble(value).toInt().toByte()
        } catch (_: Exception) {
            throw InvalidParameterException("Value $value must be a valid byte")
        }
    }

    fun asUByte(value: Any): Any {
        return try {
            asDouble(value).toInt().toUByte()
        } catch (_: Exception) {
            throw InvalidParameterException("Value $value must be a valid unsigned byte")
        }
    }

    fun asShort(value: Any): Any {
        return try {
            val d: Double = asDouble(value)
            d.toInt().toShort()
        } catch (_: Exception) {
            throw InvalidParameterException("Value $value must be a valid short number")
        }
    }

    fun asUShort(value: Any): Any {
        return try {
            val d: Double = asDouble(value)
            d.toInt().toUShort()
        } catch (_: Exception) {
            throw InvalidParameterException("Value $value must be a valid unsigned short number")
        }
    }

    fun asInt(value: Any): Any {
        return try {
            val d: Double = asDouble(value)
            d.toInt()
        } catch (_: Exception) {
            throw InvalidParameterException("Value $value must be a valid integer number")
        }
    }

    fun asUInt(value: Any): Any {
        return try {
            val d: Double = asDouble(value)
            d.toInt().toUInt()
        } catch (_: Exception) {
            throw InvalidParameterException("Value $value must be a valid unsigned integer number")
        }
    }

    fun asLong(value: Any): Any {
        return try {
            val d: Double = asDouble(value)
            d.toLong()
        } catch (_: Exception) {
            throw InvalidParameterException("Value $value must be a valid long number")
        }
    }

    fun asULong(value: Any): Any {
        return try {
            val d: Double = asDouble(value)
            d.toULong()
        } catch (_: Exception) {
            throw InvalidParameterException("Value $value must be a valid unsigned long number")
        }
    }

    fun asFloat(value: Any): Any {
        if (value is JsonPrimitive) {
            return value.asFloat
        }
        return try {
            val d: Double = asDouble(value)
            d.toFloat()
        } catch (_: Exception) {
            throw InvalidParameterException("Value $value must be a valid float number")
        }
    }


    fun maxValueForType(clazz: KClass<*>): Double {
        return when (clazz) {
            Double::class -> Double.MAX_VALUE
            Float::class -> Float.MAX_VALUE.toDouble()
            Int::class -> Int.MAX_VALUE.toDouble()
            Long::class -> Long.MAX_VALUE.toDouble()
            Short::class -> Short.MAX_VALUE.toDouble()
            Byte::class -> Byte.MAX_VALUE.toDouble()
            UByte::class -> UByte.MAX_VALUE.toDouble()
            UShort::class -> UShort.MAX_VALUE.toDouble()
            UInt::class -> UInt.MAX_VALUE.toDouble()
            ULong::class -> ULong.MAX_VALUE.toDouble()
            else -> Double.MAX_VALUE
        }
    }

    fun minValueForType(clazz: KClass<*>): Double {
        return when (clazz) {
            Double::class -> Double.MIN_VALUE
            Float::class -> Float.MIN_VALUE.toDouble()
            Int::class -> Int.MIN_VALUE.toDouble()
            Long::class -> Long.MIN_VALUE.toDouble()
            Short::class -> Short.MIN_VALUE.toDouble()
            Byte::class -> Byte.MIN_VALUE.toDouble()
            UByte::class -> UByte.MIN_VALUE.toDouble()
            UShort::class -> UShort.MIN_VALUE.toDouble()
            UInt::class -> UInt.MIN_VALUE.toDouble()
            ULong::class -> ULong.MIN_VALUE.toDouble()
            else -> Double.MIN_VALUE
        }
    }

}