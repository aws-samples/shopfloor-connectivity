// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.ipc.extensions


import com.amazonaws.sfc.data.SourceOutputData
import com.amazonaws.sfc.data.TargetOutputDataItem
import com.amazonaws.sfc.ipc.SourceValues
import com.amazonaws.sfc.ipc.TargetOutputDataOuterClass
import com.amazonaws.sfc.ipc.extensions.GrpcTargetValueAsNativeExt.asDataChannelOutputData
import java.time.Instant

object GrpcOutputDataAsNative {

     fun TargetOutputDataOuterClass.TargetOutputData.asNative(): List<TargetOutputDataItem> {
        return this.itemsList.map { it.asNative() }
    }

   fun TargetOutputDataOuterClass.TargetOutputDataItem.asNative() = TargetOutputDataItem(

        schedule = this.schedule,
        sources = this.asSourceOutputDataMap(),
        metadata = this.metadata.metadataMap.toMap(),
        serial = this.serial,
        timestamp = if (this.hasTimestamp())
            Instant.ofEpochSecond(this.timestamp.seconds, this.timestamp.nanos.toLong())
        else
            null
    )

    private fun TargetOutputDataOuterClass.TargetOutputDataItem.asSourceOutputDataMap(): Map<String, SourceOutputData> =
        this.valuesMap.map { (sourceName, channelValuesMap: SourceValues) ->
            sourceName to channelValuesMap.asDataChannelOutputData()
        }.toMap()

}