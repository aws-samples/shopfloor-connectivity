/*
 Copyright (c) 2021. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.ipc.extensions


import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.data.JsonHelper.Companion.gsonPretty
import com.amazonaws.sfc.ipc.*
import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant

/**
 * Helper for converting values into GRPC stream request for IPC target servers
 */
object GrpcTargetValueFromNativeExt {

    // used to encode structured type as json
    private val gson by lazy {
        JsonHelper.gsonExtended()
    }


    /**
     * Creates a new WriteValues request
     * @param targetData TargetData The data to send in the request
     * @return WriteValuesRequest
     */
    fun newWriteValuesRequest(targetData: TargetData, compressed: Boolean): WriteValuesRequest {

        // The schedule
        val builder = WriteValuesRequest.newBuilder()
            .setSchedule(targetData.schedule)
            .setSerial(targetData.serial)
            .setCreatedTimestamp(newTimestamp(targetData.timestamp))


        // The data
        targetData.sources.forEach { (source, sourceData) ->
            builder.addSourceValues(source, sourceData)
        }

        // Top level metadata
        builder.metadata = Metadata.newBuilder().putAllMetadata(targetData.metadata).build()

        builder.noBuffering = targetData.noBuffering

        val request = builder.build()

        val compress = (compressed && targetData.sources.map { it.value.channels.isNotEmpty() }.all { it })
        return if (compress) compressRequest(request) else request

    }

    private fun compressRequest(request: WriteValuesRequest): WriteValuesRequest {
        val bufferSize = 1024 * 32
        val unCompressedBytes = ByteArrayInputStream(request.toByteArray())
        val compressedBytes = ByteArrayOutputStream(bufferSize)
        ZipCompression.compress(unCompressedBytes, compressedBytes, bufferSize)
        return WriteValuesRequest.newBuilder()
            .setCompressed(ByteString.readFrom(ByteArrayInputStream(compressedBytes.toByteArray())))
            .build()


    }


    // Encode and adds all source vales to the request
    private fun WriteValuesRequest.Builder.addSourceValues(sourceName: String, sourceData: SourceOutputData): WriteValuesRequest.Builder {

        val sourceDataBuilder = SourceValues.newBuilder()

        if (sourceData.isAggregated) {
            addAggregatedSourceData(sourceData, sourceDataBuilder)
        } else {
            addSourceData(sourceData, sourceDataBuilder)
        }

        if (!sourceData.metadata.isNullOrEmpty()) {
            sourceDataBuilder.metadata = Metadata.newBuilder().putAllMetadata(sourceData.metadata).build()
        }
        this.putValues(sourceName, sourceDataBuilder.build())
        return this
    }

    private fun addAggregatedSourceData(sourceData: SourceOutputData, sourceDataBuilder: SourceValues.Builder) {
        // for every channel (each channel has a map of aggregated values)
        sourceData.channels.forEach { (channelName, channelValue) ->
            val aggregatedChannelValues = channelValue.value as Map<*, *>
            // build a map containing all aggregated values for a channel
            val aggregatedValuesMapBuilder = TargetChannelValuesMap.newBuilder()
            aggregatedChannelValues.forEach { (aggregationName, aggregatedValue) ->
                try {
                    aggregatedValuesMapBuilder.addAnyValue(aggregationName as String, (aggregatedValue as ChannelOutputData))
                } catch (e: Throwable) {
                    println("*** Fatal error converting aggregation value $aggregatedValue for aggregation $aggregationName for channel \"$channelName\" to a gRPC target Value ***")
                }
            }

            // add the aggregated values for the channel
            sourceDataBuilder.aggregatedValuesBuilder.putValues(channelName, aggregatedValuesMapBuilder.build())

            if (!channelValue.metadata.isNullOrEmpty()) {
                val channelMetadata = Metadata.newBuilder().putAllMetadata(channelValue.metadata).build()
                sourceDataBuilder.aggregatedValuesBuilder.putMetadata(channelName, channelMetadata)
            }
        }
        // all list with all channels
        sourceDataBuilder.aggregatedValuesBuilder.build()
    }

