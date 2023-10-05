/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.ipc.extensions

import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.data.DataTypes.safeAsList
import com.amazonaws.sfc.ipc.ChannelValue
import com.amazonaws.sfc.ipc.ReadValuesReply
import com.amazonaws.sfc.ipc.SourceReadValuesReply
import com.amazonaws.sfc.ipc.extensions.GrpcTargetValueFromNativeExt.isMixedTypeArray
import com.amazonaws.sfc.ipc.extensions.GrpcValueFromNativeExt.channelCustomValue
import com.amazonaws.sfc.ipc.extensions.GrpcValueFromNativeExt.channelCustomValueList
import com.amazonaws.sfc.ipc.extensions.GrpcValueFromNativeExt.channelValue
import com.google.protobuf.ByteString
import java.io.ByteArrayOutputStream
import java.time.Instant

/**
 * Extensions to convert native source values into gRPC response. These are used by the gRPC protocol servers to
 * build the replies which are streamed to the SFC code client.
 */
object GrpcSourceValueFromNativeExt {

    /**
     * Creates a new ReadValues gRPC reply builder
     * @param sourceID String ID of the source
     * @param timestamp Instant? Timestamp for the source
     * @return SourceReadValuesReply.Builder
     */
    private fun newSourceReadValuesReplyBuilder(sourceID: String, timestamp: Instant? = null): SourceReadValuesReply.Builder {
        val builder = SourceReadValuesReply.newBuilder().setSourceID(sourceID)
        if (timestamp != null) {
            builder.timestamp = newTimestamp(timestamp)
        }
        return builder
    }

    /**
     * Sets the timestamps for channel values in the gRPC response
     * @receiver ChannelValue.Builder
     * @param ts Instant?
     * @return ChannelValue.Builder
     */
    fun ChannelValue.Builder.setValueTimestamp(ts: Instant? = null): ChannelValue.Builder {
        if (ts != null) {
            this.timestamp = newTimestamp(ts)
        }
        return this
    }


    // BYTE

