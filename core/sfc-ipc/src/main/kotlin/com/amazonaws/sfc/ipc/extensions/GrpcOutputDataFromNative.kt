// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.ipc.extensions

import com.amazonaws.sfc.data.SourceOutputData
import com.amazonaws.sfc.data.TargetData
import com.amazonaws.sfc.ipc.Metadata
import com.amazonaws.sfc.ipc.SourceValues
import com.amazonaws.sfc.ipc.TargetOutputDataOuterClass.TargetOutputData
import com.amazonaws.sfc.ipc.TargetOutputDataOuterClass.TargetOutputDataItem
import com.amazonaws.sfc.ipc.extensions.GrpcTargetValueFromNativeExt.addAggregatedSourceData
import com.amazonaws.sfc.ipc.extensions.GrpcTargetValueFromNativeExt.addSourceData
import com.google.protobuf.Any
import com.google.protobuf.Any.pack
import com.google.protobuf.AnyOrBuilder
import com.google.protobuf.AnyProto
import com.google.protobuf.ByteString
import kotlin.collections.asIterable

object GrpcOutputDataFromNative {

    fun List<TargetData>.asProtobuf() : TargetOutputData{

        val builder = TargetOutputData.newBuilder()

        val items =this.map { it.asProtobuf() }.asIterable()
        this.forEach {
            @Suppress("UNCHECKED_CAST")
            builder.addAllItems(items)
        }
        return builder.build()
    }

    fun TargetData.asProtobuf(): TargetOutputDataItem {

        // The schedule
        val itemBuilder = TargetOutputDataItem.newBuilder()
            .setSchedule(this.schedule)
            .setSerial(this.serial)
            .setTimestamp(newTimestamp(this.timestamp))


        // The data
        this.sources.forEach { (source, sourceData) ->
            itemBuilder.addSourceValues(source, sourceData)
        }

        // Top level metadata
        itemBuilder.metadata = Metadata.newBuilder().putAllMetadata(this.metadata).build()

        val request = itemBuilder.build()

        return request

    }

    // Encode and adds all source vales to the request
    private fun TargetOutputDataItem.Builder.addSourceValues(sourceName: String, sourceData: SourceOutputData): TargetOutputDataItem.Builder {

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

}