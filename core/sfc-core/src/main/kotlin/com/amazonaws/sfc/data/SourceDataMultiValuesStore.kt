// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data

import io.ktor.util.collections.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Data store for multiple values received from data updates or events
 */
open class SourceDataMultiValuesStore<T>(private val maxRetainSize: Int = 0, private val maxRetainPeriod: Int = 0, private val fnLimit: ((String, Duration?, Int?, Boolean) -> Unit)? = null) :
        SourceDataStore<T> {

    val lock = Mutex()

    // the stored data values
    private var values = ConcurrentHashMap<String, ConcurrentLinkedQueue<Pair<T, Instant>>>()

    private var fullChannels = ConcurrentSet<String>()
    private var expiredChannels = ConcurrentSet<String>()

    // adds a value to the store
    override fun add(channelID: String, value: T) {

        runBlocking {
            lock.withLock {

                var list = values[channelID]
                if (list == null) {
                    list = ConcurrentLinkedQueue<Pair<T, Instant>>()
                    values[channelID] = list
                }

                if (maxRetainPeriod > 0) {
                    var first = list.firstOrNull()
                    if (first?.second?.plusMillis(maxRetainPeriod.toLong())?.isBefore(Instant.now()) == true) {
                        if (!expiredChannels.contains(channelID)) {
                            expiredChannels.add(channelID)
                            fnLimit?.invoke(channelID, (maxRetainPeriod.toDuration(DurationUnit.MILLISECONDS)), null, true)
                        }

                        while (first?.second?.plusMillis(maxRetainPeriod.toLong())?.isBefore(Instant.now()) == true) {
                            list.poll()
                            first = list.firstOrNull()
                        }

                    } else{
                        if (expiredChannels.contains(channelID)) {
                            fnLimit?.invoke(channelID, (maxRetainPeriod.toDuration(DurationUnit.MILLISECONDS)), null, false)
                            expiredChannels.remove(channelID)
                        }
                    }
                }


                if (maxRetainSize > 0) {

                    if (list.size >= maxRetainSize) {

                        if (!fullChannels.contains(channelID)) {
                            fullChannels.add(channelID)
                            fnLimit?.invoke(channelID, null, maxRetainSize, true)
                        }
                        while (list.size >= maxRetainSize) {
                            list.poll()
                        }

                    } else {
                        if (fullChannels.contains(channelID)) {
                            fnLimit?.invoke(channelID, null, maxRetainSize, false)
                            fullChannels.remove(channelID)
                        }
                    }
                }

                list.add(value to Instant.now())
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
                val data: Map<String, ConcurrentLinkedQueue<Pair<T, Instant>>> = values.filter {
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
                    it.key to it.value.map { it.first }
                }

            }
        }
    }

}