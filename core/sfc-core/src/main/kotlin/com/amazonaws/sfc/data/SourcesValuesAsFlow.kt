/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.data

import com.amazonaws.sfc.config.BaseConfiguration.Companion.WILD_CARD
import com.amazonaws.sfc.system.DateTime.systemDateTime
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import java.io.Closeable
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import kotlin.time.toDuration

/**
 * Reads values from a protocol source adapter with a specified interval. The values are returned as a flow of data
 * that can be consumed from the sourceReadResults property
 * @property adapter ProtocolAdapter
 * @property sourceChannels Mapping<String, List<String>>
 * @property interval Duration
 * @constructor
 */
class SourcesValuesAsFlow(
    private val adapter: ProtocolAdapter,
    private val sourceChannels: Map<String, List<String>>,
    private val interval: Duration) : Closeable {

    private var initJob: Job? = null

    private val scope = buildScope("IPC Protocol Service Values Flow Handler")

    init {
        // needs to run as coroutine as locking functions are required which can only be used in suspended functions
        initJob = scope.launch("initialize") {
            createSourceReaderLocks(sourceChannels.keys)
        }
    }

    // read values returned as a flow
    val sourceReadResults: Flow<ReadResult> by lazy {


        flow {
            // wait for initialization has been finished
            initJob?.join()

            // create map of sources and channels to read
            val sourcesToRead = sourceChannels.map { (sourceID, sourceChannels) ->
                sourceID to sourceChannels.channelList
            }.toMap()

            var cancelled = false

            while (!cancelled) {

                // measure time it takes to handle a read cycle
                val duration = measureTime {

                    // create map, indexed by the sourceID, with deferred read results
                    val deferredResponses: Map<String, Deferred<SourceReadResult>> = sourcesToRead.map {
                        val sourceID = it.key
                        val channels = it.value
                        sourceID to scope.async {
                            sourceReadLocks[sourceID]?.lock()
                            try {
                                // make call to adapter and get the result
                                adapter.read(sourceID, channels)

                            } catch (e: Throwable) {
                                // error making the actual call to the protocol adapter
                                SourceReadError(e.message ?: e.stackTrace.toString(), systemDateTime())
                            } finally {
                                sourceReadLocks[sourceID]?.unlock()
                            }

                        }
                    }.toMap()

                    // wait for deferred read calls and create map of returned responses
                    val channelResults = deferredResponses.map { resp ->
                        resp.key to resp.value.await()
                    }.toMap()

                    // emit result in flow
                    try {
                        emit(ReadResult(channelResults))
                    } catch (e: Throwable) {
                        cancelled = true
                    }
                }
                // wait for next iteration
                delay(interval - duration)
            }
        }
    }

    private val List<String>?.channelList: List<String>?
        get() = if (this.isNullOrEmpty() || this[0] == WILD_CARD) null else this.toSet().toList()


    override fun close() {
        runBlocking {
            adapter.stop(1.toDuration(DurationUnit.SECONDS))
        }
    }

    companion object {

        private val addSourceLock = Mutex()

        // a lock per source to prevent simultaneous reads on a source
        private val sourceReadLocks: MutableMap<String, Mutex> = mutableMapOf()

        private suspend fun createSourceReaderLocks(sources: Set<String>) {

            coroutineScope {
                launch("Create Reader Locks") {
                    addSourceLock.lock()
                    try {
                        sources.forEach { sourceID ->
                            if (!sourceReadLocks.containsKey(sourceID)) {
                                sourceReadLocks[sourceID] = Mutex()
                            }
                        }
                    } finally {
                        addSourceLock.unlock()
                    }
                }
            }
        }
    }
}

