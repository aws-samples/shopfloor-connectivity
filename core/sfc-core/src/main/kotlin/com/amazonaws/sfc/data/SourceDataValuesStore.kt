
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Data store for values received from data updates or events
 */
open class SourceDataValuesStore<T> {

    // mutex for exclusive access to store
    private var mutex = Mutex()

    // the stored data values
    private var values = mutableMapOf<String, T>()

    // adds a value to the store
    suspend fun add(channelID: String, value: T) {
        mutex.withLock {
            values[channelID] = value
        }
    }

    /**
     * Clears all data in the store
     */
    suspend fun clear() {
        mutex.withLock {
            values.clear()
        }
    }


    suspend fun read(channels: List<String>?): Sequence<Pair<String, T>> {

        mutex.withLock {

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

            return sequence {
                data.forEach {
                    yield(it.key to it.value)
                }
            }
        }

    }
}