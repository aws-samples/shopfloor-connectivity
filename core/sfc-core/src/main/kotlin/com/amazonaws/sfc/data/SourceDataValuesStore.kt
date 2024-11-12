// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap


/**
 * Data store for values received from data updates or events
 */

open class SourceDataValuesStore<T> : SourceDataStore<T> {

    val lock = Mutex()

    private var values = ConcurrentHashMap<String, T>()

    override fun add(channelID: String, value: T) {
        runBlocking {
            lock.withLock {
                values[channelID] = value
            }
        }
    }

    val size
        get() = values.size

    override fun read(channels: List<String>?): List<Pair<String, Any>> {

        return runBlocking {
            lock.withLock {

                if (values.isEmpty()) return@runBlocking emptyList()


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

                return@runBlocking data.map {
                    it.key to it.value as Any
                }
            }
        }

    }


    override fun clear() {
        runBlocking {
            lock.withLock {
                values.clear()
            }
        }
    }
}

