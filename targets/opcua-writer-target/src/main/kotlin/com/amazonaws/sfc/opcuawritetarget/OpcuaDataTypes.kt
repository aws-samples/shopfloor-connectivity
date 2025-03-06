// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.opcuawritetarget

import com.amazonaws.sfc.data.JsonHelper
import org.eclipse.milo.opcua.stack.core.Identifiers
import org.eclipse.milo.opcua.stack.core.types.builtin.*
import java.time.Instant


class TypeConversionException(message: String) : Exception(message)

class OpcuaDataTypes {

    companion object {

        fun asType(value: Any, type: NodeId): Any =
            when (type) {
                Identifiers.Boolean -> toBoolean(value)

                Identifiers.ByteString -> ByteString.of(value.toString().encodeToByteArray())
                Identifiers.DateTime -> DateTime(Instant.parse(value.toString()))
                Identifiers.Double -> toDouble(value)
                Identifiers.ExpandedNodeId -> ExpandedNodeId.parse(value.toString())
                Identifiers.Float -> toFloat(value)
                Identifiers.Int16 -> toShort(value)
                Identifiers.Int32 -> toInt(value)
                Identifiers.Int64 -> toLong(value)
                Identifiers.NodeId -> NodeId.parse(value.toString())
                Identifiers.SByte -> toByte(value)
                Identifiers.String -> value.toString()
                Identifiers.Structure -> JsonHelper.gsonExtended().toJson(value)
                Identifiers.Byte -> toByte(value)
                Identifiers.UInt16 -> toShort(value)
                Identifiers.UInt32 -> toInt(value)
                Identifiers.UInt64 -> toLong(value)
                Identifiers.XmlElement ->  XmlElement(value.toString())
                else -> value

            }


        fun toBoolean(value: Any): Boolean {
            return when (value) {
                is Boolean -> value
                is Byte -> value.toInt() != 0
                is UByte -> value.toInt() != 0
                is Int -> value != 0
                is UInt -> value != 0.toUInt()
                is Short -> value != 0.toShort()
                is UShort -> value != 0.toUShort()
                is Long -> value != 0.toLong()
                is ULong -> value != 0.toULong()
                is Float -> value != 0.0f
                is Double -> value != 0.0
                else -> throw TypeConversionException("Cannot convert $value to Boolean")

            }
        }

        fun toByte(value: Any): Any =
            when (value) {
                is Boolean -> if (value) 1 else 0
                is Byte -> value
                is UByte -> value.toByte()
                is Int -> value.toByte()
                is UInt -> value.toByte()
                is Short -> value.toByte()
                is UShort -> value.toByte()
                is Long -> value.toByte()
                is ULong -> value.toByte()
                is Float -> value.toInt().toByte()
                is Double -> value.toInt().toByte()
                else -> throw TypeConversionException("Cannot convert $value to Byte")
            }

        fun toUByte(value: Any): Any =
            when (value) {
                is Boolean -> if (value) 1.toUByte() else 0.toUByte()
                is Byte -> value.toUByte()
                is UByte -> value
                is Int -> value.toUByte()
                is UInt -> value.toUByte()
                is Short -> value.toUByte()
                is UShort -> value.toUByte()
                is Long -> value.toUByte()
                is ULong -> value.toUByte()
                is Float -> value.toInt().toByte()
                is Double -> value.toInt().toByte()
                else -> throw TypeConversionException("Cannot convert $value to UByte")
            }

        fun toShort(value: Any): Any =
            when (value) {
                is Boolean -> if (value) 1.toShort() else 0.toShort()
                is Byte -> value.toShort()
                is UByte -> value.toShort()
                is Int -> value.toShort()
                is UInt -> value.toShort()
                is Short -> value
                is UShort -> value.toShort()
                is Long -> value.toShort()
                is ULong -> value.toShort()
                is Float -> value.toInt().toShort()
                is Double -> value.toInt().toShort()
                else -> throw TypeConversionException("Cannot convert $value to Short")
            }


        fun toUShort(value: Any): UShort =
            when (value) {
                is Boolean -> if (value) 1.toUShort() else 0.toUShort()
                is Byte -> value.toUShort()
                is UByte -> value.toUShort()
                is Int -> value.toUShort()
                is UInt -> value.toUShort()
                is Short -> value.toUShort()
                is UShort -> value
                is Long -> value.toUShort()
                is ULong -> value.toUShort()
                is Float -> value.toInt().toUShort()
                is Double -> value.toInt().toUShort()
                else -> throw TypeConversionException("Cannot convert $value to UShort")
            }

        fun toInt(value: Any): Int =
            when (value) {
                is Boolean -> if (value) 1 else 0
                is Byte -> value.toInt()
                is UByte -> value.toInt()
                is Int -> value
                is UInt -> value.toInt()
                is Short -> value.toInt()
                is UShort -> value.toInt()
                is Long -> value.toInt()
                is ULong -> value.toInt()
                is Float -> value.toInt()
                is Double -> value.toInt()
                else -> throw TypeConversionException("Cannot convert $value to Int")
            }

        fun toUInt(value: Any): UInt = when (value) {
            is Boolean -> if (value) 1.toUInt() else 0.toUInt()
            is Byte -> value.toUInt()
            is UByte -> value.toUInt()
            is Int -> value.toUInt()
            is UInt -> value
            is Short -> value.toUInt()
            is UShort -> value.toUInt()
            is Long -> value.toUInt()
            is ULong -> value.toUInt()
            is Float -> value.toInt().toUInt()
            is Double -> value.toInt().toUInt()
            else -> throw TypeConversionException("Cannot convert $value to UInt")
        }

        fun toLong(value: Any): Long =
            when (value) {
                is Boolean -> if (value) 1.toLong() else 0.toLong()
                is Byte -> value.toLong()
                is UByte -> value.toLong()
                is Int -> value.toLong()
                is UInt -> value.toLong()
                is Short -> value.toLong()
                is UShort -> value.toLong()
                is Long -> value.toLong()
                is ULong -> value.toLong()
                is Float -> value.toInt().toLong()
                is Double -> value.toInt().toLong()
                else -> throw TypeConversionException("Cannot convert $value to Long")
            }


        fun toULong(value: Any): ULong = when (value) {
            is Boolean -> if (value) 1.toULong() else 0.toULong()
            is Byte -> value.toULong()
            is UByte -> value.toULong()
            is Int -> value.toULong()
            is UInt -> value.toULong()
            is Short -> value.toULong()
            is UShort -> value.toULong()
            is Long -> value.toULong()
            is ULong -> value.toULong()
            is Float -> value.toInt().toULong()
            is Double -> value.toInt().toULong()
            else -> throw TypeConversionException("Cannot convert $value to ULong")
        }

        fun toFloat(value: Any): Float = when (value) {
            is Boolean -> if (value) 1.toFloat() else 0.toFloat()
            is Byte -> value.toFloat()
            is UByte -> value.toFloat()
            is Int -> value.toFloat()
            is UInt -> value.toFloat()
            is Short -> value.toFloat()
            is UShort -> value.toFloat()
            is Long -> value.toFloat()
            is ULong -> value.toFloat()
            is Float -> value.toFloat()
            is Double -> value.toFloat()
            else -> throw TypeConversionException("Cannot convert $value to Float")
        }

        fun toDouble(value: Any): Double =
            when (value) {
                is Boolean -> if (value) 1.toDouble() else 0.toDouble()
                is Byte -> value.toDouble()
                is UByte -> value.toDouble()
                is Int -> value.toDouble()
                is UInt -> value.toDouble()
                is Short -> value.toDouble()
                is UShort -> value.toDouble()
                is Long -> value.toDouble()
                is ULong -> value.toDouble()
                is Double -> value.toDouble()
                is Float -> value.toDouble()
                else -> throw TypeConversionException("Cannot convert $value to Double")
            }
    }
}