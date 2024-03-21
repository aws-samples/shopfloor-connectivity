// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data

import java.util.concurrent.ConcurrentHashMap

/**
 * Data store for multiple values received from data updates or events
 */
open class SourceDataMultiValuesStore<T> {


    // the stored data values
    private var values = ConcurrentHashMap<String, MutableList<T>>()

    // adds a value to the store
    suspend fun add(channelID: String, value: T) {

        if (values.containsKey(channelID)) {
            values[channelID]?.add(value)
        } else {
            values[channelID] = mutableListOf(value)
        }
    }


    /**
     * Clears all data in the store
     */
    suspend fun clear() {
        values.clear()
    }


    suspend fun read(channels: List<String>?): List<Pair<String, List<T>>> {

        // get the data for the requested channels
        val data: Map<String, List<T>> = values.filter {
            (channels == null || it.key in channels)
        }

        if (channels == null) {
            values.clear()
        } else {
            values.entries.removeIf {
                channels.contains(it.key)
            }
        }

        return data.map {
            it.key to it.value
        }

    }

}