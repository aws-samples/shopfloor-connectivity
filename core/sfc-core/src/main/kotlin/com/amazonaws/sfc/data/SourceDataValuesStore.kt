// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock


/**
 * Data store for values received from data updates or events
 */

open class SourceDataValuesStore<T>() {

    private var values = ConcurrentHashMap<String, T>()

    fun add(channelID: String, value: T) {
        values[channelID] = value
    }

    val size
        get() = values.size

    fun read(channels: List<String>?): List<Pair<String, T>> {

        if (values.isEmpty()) return emptyList()


            // get the data for the requested channels
            val data: Map<String, T> = values.filter {
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


    fun clear() {
        values.clear()
    }
}