    /**
     * Adds a byte value to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value Byte Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    private fun SourceReadValuesReply.Builder.addValue(name: String, value: Byte, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }

    /**
     * Adds a list of bytes value to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value ArrayList<Byte> Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @JvmName("addByteList")
    // Byte maps to signed int8 in Kotlin
    fun SourceReadValuesReply.Builder.addValue(name: String, value: ArrayList<Byte>, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }

    /**
     * Adds an array of bytes to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value ByteArray Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @JvmName("addByteArray")
    // Byte maps to signed int8 in Kotlin
    fun SourceReadValuesReply.Builder.addValue(name: String, value: ByteArray, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(arrayListOf(*value.toTypedArray()), ts))
        return this
    }

    /**
     * Adds an unsigned byte value to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value UByte Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    private fun SourceReadValuesReply.Builder.addValue(name: String, value: UByte, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value.toInt(), ts))
        return this
    }

    /**
     * Adds a list of unsigned bytes value to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value ArrayList<UByte> Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @JvmName("addUByteList")
    fun SourceReadValuesReply.Builder.addValue(name: String, value: ArrayList<UByte>, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }

    /**
     * Adds an array of unsigned bytes value to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value UByteArray Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @Suppress("unused")
    private fun SourceReadValuesReply.Builder.addValue(name: String, value: UByteArray, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(arrayListOf(*value.toTypedArray()), ts))
        return this
    }


    // INT16

    /**
     * Adds a 16-bit integer  value to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value Short Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    private fun SourceReadValuesReply.Builder.addValue(name: String, value: Short, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }

    /**
     * Adds a lists of 16-bit integer values to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value ArrayList<Short> Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @JvmName("addShortList")
    fun SourceReadValuesReply.Builder.addValue(name: String, value: ArrayList<Short>, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }

    /**
     * Adds an array of 16-bit integer values to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value ShortArray Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @JvmName("addShortArray")
    fun SourceReadValuesReply.Builder.addValue(name: String, value: ShortArray, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(arrayListOf(*value.toTypedArray()), ts))
        return this
    }

    /**
     * Adds a 16-bit unsigned integer value to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value Short Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    private fun SourceReadValuesReply.Builder.addValue(name: String, value: UShort, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value.toInt(), ts))
        return this
    }

    /**
     * Adds a lists of 16-bit unsigned integer values to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value ArrayList<UShort> Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @JvmName("addUShortList")
    fun SourceReadValuesReply.Builder.addValue(name: String, value: ArrayList<UShort>, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }

    /**
     * Adds an array of 16-bit unsigned integer values to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value UShortArray Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @Suppress("unused")
    private fun SourceReadValuesReply.Builder.addValue(name: String, value: UShortArray, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(arrayListOf(*value.toTypedArray()), ts))
        return this
    }


    // INT32

    /**
     * Adds a 32-bit integer  value to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value Int Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    private fun SourceReadValuesReply.Builder.addValue(name: String, value: Int, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }

    /**
     * Adds a lists of 32-bit integer values to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value ArrayList<Int> Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @JvmName("addInt32List")
    fun SourceReadValuesReply.Builder.addValue(name: String, value: ArrayList<Int>, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }

    /**
     * Adds an array of 32-bit integer values to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value IntArray Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @JvmName("addInt32Array")
    fun SourceReadValuesReply.Builder.addValue(name: String, value: IntArray, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(arrayListOf(*value.toTypedArray()), ts))
        return this
    }

    /**
     * Adds a 32-bit unsigned integer  value to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value UInt Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    private fun SourceReadValuesReply.Builder.addValue(name: String, value: UInt, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value.toLong(), ts))
        return this
    }

    /**
     * Adds a lists of 32-bit unsigned integer values to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value ArrayList<UInt> Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @JvmName("addUInt32List")
    fun SourceReadValuesReply.Builder.addValue(name: String, value: ArrayList<UInt>, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }

    /**
     * Adds an array of 32-bit unsigned integer values to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value UIntArray Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @Suppress("unused")
    private fun SourceReadValuesReply.Builder.addValue(name: String, value: UIntArray, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(arrayListOf(*value.toTypedArray()), ts))
        return this
    }


    // INT64

    /**
     * Adds a 64-bit integer  value to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value Long Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    private fun SourceReadValuesReply.Builder.addValue(name: String, value: Long, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }

    /**
     * Adds a lists of 64-bit integer values to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value ArrayList<Long> Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @JvmName("addLongList")
    fun SourceReadValuesReply.Builder.addValue(name: String, value: ArrayList<Long>, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }

    /**
     * Adds an array of 64-bit integer values to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value ArrayList<Long> Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @JvmName("addLongArray")
    fun SourceReadValuesReply.Builder.addValue(name: String, value: LongArray, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(arrayListOf(*value.toTypedArray()), ts))
        return this
    }

    /**
     * Adds a 64-bit unsigned integer  value to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value Long Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    private fun SourceReadValuesReply.Builder.addValue(name: String, value: ULong, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value.toLong(), ts))
        return this
    }

    /**
     * Adds a lists of 64-bit unsigned integer values to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value ArrayList<ULong> Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @JvmName("addULongList")
    fun SourceReadValuesReply.Builder.addValue(name: String, value: ArrayList<ULong>, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }

    /**
     * Adds an array of 64-bit integer values to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value ArrayList<ULong> Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @Suppress("unused")
    private fun SourceReadValuesReply.Builder.addValue(name: String, value: ULongArray, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(arrayListOf(*value.toTypedArray()), ts))
        return this
    }


    // BOOL

    /**
     * Adds a boolean value to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value Bool Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    private fun SourceReadValuesReply.Builder.addValue(name: String, value: Boolean, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }

    /**
     * Adds a list of boolean values to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value ArrayList<Boolean> Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @JvmName("addBooleanList")
    fun SourceReadValuesReply.Builder.addValue(name: String, value: ArrayList<Boolean>, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }

    /**
     * Adds an array of boolean values to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value BooleanArray Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @JvmName("addBooleanArray")
    fun SourceReadValuesReply.Builder.addValue(name: String, value: BooleanArray, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(arrayListOf(*value.toTypedArray()), ts))
        return this
    }


    // FLOAT

    /**
     * Adds a float value to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value Float Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    private fun SourceReadValuesReply.Builder.addValue(name: String, value: Float, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }

    /**
     * Adds a list of float values to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value ArrayList<Float> Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @JvmName("addFloatList")
    fun SourceReadValuesReply.Builder.addValue(name: String, value: ArrayList<Float>, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }


    /**
     * Adds an array of float values to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value FloatArray Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @JvmName("addFloatArray")
    fun SourceReadValuesReply.Builder.addValue(name: String, value: FloatArray, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(arrayListOf(*value.toTypedArray()), ts))
        return this
    }

    // DOUBLE

    /**
     * Adds a double value to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value Double Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    private fun SourceReadValuesReply.Builder.addValue(name: String, value: Double, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }

    /**
     * Adds a list of double values to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value ArrayList<Double> Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    private fun SourceReadValuesReply.Builder.addValue(name: String, value: ArrayList<Double>, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }

    /**
     * Adds an array of double values to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value DoubleArray Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    private fun SourceReadValuesReply.Builder.addValue(name: String, value: DoubleArray, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(arrayListOf(*value.toTypedArray()), ts))
        return this
    }


    // STRING

    /**
     * Adds a string value to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value String Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    private fun SourceReadValuesReply.Builder.addValue(name: String, value: String, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }

    /**
     * Adds a list of string values to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value ArrayList<String> Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @JvmName("addStringList")
    fun SourceReadValuesReply.Builder.addValue(name: String, value: ArrayList<String>, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }


    // DATETIME

    /**
     * Adds a instant value to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value Instant Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    private fun SourceReadValuesReply.Builder.addValue(name: String, value: Instant, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }

    /**
     * Adds a list of instant values to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value ArrayList<Instant> Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @JvmName("addDateTimeList")
    fun SourceReadValuesReply.Builder.addValue(name: String, value: ArrayList<Instant>, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }

    /**
     * Adds a map value to a response encode as json to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value LinkedHashMap<*,*> Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    private fun SourceReadValuesReply.Builder.addValue(name: String, value: LinkedHashMap<*, *>, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }

    /**
     * Adds a list of map values to a response encoded as json to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value ArrayList<LinkedHashMap<*, *>> Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @JvmName("addMapList")
    fun SourceReadValuesReply.Builder.addValue(name: String, value: ArrayList<LinkedHashMap<*, *>>, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelValue(value, ts))
        return this
    }


    /**
     * Adds a any value encode as json  to a response to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value Any Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @JvmName("addCustomValue")
    fun SourceReadValuesReply.Builder.addCustomValue(name: String, value: Any, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelCustomValue(value, ts))
        return this
    }

    /**
     * Adds a list of any values to a response encoded as json to a response builder
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value ArrayList<Any> Channel value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    @JvmName("addCustomValueList")
    fun SourceReadValuesReply.Builder.addCustomValueList(name: String, value: Iterable<Any>, ts: Instant? = null): SourceReadValuesReply.Builder {
        this.putValues(name, channelCustomValueList(value, ts))
        return this
    }

    /**
     * Add a value to a response builder. Based on th actual type it will store it in the correct field of the GSON response
     * @receiver SourceReadValuesReply.Builder
     * @param name String Name of the value
     * @param value Any? Value
     * @param ts Instant? Timestamp for the value
     * @return SourceReadValuesReply.Builder
     */
    private fun SourceReadValuesReply.Builder.addAnyValue(name: String, value: Any?, ts: Instant? = null): SourceReadValuesReply.Builder =

