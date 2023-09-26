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
import com.amazonaws.sfc.data.JsonHelper.Companion.gsonExtended
import com.amazonaws.sfc.ipc.ChannelValue
import com.amazonaws.sfc.ipc.ReadValuesReply
import com.amazonaws.sfc.ipc.SourceReadValuesReply
import com.google.gson.Gson
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant

/**
 * Extension methods to convert responses from the GRPC source server into native values. These are
 * used by the SFC core to extract the values from replies that are streamed from the protocol server.
 * @see ReadResult
 */
object GrpcSourceValueAsNativeExt {

    // GSON instance for decoding JSON encoded values
    private val gson: Gson by lazy {
        gsonExtended()
    }

    /**
     * Extension method to convert values in GRPC responses into native value
     */
    private val ChannelValue.asNativeValue: Any?
        get() = when (this.valueCase.number) {

            ChannelValue.SIGNED_BYTE_FIELD_NUMBER -> this.signedByte.toByte()
            ChannelValue.SIGNED_BYTE_ARRAY_FIELD_NUMBER -> this.signedByteArray.map { it.toByte() }
            ChannelValue.UNSIGNED_BYTE_FIELD_NUMBER -> this.unsignedByte.toUByte()
            ChannelValue.UNSIGNED_BYTE_ARRAY_FIELD_NUMBER -> this.unsignedByteArray.map { it.toUByte() }

            ChannelValue.SIGNED_INT16_FIELD_NUMBER -> this.signedInt16.toShort()
            ChannelValue.SIGNED_INT32_FIELD_NUMBER -> this.signedInt32
            ChannelValue.SIGNED_INT64_FIELD_NUMBER -> this.signedInt64
            ChannelValue.UNSIGNED_INT16_FIELD_NUMBER -> this.unsignedInt16.toUShort()
            ChannelValue.UNSIGNED_INT32_FIELD_NUMBER -> this.unsignedInt32.toUInt()
            ChannelValue.UNSIGNED_INT64_FIELD_NUMBER -> this.unsignedInt64.toULong()

            ChannelValue.SIGNED_INT16_ARRAY_FIELD_NUMBER -> this.signedInt16Array.itemsList.toList()
            ChannelValue.SIGNED_INT32_ARRAY_FIELD_NUMBER -> this.signedInt32Array.itemsList.toList()
            ChannelValue.SIGNED_INT64_ARRAY_FIELD_NUMBER -> this.signedInt64Array.itemsList.toList()
            ChannelValue.UNSIGNED_INT16_ARRAY_FIELD_NUMBER -> this.unsignedInt16Array.itemsList.map { it.toUShort() }
            ChannelValue.UNSIGNED_INT32_ARRAY_FIELD_NUMBER -> this.unsignedInt32Array.itemsList.map { it.toUInt() }
            ChannelValue.UNSIGNED_INT64_ARRAY_FIELD_NUMBER -> this.unsignedInt64Array.itemsList.map { it.toULong() }

            ChannelValue.BOOL_FIELD_NUMBER -> this.bool
            ChannelValue.BOOL_ARRAY_FIELD_NUMBER -> this.boolArray.itemsList.map { it }

            ChannelValue.FLOAT_FIELD_NUMBER -> this.float
            ChannelValue.DOUBLE_FIELD_NUMBER -> this.double
            ChannelValue.FLOAT_ARRAY_FIELD_NUMBER -> this.floatArray.itemsList.toList()
            ChannelValue.DOUBLE_ARRAY_FIELD_NUMBER -> this.doubleArray.itemsList.toList()

            ChannelValue.STRING_FIELD_NUMBER -> this.string
            ChannelValue.STRING_ARRAY_FIELD_NUMBER -> this.stringArray.itemsList.toList()

            ChannelValue.DATETIME_FIELD_NUMBER -> Instant.ofEpochSecond(this.datetime.seconds, this.datetime.nanos.toLong())
            ChannelValue.DATETIME_ARRAY_FIELD_NUMBER -> this.datetimeArray.timestampList.map { Instant.ofEpochSecond(it.seconds, it.nanos.toLong()) }

            ChannelValue.TYPED_HASH_MAP_FIELD_NUMBER -> {
                this.typedHashMap.entriesMap.map { (k, v) ->
                    k to v.asNativeValue
                }.toMap<String?, Any?>()

            }

            ChannelValue.TYPED_HAS_MAP_ARRAY_FIELD_NUMBER -> {
                this.typedHasMapArray.itemsList.map { m ->
                    m.entriesMap.map { (k, v) ->
                        k to v.asNativeValue
                    }.toMap()
                }
            }

            ChannelValue.CUSTOM_FIELD_NUMBER -> try {
                gson.fromJson(this.custom, Any::class.java)
            } catch (_: Throwable) {
                null
            }

            ChannelValue.CUSTOM_ARRAY_FIELD_NUMBER -> try {
                gson.fromJson(this.customArray, ArrayList<Any>()::class.java)
            } catch (_: Throwable) {
                null
            }

            else -> null
        }

    // Gets timestamp from a GRPC response source into an Instance
    private val SourceReadValuesReply.kotlinTimestamp: Instant?
        get() = if (this.hasTimestamp())
            Instant.ofEpochSecond(this.timestamp.seconds, this.timestamp.nanos.toLong())
        else null

    // Gets timestamp from a GRPC response value into an Instance
    private val ChannelValue.kotlinTimestamp: Instant?
        get() = if (this.hasTimestamp())
            Instant.ofEpochSecond(this.timestamp.seconds, this.timestamp.nanos.toLong())
        else null


    // GRPC response value into SFC channel value
    private val ChannelValue.asReadValue: ChannelReadValue
        get() = ChannelReadValue(this.asNativeValue, this.kotlinTimestamp)


    // GRPC response values into map of SFC channel value
    private val SourceReadValuesReply.asSourceReadValue: SourceReadSuccess
        get() = SourceReadSuccess(
            values = this.valuesMap.map { it.key to it.value.asReadValue }.toMap(),
            timestamp = this.kotlinTimestamp
        )

    /**
     * Extension method to map GRPC response into native SFC ReadResult instance
     */
    val ReadValuesReply.asReadResult: ReadResult
        get() {
            return if (this.compressed.isEmpty)
                this.buildReadResult()
            else
                this.uncompressReply()
        }

    private fun ReadValuesReply.uncompressReply(): ReadResult {
        val replyBytes = ByteArrayInputStream(compressed.toByteArray())
        val bufferSize = 1024 * 32
        val unCompressedReplyBytes = ByteArrayOutputStream(bufferSize)
        ZipCompression.uncompress(replyBytes, unCompressedReplyBytes, bufferSize)
        val unCompressedReply = ReadValuesReply.parseFrom(ByteArrayInputStream(unCompressedReplyBytes.toByteArray()))
        return unCompressedReply.buildReadResult()
    }

    private fun ReadValuesReply.buildReadResult() = ReadResult(this.sourcesList.associate { s: SourceReadValuesReply ->
        val ts = s.kotlinTimestamp
        s.sourceID to (
                if (s.error.isNullOrEmpty()) s.asSourceReadValue
                else
                    SourceReadError(error = s.error, timestamp = ts))
    })
}