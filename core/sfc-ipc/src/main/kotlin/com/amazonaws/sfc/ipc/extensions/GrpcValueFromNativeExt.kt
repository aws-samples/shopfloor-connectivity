
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.ipc.extensions

import com.amazonaws.sfc.data.JsonHelper.Companion.gsonExtended
import com.amazonaws.sfc.ipc.*
import com.amazonaws.sfc.ipc.extensions.GrpcSourceValueFromNativeExt.setValueTimestamp
import com.google.gson.Gson
import com.google.protobuf.ByteString
import java.time.Instant

/**
 * Helpers to build the values in gRPC responses which are sent from the protocol server to the SFC core
 */
object GrpcValueFromNativeExt {

    /**
     * gson engine used to decode structured values with mixed types to jSON
     */
    val gson: Gson by lazy {
        gsonExtended()
    }


    /**
     * Builds a new channel value from a byte value
     * @param value Byte
     * @param ts Instant?
     * @return ChannelValue?
     */
    fun channelValue(value: Byte, ts: Instant?): ChannelValue? =
        ChannelValue.newBuilder()
            .setSignedByte(value.toInt())
            .setValueTimestamp(ts).build()

    /**
     * Builds a new channel value from an array of bye values
     * @param value ArrayList<Byte>
     * @param ts Instant?
     * @return ChannelValue?
     */
    fun channelValue(value: ArrayList<Byte>, ts: Instant?): ChannelValue? =
        ChannelValue.newBuilder()
            .setSignedByteArray(ByteString.copyFrom(value.toByteArray()))
            .setValueTimestamp(ts).build()