    private fun addSourceData(sourceData: SourceOutputData, sourceDataBuilder: SourceValues.Builder) {
        // build a map for values
        val channelValuesMapBuilder = TargetChannelValuesMap.newBuilder()
        // add all values to the map
        sourceData.channels.forEach { (channelName, channelValue) ->
            channelValuesMapBuilder.addAnyValue(channelName, channelValue)
        }

        if (sourceData.timestamp != null) {
            sourceDataBuilder.sourceTimestamp = newTimestamp(sourceData.timestamp!!)
        }
        // build the map
        sourceDataBuilder.values = channelValuesMapBuilder.build()
    }


    // BYTE

    // Adds a list of byte values
    @JvmName("builderForValueValueListByte")
    private fun builderForValue(value: Byte): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder().setSignedByte(value.toInt())

    // Adds an array of byte values
    @JvmName("builderForValueValueListByteArray")
    private fun builderForValue(value: ArrayList<Byte>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder().setSignedByteArray(ByteString.copyFrom(value.toByteArray()))


    // Adds an array of an array of byte values
    @JvmName("builderForValueValueListByteArrayArray")
    private fun builderForValue(value: ArrayList<ArrayList<Byte>>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setSignedByteArrayArray(
                SignedByteArrayArray.newBuilder()
                    .addAllItems(value.map { ByteString.copyFrom(it.toByteArray()) })
                    .build()
            )


    // Adds an unsigned byte value
    // Byte maps to signed int8 in Kotlin
    private fun builderForValueUByte(value: UByte): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder().setUnsignedByte(value.toInt())


    // Adds an array of unsigned byte values
    @JvmName("builderForValueValueUByteArray")
    private fun builderForValue(value: ArrayList<UByte>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setUnsignedByteArray(ByteString.copyFrom(byteArrayOf(*(value.map { it.toByte() }).toByteArray())))


    // Adds an array of an array of unsigned byte values
    @JvmName("builderForValueValueListUByteArrayArray")
    private fun builderForValue(value: ArrayList<ArrayList<UByte>>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setUnsignedByteArrayArray(
                UnsignedByteArrayArray.newBuilder()
                    .addAllItems(value.map { a -> ByteString.copyFrom(byteArrayOf(*(a.map { b -> b.toByte() }).toByteArray())) })
                    .build()
            )


    // INT16

    //Adds a 16-bit integer value
    @JvmName("builderForValueValueShort")
    private fun builderForValue(value: Short): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder().setSignedInt16(value.toInt())


    //Adds an array of 16-bit integer values
    @JvmName("builderForValueValueShortArray")
    private fun builderForValue(value: ArrayList<Short>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setSignedInt16Array(SignedInt16Array.newBuilder().addAllItems(value.map { it.toInt() }))


    //Adds an array of arrays of 16-bit integer values
    @JvmName("builderForValueValueListShortArrayArray")
    private fun builderForValue(value: ArrayList<ArrayList<Short>>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setSignedInt16ArrayArray(
                SignedInt16ArrayArray.newBuilder()
                    .addAllItems(value.map { a -> SignedInt16Array.newBuilder().addAllItems(a.map { it.toInt() }).build() })
                    .build()
            )


    //Adds a 16-bit unsigned integer value
    private fun builderForValueUShort(value: UShort): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder().setUnsignedInt16(value.toInt())


    //Adds an array of 16-bit unsigned integer values
    @JvmName("builderForValueValueUShortArray")
    private fun builderForValue(value: ArrayList<UShort>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setUnsignedInt16Array(UnsignedInt16Array.newBuilder().addAllItems(value.map { it.toInt() }))


    //Adds an array of arrays of 16-bit unsigned integer values
    @JvmName("builderForValueValueListUShortArrayArray")
    private fun builderForValue(value: ArrayList<ArrayList<UShort>>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setUnsignedInt16ArrayArray(
                UnsignedInt16ArrayArray.newBuilder()
                    .addAllItems(value.map { a -> UnsignedInt16Array.newBuilder().addAllItems(a.map { it.toInt() }).build() })
                    .build()
            )


    // INT32

    //Adds a 32-bit integer value
    @JvmName("builderForValueValueInt")
    private fun builderForValue(value: Int): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder().setSignedInt32(value)


    //Adds an array of 32-bit integer values
    @JvmName("builderForValueValueIntArray")
    private fun builderForValue(value: ArrayList<Int>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setSignedInt32Array(SignedInt32Array.newBuilder().addAllItems(value.asIterable()))


    //Adds an array of arrays of 32-bit integer values
    @JvmName("builderForValueValueListIntArrayArray")
    private fun builderForValue(value: ArrayList<ArrayList<Int>>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setSignedInt32ArrayArray(
                SignedInt32ArrayArray.newBuilder()
                    .addAllItems(value.map { a -> SignedInt32Array.newBuilder().addAllItems(a).build() })
                    .build()
            )


    //Adds a 32-bit unsigned integer value
    private fun builderForValueUInt(value: UInt): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder().setUnsignedInt32(value.toInt())


    //Adds an array of 32-bit unsigned integer values
    @JvmName("builderForValueValueUIntArray")
    private fun builderForValue(value: ArrayList<UInt>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setUnsignedInt32Array(UnsignedInt32Array.newBuilder().addAllItems(value.map { it.toInt() }))


    //Adds an array of arrays of 32-bit unsigned integer values
    @JvmName("builderForValueValueListUIntArrayArray")
    private fun builderForValue(value: ArrayList<ArrayList<UInt>>): TargetChannelValue.Builder? =
        TargetChannelValue.newBuilder()
            .setUnsignedInt32ArrayArray(
                UnsignedInt32ArrayArray.newBuilder()
                    .addAllItems(value.map { a -> UnsignedInt32Array.newBuilder().addAllItems(a.map { it.toInt() }).build() })
                    .build()
            )


    // INT64

    //Adds a 64-bit integer value
    @JvmName("builderForValueValueLong")
    private fun builderForValue(value: Long): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder().setSignedInt64(value)


    //Adds an array of 64-bit integer values
    @JvmName("builderForValueValueLongArray")
    private fun builderForValue(value: ArrayList<Long>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setSignedInt64Array(SignedInt64Array.newBuilder().addAllItems(value.asIterable()))


    // Adds an array of arrays of 64-bit integer values
    @JvmName("builderForValueValueListLongArrayArray")
    private fun builderForValue(value: ArrayList<ArrayList<Long>>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setSignedInt64ArrayArray(
                SignedInt64ArrayArray.newBuilder()
                    .addAllItems(value.map { a -> SignedInt64Array.newBuilder().addAllItems(a).build() })
                    .build()
            )


    //Adds a 64-bit unsigned integer value
    private fun builderForValueULong(value: ULong): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setUnsignedInt64(value.toLong())


    // Adds an array of 64-bit unsigned integer values
    @JvmName("builderForValueValueULongArray")
    private fun builderForValue(value: ArrayList<ULong>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setUnsignedInt64Array(UnsignedInt64Array.newBuilder().addAllItems(value.map { it.toLong() }))


    // Adds an array of arrays of 64-bit unsigned integer values
    @JvmName("builderForValueValueListULongArrayArray")
    private fun builderForValue(value: ArrayList<ArrayList<ULong>>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setUnsignedInt64ArrayArray(
                UnsignedInt64ArrayArray.newBuilder()
                    .addAllItems(value.map { a -> UnsignedInt64Array.newBuilder().addAllItems(a.map { it.toLong() }).build() })
                    .build()
            )


    // BOOL

    // Adds a boolean value
    @JvmName("builderForValueValueBool")
    private fun builderForValue(value: Boolean): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder().setBool(value)

    // Adds an array of boolean values
    @JvmName("builderForValueValueBoolArray")
    private fun builderForValue(value: ArrayList<Boolean>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setBoolArray(BoolArray.newBuilder().addAllItems(value.asIterable()))


    // Adds an array of arrays of boolean values
    @JvmName("builderForValueValueListBooleanArray")
    private fun builderForValue(value: ArrayList<ArrayList<Boolean>>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setBoolArrayArray(
                BoolArrayArray.newBuilder()
                    .addAllItems(value.map { a -> BoolArray.newBuilder().addAllItems(a.map { b -> b }).build() })
                    .build()
            )


    // FLOAT

    // Adds a float value
    @JvmName("builderForValueValueFloat")
    private fun builderForValue(value: Float): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder().setFloat(value)


    // Adds an array of float values
    @JvmName("builderForValueValueFloatArray")
    private fun builderForValue(value: ArrayList<Float>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setFloatArray(com.amazonaws.sfc.ipc.FloatArray.newBuilder().addAllItems(value.asIterable()))


    // Adds an array of arrays of float values
    @JvmName("builderForValueValueListFloatArrayArray")
    private fun builderForValue(value: ArrayList<ArrayList<Float>>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setFloatArrayArray(
                FloatArrayArray.newBuilder()
                    .addAllItems(value.map { a -> com.amazonaws.sfc.ipc.FloatArray.newBuilder().addAllItems(a).build() })
                    .build()
            )

    // DOUBLE

    // Adds a double value
    @JvmName("builderForValueValueDouble")
    private fun builderForValue(value: Double): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder().setDouble(value)


    // Adds an array of double values
    @JvmName("builderForValueValueDoubleArray")
    private fun builderForValue(value: ArrayList<Double>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setDoubleArray(com.amazonaws.sfc.ipc.DoubleArray.newBuilder().addAllItems(value.asIterable()))


    // Adds an array of arrays of double values
    @JvmName("builderForValueValueListDoubleArrayArray")
    private fun builderForValue(value: ArrayList<ArrayList<Double>>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setDoubleArrayArray(
                DoubleArrayArray.newBuilder()
                    .addAllItems(value.map { a -> com.amazonaws.sfc.ipc.DoubleArray.newBuilder().addAllItems(a).build() })
                    .build()
            )


    // STRING

    // Adds a string value
    @JvmName("builderForValueValueString")
    private fun builderForValue(value: String): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder().setString(value)


    // Adds an array of string values
    @JvmName("builderForValueValueStringArray")
    private fun builderForValue(value: ArrayList<String>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setStringArray(StringArray.newBuilder().addAllItems(value.asIterable()))

    // Adds an array of arrays of string values
    @JvmName("builderForValueValueListStringArrayArray")
    private fun builderForValue(value: ArrayList<ArrayList<String>>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setStringArrayArray(
                StringArrayArray.newBuilder()
                    .addAllItems(value.map { a -> StringArray.newBuilder().addAllItems(a).build() })
                    .build()
            )


    // Timestamp/Instant

    // Adds a timestamp value
    @JvmName("builderForValueValueInstant")
    private fun builderForValue(value: Instant): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setTimestamp(Timestamp.newBuilder().setSeconds(value.epochSecond).setNanos(value.nano).build())


    // Adds an array of timestamp values
    @JvmName("builderForValueValueInstantArray")
    private fun builderForValue(value: ArrayList<Instant>): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder()
            .setTimestampArray(
                TimestampArray.newBuilder().addAllTimestamp(value.map { Timestamp.newBuilder().setSeconds(it.epochSecond).setNanos(it.nano).build() })
                    .build()
            )


    // Adds an array of arrays of timestamp values
    private fun addCustomValue(value: Any): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder().setCustom(gsonPretty().toJson(value))


    // Adds a structured values type encoded as JSON
    private fun addCustomValueList(value: Any): TargetChannelValue.Builder =
        TargetChannelValue.newBuilder().setCustomArray(gson.toJson(value))

    // Adds an array of structured values type encoded as JSON
    private fun builderForValue(value: LinkedHashMap<*, *>): TargetChannelValue.Builder {

        return TargetChannelValue.newBuilder()
            .setTypedHashMap(targetValueTypedMap(value))

    }

    private fun targetValueTypedMap(value: LinkedHashMap<*, *>): TypedTargetChannelValueMap.Builder? =
        TypedTargetChannelValueMap.newBuilder()
            .putAllEntries(
                sequence {
                    value.forEach { (k, v) ->
                        val targetChannelValue = builderForAnyValue(ChannelOutputData(v))?.build()
                        if (targetChannelValue != null) yield(k.toString() to targetChannelValue)
                    }
                }.toMap())


    // Adds an array of arrays of structured values type encoded as JSON
    private fun builderForValue(value: ArrayList<LinkedHashMap<*, *>>): TargetChannelValue.Builder {

        val valuesList = sequence {
            value.forEach {
                val targetValue = targetValueTypedMap(it)?.build()
                if (targetValue != null) yield(targetValue)
            }
        }.toList()

        return TargetChannelValue.newBuilder()
            .setTypedHashMapArray(
                TypedTargetChannelValueMapArray.newBuilder()
                    .addAllItems(valuesList))
    }


    // Add a value, which can be a single value, an array of an array of arrays to the request builder values
    private fun TargetChannelValuesMap.Builder.addAnyValue(name: String, channelOutput: ChannelOutputData): TargetChannelValuesMap.Builder {

        if (channelOutput.value == null) {
            return this
        }

        val builder: TargetChannelValue.Builder? = builderForAnyValue(channelOutput)

        if (builder != null) {
            if (channelOutput.timestamp != null) {
                val ts = Timestamp.newBuilder().setSeconds(channelOutput.timestamp!!.epochSecond).setNanos(channelOutput.timestamp!!.nano).build()
                builder.valueTimestamp = ts


            }
            if (channelOutput.metadata != null) {
                builder.metadata = Metadata.newBuilder().putAllMetadata(channelOutput.metadata).build()
                //metadataMap.putAll(channelOutput.metadata!!)
            }
            this.putValue(name, builder.build())
        }
        return this

    }

    // Add a value, which can be a single value, an array of an array of arrays to the request builder values
    private fun builderForArray(value: Iterable<*>): TargetChannelValue.Builder? {

        @Suppress("ReplaceSizeZeroCheckWithIsEmpty")
        if (value.count() == 0) {
            return null
        }

        if (isMixedTypeArray(value)) {
            return addCustomValueList(value)
        }

        // To handle singletonList if there is only single value in the list
        val valueAsArray = if (value.count() == 1) arrayListOf(value.first()) else value

        @Suppress("UNCHECKED_CAST")
        return when (valueAsArray.first()) {
            is Byte -> builderForValue(valueAsArray as ArrayList<Byte>)
            is UByte -> builderForValue(valueAsArray as ArrayList<UByte>)
            is Short -> builderForValue(valueAsArray as ArrayList<Short>)
            is UShort -> builderForValue(valueAsArray as ArrayList<UShort>)
            is Int -> builderForValue(valueAsArray as ArrayList<Int>)
            is UInt -> builderForValue(valueAsArray as ArrayList<UInt>)
            is Long -> builderForValue(valueAsArray as ArrayList<Long>)
            is ULong -> builderForValue(valueAsArray as ArrayList<ULong>)
            is Boolean -> builderForValue(valueAsArray as ArrayList<Boolean>)
            is Float -> builderForValue(valueAsArray as ArrayList<Float>)
            is Double -> builderForValue(valueAsArray as ArrayList<Double>)
            is String -> builderForValue(valueAsArray as ArrayList<String>)
            is Instant -> builderForValue(valueAsArray as ArrayList<Instant>)
            is ChannelOutputData -> builderForValue(valueAsArray as ArrayList<ChannelOutputData>)
            is LinkedHashMap<*, *> -> builderForValue((valueAsArray as ArrayList<LinkedHashMap<*, *>>))
            is Iterable<*> -> builderForNestedArray(valueAsArray as ArrayList<ArrayList<*>>)
            else -> addCustomValueList(valueAsArray)
        }
    }

    @JvmName("builderForValueChanelDataOutputList")
    private fun builderForValue(channelOutputDataList: ArrayList<ChannelOutputData>): TargetChannelValue.Builder? {
        val builder = TargetChannelValue.newBuilder()
        val listBuilder = TargetChannelValuesList.newBuilder()
        channelOutputDataList.forEach { v ->
            val itemBuilder: TargetChannelValue.Builder? = builderForAnyValue(v)
            if (itemBuilder != null) {
                if (v.timestamp != null) {
                    itemBuilder.valueTimestamp = newTimestamp(v.timestamp!!)
                }
                listBuilder.addItems(itemBuilder)
            }
        }
        builder.valuesList = listBuilder.build()
        return builder

    }

    private fun builderForAnyValue(channelOutput: ChannelOutputData): TargetChannelValue.Builder? {
        val builder: TargetChannelValue.Builder? = when (channelOutput.value) {
            is Iterable<*> -> builderForArray(channelOutput.value as Iterable<*>)
            is Byte -> builderForValue(channelOutput.value as Byte)
            is UByte -> builderForValueUByte(channelOutput.value as UByte)
            is Short -> builderForValue(channelOutput.value as Short)
            is UShort -> builderForValueUShort(channelOutput.value as UShort)
            is Int -> builderForValue(channelOutput.value as Int)
            is UInt -> builderForValueUInt(channelOutput.value as UInt)
            is Long -> builderForValue(channelOutput.value as Long)
            is ULong -> builderForValueULong(channelOutput.value as ULong)
            is Boolean -> builderForValue(channelOutput.value as Boolean)
            is Float -> builderForValue(channelOutput.value as Float)
            is Double -> builderForValue(channelOutput.value as Double)
            is String -> builderForValue(channelOutput.value as String)
            is Instant -> builderForValue(channelOutput.value as Instant)
            is LinkedHashMap<*, *> -> builderForValue(channelOutput.value as LinkedHashMap<*, *>)
            else -> addCustomValue(channelOutput.value!!)
        }
        return builder
    }


    // Add a value, that is an array of an array of arrays to the request builder values
    private fun builderForNestedArray(value: ArrayList<ArrayList<*>>): TargetChannelValue.Builder? {
        if (value[0].size == 0) {
            return null
        }

        if (isMixedTypeArray(value) || value.any { isMixedTypeArray(it) }) {
            return addCustomValueList(value)
        }

        @Suppress("UNCHECKED_CAST")
        return when (value[0][0]) {
            is Byte -> builderForValue(arrayListOf(*value.map { it as ArrayList<Byte> }.toTypedArray()))
            is UByte -> builderForValue(arrayListOf(*value.map { it as ArrayList<UByte> }.toTypedArray()))
            is Short -> builderForValue(arrayListOf(*value.map { it as ArrayList<Short> }.toTypedArray()))
            is UShort -> builderForValue(arrayListOf(*value.map { it as ArrayList<UShort> }.toTypedArray()))
            is Int -> builderForValue(arrayListOf(*value.map { it as ArrayList<Int> }.toTypedArray()))
            is UInt -> builderForValue(arrayListOf(*value.map { it as ArrayList<UInt> }.toTypedArray()))
            is Long -> builderForValue(arrayListOf(*value.map { it as ArrayList<Long> }.toTypedArray()))
            is ULong -> builderForValue(arrayListOf(*value.map { it as ArrayList<ULong> }.toTypedArray()))
            is Boolean -> builderForValue(arrayListOf(*value.map { it as ArrayList<Boolean> }.toTypedArray()))
            is Float -> builderForValue(arrayListOf(*value.map { it as ArrayList<Float> }.toTypedArray()))
            is Double -> builderForValue(arrayListOf(*value.map { it as ArrayList<Double> }.toTypedArray()))
            is String -> builderForValue(arrayListOf(*value.map { it as ArrayList<String> }.toTypedArray()))
            else -> addCustomValueList(value)
        }
    }


    /**
     * Tests if a list of values contains values of different data types
     * @param value ArrayList<*> List of values to test
     * @return Boolean
     */
    fun isMixedTypeArray(value: Iterable<*>): Boolean {

        if (value.count() <= 1) {
            return false
        }

        return try {
            val t = value.iterator().next()!!::class.java
            value.iterator().asSequence().any { it != null && it::class.java != t }
        } catch (_: Throwable) {
            true
        }
    }
}