        when (value) {
            is Iterable<*> -> addArray(value, name, ts)
            is Boolean -> this.addValue(name, value, ts)
            is BooleanArray -> this.addValue(name, value, ts)
            is Byte -> this.addValue(name, value, ts)
            is ByteArray -> this.addValue(name, value, ts)
            is Double -> this.addValue(name, value, ts)
            is DoubleArray -> this.addValue(name, value, ts)
            is Float -> this.addValue(name, value, ts)
            is FloatArray -> this.addValue(name, value, ts)
            is Instant -> this.addValue(name, value, ts)
            is Int -> this.addValue(name, value, ts)
            is IntArray -> this.addValue(name, value, ts)
            is Long -> this.addValue(name, value, ts)
            is LongArray -> this.addValue(name, value, ts)
            is Short -> this.addValue(name, value, ts)
            is ShortArray -> this.addValue(name, value, ts)
            is String -> this.addValue(name, value, ts)
            is UByte -> this.addValue(name, value.toUByte(), ts)
            is UInt -> this.addValue(name, value, ts)
            is UShort -> this.addValue(name, value.toUShort(), ts)
            is ULong -> this.addValue(name, value, ts)
            is LinkedHashMap<*, *> -> this.addValue(name, value, ts)
            else -> if (value != null) this.addCustomValue(name, value, ts) else this
        }

