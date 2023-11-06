
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.storeforward

import com.amazonaws.sfc.data.TargetData
import com.amazonaws.sfc.storeforward.config.MessageBufferConfiguration
import kotlin.time.Duration


interface MessageBuffer {
    suspend fun add(targetID: String, targetData: TargetData)
    suspend fun add(targetID: String, targetDataList: Iterable<TargetData>)
    suspend fun remove(targetID: String, serial: String)
    suspend fun remove(targetID: String, serials: Iterable<String>)
    suspend fun size(targetID: String): Long
    suspend fun count(targetID: String): Int
    suspend fun listAfter(targetID: String, bufferConfig: MessageBufferConfiguration): Sequence<TargetData?>
    suspend fun list(targetID: String, fifo: Boolean): Sequence<TargetData?>
    suspend fun first(targetID: String, retainPeriod: Duration?): TargetData?
    suspend fun last(targetID: String): TargetData?
    suspend fun clear(targetID: String)
    suspend fun cleanBySize(targetID: String, bufferConfig: MessageBufferConfiguration): Pair<Int, Long>
    suspend fun cleanByAge(targetID: String, bufferConfig: MessageBufferConfiguration): Int
    suspend fun cleanByNumber(targetID: String, bufferConfig: MessageBufferConfiguration): Int

}