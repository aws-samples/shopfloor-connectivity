/*
 Copyright (c) 2021. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

@file:Suppress("unused")

package com.amazonaws.sfc.ipc.extensions

import com.amazonaws.sfc.data.ChannelOutputData
import com.amazonaws.sfc.data.JsonHelper.Companion.gsonExtended
import com.amazonaws.sfc.data.SourceOutputData
import com.amazonaws.sfc.data.TargetData
import com.amazonaws.sfc.data.ZipCompression
import com.amazonaws.sfc.ipc.SourceValues
import com.amazonaws.sfc.ipc.TargetChannelValue
import com.amazonaws.sfc.ipc.TargetChannelValue.VALUES_LIST_FIELD_NUMBER
import com.amazonaws.sfc.ipc.TargetChannelValuesMap
import com.amazonaws.sfc.ipc.WriteValuesRequest
import com.google.gson.Gson
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant

/**
 * Helper to convert GRPC requests that were streamed to the target server into native SourceReadValues.
 */
object GrpcTargetValueAsNativeExt {

    // used to decode json encoded values
    private val gson: Gson by lazy {
        gsonExtended()
    }

    /**
     * Converts a value from the GRPC request into native value
     */
    private val TargetChannelValue.asNativeValue: Any?
        get() = when (this.valueCase.number) {

            TargetChannelValue.SIGNED_BYTE_FIELD_NUMBER -> this.signedByte.toByte()
            TargetChannelValue.SIGNED_BYTE_ARRAY_FIELD_NUMBER -> this.signedByteArray.map { it.toByte() }
            TargetChannelValue.SIGNED_BYTE_ARRAY_ARRAY_FIELD_NUMBER -> this.signedByteArrayArray.itemsList.map { a -> a.map { b -> b.toByte() } }

            TargetChannelValue.UNSIGNED_BYTE_FIELD_NUMBER -> this.unsignedByte.toUByte()
            TargetChannelValue.UNSIGNED_BYTE_ARRAY_FIELD_NUMBER -> this.unsignedByteArray.map { it.toUByte() }
            TargetChannelValue.UNSIGNED_BYTE_ARRAY_ARRAY_FIELD_NUMBER -> this.unsignedByteArrayArray.itemsList.map { a -> a.map { b -> b.toUByte() } }

            TargetChannelValue.SIGNED_INT16_FIELD_NUMBER -> this.signedInt16.toShort()
            TargetChannelValue.SIGNED_INT32_FIELD_NUMBER -> this.signedInt32
            TargetChannelValue.SIGNED_INT64_FIELD_NUMBER -> this.signedInt64
            TargetChannelValue.UNSIGNED_INT16_FIELD_NUMBER -> this.unsignedInt16.toUShort()
            TargetChannelValue.UNSIGNED_INT32_FIELD_NUMBER -> this.unsignedInt32.toUInt()
            TargetChannelValue.UNSIGNED_INT64_FIELD_NUMBER -> this.unsignedInt64.toULong()

            TargetChannelValue.SIGNED_INT16_ARRAY_FIELD_NUMBER -> this.signedInt16Array.itemsList.toList()
            TargetChannelValue.SIGNED_INT32_ARRAY_FIELD_NUMBER -> this.signedInt32Array.itemsList.toList()
            TargetChannelValue.SIGNED_INT64_ARRAY_FIELD_NUMBER -> this.signedInt64Array.itemsList.toList()
            TargetChannelValue.UNSIGNED_INT16_ARRAY_FIELD_NUMBER -> this.unsignedInt16Array.itemsList.map { it.toUShort() }
            TargetChannelValue.UNSIGNED_INT32_ARRAY_FIELD_NUMBER -> this.unsignedInt32Array.itemsList.map { it.toUInt() }
            TargetChannelValue.UNSIGNED_INT64_ARRAY_FIELD_NUMBER -> this.unsignedInt64Array.itemsList.map { it.toULong() }

            TargetChannelValue.SIGNED_INT16ARRAY_ARRAY_FIELD_NUMBER -> this.signedInt16ArrayArray.itemsList.map { a -> a.itemsList.toList() }
            TargetChannelValue.SIGNED_INT32ARRAY_ARRAY_FIELD_NUMBER -> this.signedInt32ArrayArray.itemsList.map { a -> a.itemsList.toList() }
            TargetChannelValue.SIGNED_INT64ARRAY_ARRAY_FIELD_NUMBER -> this.signedInt64ArrayArray.itemsList.map { a -> a.itemsList.toList() }
            TargetChannelValue.UNSIGNED_INT16_ARRAY_ARRAY_FIELD_NUMBER -> this.unsignedInt16ArrayArray.itemsList.map { a -> a.itemsList.toList() }
            TargetChannelValue.UNSIGNED_INT32_ARRAY_ARRAY_FIELD_NUMBER -> this.unsignedInt32ArrayArray.itemsList.map { a -> a.itemsList.toList() }
            TargetChannelValue.UNSIGNED_INT64_ARRAY_ARRAY_FIELD_NUMBER -> this.unsignedInt64ArrayArray.itemsList.map { a -> a.itemsList.toList() }

            TargetChannelValue.BOOL_FIELD_NUMBER -> this.bool
            TargetChannelValue.BOOL_ARRAY_FIELD_NUMBER -> this.boolArray.itemsList.map { it }
            TargetChannelValue.BOOL_ARRAY_ARRAY_FIELD_NUMBER -> this.boolArrayArray.itemsList.map { a -> a.itemsList.toList() }

            TargetChannelValue.FLOAT_FIELD_NUMBER -> this.float
            TargetChannelValue.DOUBLE_FIELD_NUMBER -> this.double
            TargetChannelValue.FLOAT_ARRAY_FIELD_NUMBER -> this.floatArray.itemsList.toList()
            TargetChannelValue.DOUBLE_ARRAY_FIELD_NUMBER -> this.doubleArray.itemsList.toList()

            TargetChannelValue.FLOAT_ARRAY_ARRAY_FIELD_NUMBER -> this.floatArrayArray.itemsList.map { a -> a.itemsList.toList() }
            TargetChannelValue.DOUBLE_ARRAY_ARRAY_FIELD_NUMBER -> this.doubleArrayArray.itemsList.map { a -> a.itemsList.toList() }

            TargetChannelValue.STRING_FIELD_NUMBER -> this.string
            TargetChannelValue.STRING_ARRAY_FIELD_NUMBER -> this.stringArray.itemsList.toList()
            TargetChannelValue.STRING_ARRAY_ARRAY_FIELD_NUMBER -> this.stringArrayArray.itemsList.map { a -> a.itemsList.toList() }

            TargetChannelValue.TIMESTAMP_FIELD_NUMBER -> Instant.ofEpochSecond(this.timestamp.seconds, this.timestamp.nanos.toLong())
            TargetChannelValue.TIMESTAMP_ARRAY_FIELD_NUMBER -> this.timestampArray.timestampList.map { a -> Instant.ofEpochSecond(a.seconds, a.nanos.toLong()) }

            TargetChannelValue.TYPED_HASH_MAP_FIELD_NUMBER -> {
                typedHashMap.entriesMap.map { (k, v) ->
                    k to v.asNativeValue
                }.toMap<String?, Any?>()
            }

            TargetChannelValue.TYPED_HASH_MAP_ARRAY_FIELD_NUMBER -> {
                val listOfMaps = this.typedHashMapArray.itemsList
                listOfMaps.map { m ->
                    m.entriesMap.map { (k, v) ->
                        k to v.asNativeValue
                    }.toMap<String?, Any?>()
                }

            }

            TargetChannelValue.CUSTOM_FIELD_NUMBER -> try {
                gson.fromJson(this.custom, Any::class.java)
            } catch (_: Throwable) {
                null
            }

            TargetChannelValue.CUSTOM_ARRAY_FIELD_NUMBER -> try {
                gson.fromJson(this.customArray, ArrayList<Any>()::class.java)
            } catch (_: Throwable) {
                null
            }

            VALUES_LIST_FIELD_NUMBER -> {
                this.valuesList.itemsList.map { i: TargetChannelValue ->
                    val value: Any? = i.asNativeValue
                    ChannelOutputData(
                        value = value,
                        timestamp = if (i.hasValueTimestamp()) Instant.ofEpochSecond(i.valueTimestamp.seconds, i.valueTimestamp.nanos.toLong()) else null,
                        metadata = if (i.metadata.metadataMap.isNotEmpty()) i.metadata.metadataMap.toMap() else null
                    )
                }
            }

            else -> null
        }