    // Stores an array of values to a response
    private fun SourceReadValuesReply.Builder.addArray(value: Iterable<*>, name: String, ts: Instant? = null): SourceReadValuesReply.Builder {

        @Suppress("ReplaceSizeZeroCheckWithIsEmpty")
        if (value.count() == 0) {
            return this
        }

        if (isMixedTypeArray(value)) {
            return this.addCustomValueList(name, value.filterNotNull(), ts)
        }

        // If a list contains a single item the type is a SingletonList which can not be cast into an ArrayList
        val valueAsList = safeAsList(value)


        @Suppress("UNCHECKED_CAST")
        return when (valueAsList.first()) {
            is Boolean -> this.addValue(name, (valueAsList as ArrayList<Boolean>), ts)
            is Byte -> this.addValue(name, (valueAsList as ArrayList<Byte>), ts)
            is Double -> this.addValue(name, (valueAsList as ArrayList<Double>), ts)
            is Float -> this.addValue(name, (valueAsList as ArrayList<Float>), ts)
            is Int -> this.addValue(name, (valueAsList as ArrayList<Int>), ts)
            is Instant -> this.addValue(name, (valueAsList as ArrayList<Instant>))
            is Long -> this.addValue(name, (valueAsList as ArrayList<Long>), ts)
            is Short -> this.addValue(name, (valueAsList as ArrayList<Short>), ts)
            is String -> this.addValue(name, valueAsList as ArrayList<String>, ts)
            is UByte -> this.addValue(name, (valueAsList as ArrayList<UByte>), ts)
            is UInt -> this.addValue(name, (valueAsList as ArrayList<UInt>), ts)
            is ULong -> this.addValue(name, (valueAsList as ArrayList<ULong>), ts)
            is UShort -> this.addValue(name, (valueAsList as ArrayList<UShort>), ts)
            is LinkedHashMap<*, *> -> this.addValue(name, valueAsList as ArrayList<LinkedHashMap<*, *>>, ts)
            else -> addCustomValueList(name, valueAsList.filterNotNull(), ts)
        }

    }

    /**
     * Converts adapter read responses from sources to gRPC reply message
     * @receiver ReadResult The read result to convert into a gRPC reply message
     * @return gRPC ReadValuesReply
     */
    fun ReadResult.asReadValuesReply(compressed: Boolean): ReadValuesReply {
        val readRequestReplyBuilder = ReadValuesReply.newBuilder()
        // map of results indexed by sourceID
        this.forEach { result ->
            val sourceID = result.key
            val readSourcesReply = result.value
            readRequestReplyBuilder.addSources(readSourcesReply.asSourceReadValuesReply(sourceID))
        }
        val readValuesReply = readRequestReplyBuilder.build()

        val compress = (compressed && this.values.map { if (it is SourceReadSuccess) it.values.isNotEmpty() else false }.all { it })
        return if (compress) createCompressedReply(readValuesReply) else readValuesReply
    }

    private fun createCompressedReply(readValuesReply: ReadValuesReply): ReadValuesReply {
        val replyBytes = readValuesReply.toByteArray()
        val compressedStream = ByteArrayOutputStream()

        GzipCompression.compress(replyBytes.inputStream(), compressedStream)
        return ReadValuesReply.newBuilder()
            .setCompressed(ByteString.readFrom(compressedStream.toByteArray().inputStream()))
            .build()
    }

    /**
     * Converts adapter read result for a single source to a gRPC source readValues message
     * @receiver SourceReadResult
     * @param sourceID String
     * @return SourceReadValuesReply?
     */
    private fun SourceReadResult.asSourceReadValuesReply(sourceID: String): SourceReadValuesReply? {
        return when (this) {
            // Read from bridge was successful
            is SourceReadSuccess -> {
                val sourceValuesReply = newSourceReadValuesReplyBuilder(sourceID, Instant.now())
                for (sourceValue in this.values) {
                    try {
                        sourceValuesReply.addAnyValue(sourceValue.key, sourceValue.value.value)
                    } catch (ex: Throwable) {
                        val typeString = if (sourceValue.value.value != null) " of type ${sourceValue.value.value!!::class.java.name}" else ""
                        println("**** Fatal error converting value ${sourceValue.value.value}$typeString for source value \"${sourceValue.key}\" into gRPC ReadValue type ****, $ex")
                    }
                }
                sourceValuesReply.build()
            }
            // Reading from bridge failed
            is SourceReadError -> newSourceReadValuesReplyBuilder(sourceID, Instant.now())
                .putAllValues(emptyMap())
                .setError(this.error).build()
        }
    }


}