// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data

import com.amazonaws.sfc.config.BaseConfiguration.Companion.WILD_CARD
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.system.DateTime.systemDateTime
import com.amazonaws.sfc.util.WorkerQueue
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.isJobCancellationException
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import java.io.Closeable
import java.util.concurrent.locks.ReentrantLock
import kotlin.coroutines.CoroutineContext
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
    private val interval: Duration,
    private val logger: Logger
) : Closeable {

    private var initJob: Job? = null
    private val classname = this.javaClass.name

    private val scope = buildScope("IPC Protocol Service Values Flow Handler")

    init {
        // needs to run as coroutine as locking functions are required which can only be used in suspended functions
        initJob = scope.launch(context = Dispatchers.IO, name = "initialize") {
            createSourceReaderLocks(sourceChannels.keys)
        }
    }

    fun sourceReadResults(
        context: CoroutineContext,
        schedule: String?,
        maxConcurrentSourceReads: Int,
        timeout: Duration,
        fnContinue: () -> Boolean
    ): Flow<ReadResult> {

        val log = logger.getCtxLoggers(classname, "SourcesValuesAsFlow")

        // declared here to make it available for flow catch
        var workerQueue: WorkerQueue<Pair<String, List<String>?>, Pair<String, SourceReadResult>>? = null

        if (!fnContinue()) return emptyFlow()

        var initialRead = true

        return flow {
            // wait for initialization has been finished
            if (!fnContinue()) initJob?.join()

            // create map of sources and channels to read
            val sourcesToRead = sourceChannels.map { (sourceID, sourceChannels) ->
                sourceID to sourceChannels.channelList
            }.toMap()


            while (context.isActive) {

                if (!fnContinue()) currentCoroutineContext().cancel()

                workerQueue = WorkerQueue(
                    maxConcurrentSourceReads,
                    context = Dispatchers.Default,
                    logger = logger
                ) { (sourceID, channels) ->


                    try {
                        val taskLogger = logger.getCtxLoggers(classname, "sourceReadResultWorker-$sourceID")
                        // make call to adapter and get the result
                        taskLogger.trace("Start reading ${channels.channelList?.size ?: "all"} channels from source $sourceID")

                        // lock the source to prevent simultaneous reads
                        runBlocking {
                            val start = systemDateTime().toEpochMilli()
                            val result = adapter.read(sourceID, channels)
                            val sourceReadDuration = (systemDateTime().toEpochMilli() - start)
                            taskLogger.trace("Finished reading from source \"$sourceID\", read ${if (result is SourceReadSuccess) "succeeded" else "failed"} and took $sourceReadDuration milliseconds")
                            if (!initialRead && sourceReadDuration > interval.inWholeMilliseconds) {
                                log.warning("Reading from source \"$sourceID\" took ${sourceReadDuration.toDuration(DurationUnit.MILLISECONDS)}, " +
                                        "which is more than the read interval of $interval${if (schedule != null)" for schedule \"$schedule\"" else ""}")
                            }
                            sourceID to result

                        }
                    } catch (e: Exception) {
                        if (!e.isJobCancellationException) {
                            log.errorEx("Error reading from source \"$sourceID\"", e)
                            sourceID to SourceReadError(e.message ?: e.stackTrace.toString(), systemDateTime())
                        } else sourceID to SourceReadSuccess(emptyMap(), systemDateTime())
                    }
                }

                // measure time it takes to handle a read cycle
                val duration = measureTime {
                    // create map, indexed by the sourceID, with deferred read results

                    sourcesToRead.forEach { (sourceID, channels) ->
                        workerQueue?.submit(sourceID to channels)
                    }

                    val result = withTimeoutOrNull(timeout) {
                        workerQueue?.await()?.filterNotNull()?.toMap()
                    }
                    if (result != null) emit(ReadResult(result))

                }

                // wait for next iteration
                if (!initialRead && duration > interval) {
                    log.warning("Read cycle took ${duration.inWholeMilliseconds.toDuration(DurationUnit.MILLISECONDS)}, " +
                            "which is more than read interval of $interval ${if (schedule != null)" for schedule \"$schedule\"" else ""}")

                } else {
                    log.trace("Read cycle took $duration")
                    runBlocking {
                        if (fnContinue()) {
                            delay(interval - duration)
                        }
                    }
                }
                initialRead = false
            }
        }.catch { e ->
            if (!e.isJobCancellationException) {
                log.errorEx("Error reading from sources", e as Exception)
            } else {
                workerQueue?.reset()
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

        private val addSourceLock = ReentrantLock()

        // a lock per source to prevent simultaneous reads on a source
        private val sourceReadLocks: MutableMap<String, ReentrantLock> = mutableMapOf()

        private suspend fun createSourceReaderLocks(sources: Set<String>) {

            coroutineScope {
                launch("Create Reader Locks") {
                    addSourceLock.lock()
                    try {
                        sources.forEach { sourceID ->
                            if (!sourceReadLocks.containsKey(sourceID)) {
                                sourceReadLocks[sourceID] = ReentrantLock()
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