    /**
     * Extracts the data read from the sources from a received WriteValues request
     * @receiver WriteValuesRequest
     * @return Mapping<String, SourceOutputData>
     */
    private fun WriteValuesRequest.asSourceOutputDataMap(): Map<String, SourceOutputData> =
        this.valuesMap.map { (sourceName, channelValuesMap: SourceValues) ->
            sourceName to channelValuesMap.asDataChannelOutputData()
        }.toMap()


    /**
     * Creates a TargetData instance from a WriteRequest
     * @receiver WriteValuesRequest
     * @return TargetData
     */
    fun WriteValuesRequest.asTargetData(): TargetData =
        if (this.compressed.isEmpty)
            this.buildTargetData()
        else
            this.uncompressTargetData()


    private fun WriteValuesRequest.uncompressTargetData(): TargetData {
        val compressedBytes = ByteArrayInputStream(this.compressed.toByteArray())
        val uncompressedBytes = ByteArrayOutputStream(1024 * 32)
        ZipCompression.uncompress(compressedBytes, uncompressedBytes, 1024 * 32)
        val request = WriteValuesRequest.parseFrom(uncompressedBytes.toByteArray())
        return request.buildTargetData()
    }

    private fun WriteValuesRequest.buildTargetData() = TargetData(
        schedule = this.schedule ?: "",
        sources = this.asSourceOutputDataMap(),
        metadata = this.metadata.metadataMap.toMap(),
        serial = this.serial,
        noBuffering = this.noBuffering,
        timestamp = if (this.hasCreatedTimestamp())
            Instant.ofEpochSecond(this.createdTimestamp.seconds, this.createdTimestamp.nanos.toLong())
        else
            Instant.MIN
    )