    /**
     * Builds a new channel value from an unsigned byte value
     * @param value UByte
     * @param ts Instant?
     * @return ChannelValue?
     */
    fun channelValue(value: UByte, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setUnsignedByte(value.toInt())
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from an array of unsigned byte values
     * @param value ArrayList<UByte>
     * @param ts Instant?
     * @return ChannelValue?
     */
    @JvmName("addUByteArray")
    fun channelValue(value: ArrayList<UByte>, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            // maps to signed byte in order to use bytes protobuf type, convert back to unsigned byte at receiving side
            .setUnsignedByteArray(ByteString.copyFrom(byteArrayOf(*(value.map { it.toByte() }).toByteArray())))
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from a 16 bit integer value
     * @param value Short
     * @param ts Instant?
     * @return ChannelValue?
     */
    fun channelValue(value: Short, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder().setSignedInt16(value.toInt())
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from an array of 16-bit integer values
     * @param value ArrayList<Short>
     * @param ts Instant?
     * @return ChannelValue?
     */
    @JvmName("addShortArray")
    fun channelValue(value: ArrayList<Short>, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setSignedInt16Array(SignedInt16Array.newBuilder().addAllItems(value.map { it.toInt() }).build())
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from an unsigned 16-bit integer value
     * @param value UShort
     * @param ts Instant?
     * @return ChannelValue?
     */
    fun channelValue(value: UShort, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder().setUnsignedInt16(value.toInt())
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from an array unsigned 16 bit values
     * @param value ArrayList<UShort>
     * @param ts Instant?
     * @return ChannelValue?
     */
    @JvmName("addUShortArray")
    fun channelValue(value: ArrayList<UShort>, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setUnsignedInt16Array(UnsignedInt16Array.newBuilder().addAllItems(value.map { it.toInt() }))
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from a 32-bit integer value
     * @param value Int
     * @param ts Instant?
     * @return ChannelValue?
     */
    fun channelValue(value: Int, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setSignedInt32(value)
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from an array of 32-bit integer values
     * @param value ArrayList<Int>
     * @param ts Instant?
     * @return ChannelValue?
     */
    @JvmName("addInt32Array")
    fun channelValue(value: ArrayList<Int>, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setSignedInt32Array(SignedInt32Array.newBuilder().addAllItems(value.asIterable()).build())
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from an unsigned 32-bit integer value
     * @param value UInt
     * @param ts Instant?
     * @return ChannelValue?
     */
    fun channelValue(value: UInt, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setUnsignedInt32(value.toInt())
            .setValueTimestamp(ts).build()
    }

    /**
     * Builds a new channel value from an array of 32-bit unsigned integer values
     * @param value ArrayList<UInt>
     * @param ts Instant?
     * @return ChannelValue?
     */
    @JvmName("addUInt32Array")
    fun channelValue(value: ArrayList<UInt>, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setUnsignedInt32Array(UnsignedInt32Array.newBuilder().addAllItems(value.map { it.toInt() }).build())
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from a 64 bit integer value
     * @param value Long
     * @param ts Instant?
     * @return ChannelValue?
     */
    fun channelValue(value: Long, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setSignedInt64(value)
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from an array of 64-bit integer values
     * @param value ArrayList<Long>
     * @param ts Instant?
     * @return ChannelValue?
     */
    @JvmName("addLongArray")
    fun channelValue(value: ArrayList<Long>, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setSignedInt64Array(SignedInt64Array.newBuilder().addAllItems(value.asIterable()).build())
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from an unsigned 64-bit integer value
     * @param value ULong
     * @param ts Instant?
     * @return ChannelValue?
     */
    fun channelValue(value: ULong, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setUnsignedInt64(value.toLong())
            .setValueTimestamp(ts).build()
    }

    /**
     * Builds a new channel value from an array of unsigned 64 bit integer values
     * @param value ArrayList<ULong>
     * @param ts Instant?
     * @return ChannelValue?
     */
    @JvmName("addULongArray")
    fun channelValue(value: ArrayList<ULong>, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setUnsignedInt64Array(UnsignedInt64Array.newBuilder().addAllItems(value.map { it.toLong() }).build())
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from a boolean value
     * @param value Boolean
     * @param ts Instant?
     * @return ChannelValue?
     */
    fun channelValue(value: Boolean, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setBool(value).setValueTimestamp(ts)
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from an array of boolean values
     * @param value ArrayList<Boolean>
     * @param ts Instant?
     * @return ChannelValue?
     */
    @JvmName("addBoolArray")
    fun channelValue(value: ArrayList<Boolean>, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setBoolArray(BoolArray.newBuilder().addAllItems(value.asIterable()))
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from a double value
     * @param value Double
     * @param ts Instant?
     * @return ChannelValue?
     */
    fun channelValue(value: Double, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setDouble(value)
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from an array of double values
     * @param value ArrayList<Double>
     * @param ts Instant?
     * @return ChannelValue?
     */
    @JvmName("addDoubleArray")
    fun channelValue(value: ArrayList<Double>, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setDoubleArray(com.amazonaws.sfc.ipc.DoubleArray.newBuilder().addAllItems(value.asIterable()).build())
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from a float value
     * @param value Float
     * @param ts Instant?
     * @return ChannelValue?
     */
    fun channelValue(value: Float, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setFloat(value)
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from an array of float values
     * @param value ArrayList<Float>
     * @param ts Instant?
     * @return ChannelValue?
     */
    @JvmName("addFloatArray")
    fun channelValue(value: ArrayList<Float>, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setFloatArray(com.amazonaws.sfc.ipc.FloatArray.newBuilder().addAllItems(value.asIterable()).build())
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from a string value
     * @param value String
     * @param ts Instant?
     * @return ChannelValue?
     */
    fun channelValue(value: String, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setString(value)
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from an array od string values
     * @param value ArrayList<String>
     * @param ts Instant?
     * @return ChannelValue?
     */
    @JvmName("addStringArray")
    fun channelValue(value: ArrayList<String>, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setStringArray(StringArray.newBuilder().addAllItems(value.asIterable()).build())
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from a timestamp value
     * @param value Instant
     * @param ts Instant?
     * @return ChannelValue?
     */
    fun channelValue(value: Instant, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setDatetime(newTimestamp(value))
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from an array of timestamp values
     * @param value ArrayList<Instant>
     * @param ts Instant?
     * @return ChannelValue?
     */
    @JvmName("addDateTimeList")
    fun channelValue(value: ArrayList<Instant>, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setDatetimeArray(TimestampArray.newBuilder().addAllTimestamp(value.map { newTimestamp(it) }).build())
            .setValueTimestamp(ts)
            .build()
    }

    fun channelValue(value: Any, ts: Instant?): ChannelValue? {
        val a = when (value) {
            is Boolean -> channelValue(value, ts)
            is Byte -> channelValue(value, ts)
            is Double -> channelValue(value, ts)
            is Float -> channelValue(value, ts)
            is Instant -> channelValue(value, ts)
            is Int -> channelValue(value, ts)
            is LinkedHashMap<*, *> -> channelValue(value, ts)
            is Long -> channelValue(value, ts)
            is Short -> channelValue(value, ts)
            is String -> channelValue(value, ts)
            is UByte -> channelValue(value.toInt(), ts)
            is UInt -> channelValue(value.toInt(), ts)
            is ULong -> channelValue(value.toLong(), ts)
            is UShort -> channelValue(value.toInt(), ts)
            is ArrayList<*> -> {
                channelArrayValue(value, ts)
            }

            else -> {
                channelValue(value.toString(), ts)
            }
        }
        return a
    }

    private fun channelArrayValue(value: ArrayList<*>, ts: Instant?) = if (value.isEmpty()) null else
        when (value.first()) {
            is Boolean -> channelValue(arrayListOf(*value.map { it as Boolean? }.toTypedArray<Boolean?>()), ts)
            is Byte -> channelValue(arrayListOf(*value.map { it as Byte }.toTypedArray<Byte>()), ts)
            is Double -> channelValue(arrayListOf(*value.map { it as Double }.toTypedArray<Double>()), ts)
            is Float -> channelValue(arrayListOf(*value.map { it as Float }.toTypedArray<Float>()), ts)
            is Instant -> channelValue(arrayListOf(*value.map { it as Instant }.toTypedArray<Instant>()), ts)
            is Int -> channelValue(arrayListOf(*value.map { it as Int }.toTypedArray<Int>()), ts)
            is LinkedHashMap<*, *> -> channelValue(arrayListOf(*value.map { it as LinkedHashMap<*, *> }.toTypedArray<LinkedHashMap<*, *>>()), ts)
            is Long -> channelValue(arrayListOf(*value.map { it as Long }.toTypedArray<Long>()), ts)
            is Short -> channelValue(arrayListOf(*value.map { it as Short }.toTypedArray<Short>()), ts)
            is String -> channelValue(arrayListOf(*value.map { it as String }.toTypedArray<String>()), ts)
            is UByte -> channelValue(arrayListOf(*value.map { it as UByte }.toTypedArray<UByte>()), ts)
            is UInt -> channelValue(arrayListOf(*value.map { it as UInt }.toTypedArray<UInt>()), ts)
            is ULong -> channelValue(arrayListOf(*value.map { it as ULong }.toTypedArray<ULong>()), ts)
            is UShort -> channelValue(arrayListOf(*value.map { it as UShort }.toTypedArray<UShort>()), ts)
            is ArrayList<*> -> channelValue(arrayListOf(*value.map { it as ArrayList<*> }.toTypedArray<ArrayList<*>>()), ts)
            else -> channelValue(ArrayList<String>(value.size).addAll(value.map { it.toString() }), ts)
        }

    /**
     * Builds a new channel value from a map of values
     * @param value LinkedHashMap<*, *>
     * @param ts Instant?
     * @return ChannelValue?
     */
    fun channelValue(value: LinkedHashMap<*, *>, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setTypedHashMap(typedMap(value))
            .setValueTimestamp(ts)
            .build()
    }

    private fun typedMap(value: LinkedHashMap<*, *>): TypedMap? =
        TypedMap.newBuilder().putAllEntries(value.map { (k, v) -> k.toString() to channelValue(v, null) }.toMap()).build()

    /**
     * Builds a new channel value from an array of map values
     * @param value ArrayList<LinkedHashMap<*, *>>
     * @param ts Instant?
     * @return ChannelValue?
     */
    @JvmName("addMapList")
    fun channelValue(value: ArrayList<LinkedHashMap<*, *>>, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setTypedHasMapArray(TypedMapArray.newBuilder()
                .addAllItems(value.map { typedMap(it) })
                .build())
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from a custom data type
     * @param value Any
     * @param ts Instant?
     * @return ChannelValue?
     */
    fun channelCustomValue(value: Any, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setCustom(gson.toJson(value))
            .setValueTimestamp(ts)
            .build()
    }

    /**
     * Builds a new channel value from an array of custom data types
     * @param value ArrayList<Any>
     * @param ts Instant?
     * @return ChannelValue?
     */
    fun channelCustomValueList(value: Iterable<Any>, ts: Instant?): ChannelValue? {
        return ChannelValue.newBuilder()
            .setCustomArray(gson.toJson(arrayListOf(*value.toList().toTypedArray())))
            .setValueTimestamp(ts)
            .build()
    }

}

