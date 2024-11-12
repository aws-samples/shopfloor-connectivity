// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Data store for multiple values received from data updates or events
 */
open class SourceDataMultiValuesStore<T> : SourceDataStore<T>{

    val lock = Mutex()

    // the stored data values
    private var values = ConcurrentHashMap<String, ConcurrentLinkedQueue<T>>()

    // adds a value to the store
    override fun add(channelID: String, value: T) {

        runBlocking {
            lock.withLock {
                val list = values.computeIfAbsent(channelID) { _ -> ConcurrentLinkedQueue<T>() }
                list.add(value)
            }
        }
    }

    val size
        get() = values.size


    /**
     * Clears all data in the store
     */
    override fun clear() {
        runBlocking {
            lock.withLock {
                values.clear()
            }
        }
    }

   override fun read(channels: List<String>?): List<Pair<String, List<T>>> {

        return runBlocking {
            lock.withLock {

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

                return@runBlocking data.map { it ->
                    it.key to it.value.map { it }
                }

            }
        }
    }

}