    /**
     * Extracts the data from a single source into a native structure
     * @receiver SourceValues Source values with optional metadata
     * @return SourceOutputData Received source data as a native structure
     */
    private fun SourceValues.asDataChannelOutputData(): SourceOutputData {
        // Data can be stored as "just the value"

        val isAggregatedData = this.hasAggregatedValues()

        val channelData =
            if (isAggregatedData) this.aggregatedValues.valuesMap.map { aggregatedValue ->
                aggregatedValue.key to ChannelOutputData(
                    value = aggregatedValue.value.asDataChannelOutputData().toMap(),
                    metadata = this.aggregatedValues.metadataMap[aggregatedValue.key]?.metadataMap?.toMap()
                )
            }.toMap()
            else
                this.values.asDataChannelOutputData()


        return SourceOutputData(
            channels = channelData,
            timestamp = if (this.hasSourceTimestamp()) Instant.ofEpochSecond(this.sourceTimestamp.seconds, this.sourceTimestamp.nanos.toLong()) else null,
            metadata = this.metadata.metadataMap,
            isAggregated = isAggregatedData
        )
    }

    /**
     * Extracts the data read from a channel into a map. If the data contains just a value it is stored directly in the map under
     * a node what has the name of the channel. If the value has additional metadata or a timestamp the values, timestamp and metadata
     * are stored in a sub map with an entry for the value, timestamp and metadata. The names used for the sub-nodes can be configured in
     * the "Elements" section in the configuration root.
     *
     * Just value for a channel named "a"
     *
     * Node "a" -> <the value>
     *
     * Value plus timestamp and/or metadata
     *
     * Node "a"  -> Node "value" -> <the value>
     *              Node "timestamp" -> <the timestamp>
     *              Node "metadata -> <the metadata>
     *
     * @receiver TargetChannelValuesMap Value data from request
     * @return Mapping<String, ChannelOutputData> Mapping with channel value and  optional timestamp and metadata
     */
    private fun TargetChannelValuesMap.asDataChannelOutputData(): Map<String, ChannelOutputData> =

        this.valueMap.map { channelValue: Map.Entry<String, TargetChannelValue> ->

            // Add timestamp if available
            val ts = if (channelValue.value.hasValueTimestamp())
                Instant.ofEpochSecond(channelValue.value.valueTimestamp.seconds, channelValue.value.valueTimestamp.nanos.toLong()) else null

            // Add metadata if available
            val md = if (channelValue.value.metadata != null && channelValue.value.metadata.metadataCount > 0)
                channelValue.value.metadata.metadataMap.toMap() else null

            channelValue.key to ChannelOutputData(channelValue.value.asNativeValue, ts, md)

        }.toMap()

}


