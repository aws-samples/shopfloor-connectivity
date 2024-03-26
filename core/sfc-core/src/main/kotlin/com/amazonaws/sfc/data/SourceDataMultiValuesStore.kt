// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap

/**
 * Data store for multiple values received from data updates or events
 */
open class SourceDataMultiValuesStore<T> {


    // the stored data values
    private var values = ConcurrentHashMap<String, ConcurrentLinkedQueue<T>>()

    // adds a value to the store
    fun add(channelID: String, value: T) {

        val list = values.computeIfAbsent(channelID) { _ -> ConcurrentLinkedQueue<T>() }
        list.add(value)
    }

    val size
        get() = values.size


    /**
     * Clears all data in the store
     */
    fun clear() {
        values.clear()
    }


    fun read(channels: List<String>?): List<Pair<String, List<T>>> {

        // get the data for the requested channels
        val data: Map<String, ConcurrentLinkedQueue<T>> = values.filter {
            (channels == null || it.key in channels)
        }

        if (channels == null) {
            values.clear()
        } else {
            values.entries.removeIf {
                channels.contains(it.key)
            }
        }

        return data.map { it ->
            it.key to it.value.map { it }
        }

    }

}