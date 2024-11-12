// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.data

interface SourceDataStore<T> {
    fun add(channelID: String, value: T)
    fun clear()
    fun read(channels: List<String>?): List<Pair<String, Any>>
}