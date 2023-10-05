/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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