package com.amazonaws.sfc.util

import com.amazonaws.sfc.log.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

class WorkerQueue<T, R>(
    private val workers: Int,
    capacity: Int = Channel.UNLIMITED,
    context: CoroutineContext = Dispatchers.Default,
    private val logger: Logger?,
    private val task: (T) -> R
) {

    private val queue = Channel<T>(capacity)
    private val done = Channel<R?>(capacity)
    private var jobs: ULong = 0UL

    private val className = this::class.java.name
    private val scope = CoroutineScope(context)

    private var workerJobs: List<Job> = buildWorkers()

    private fun buildWorkers(): List<Job> {
        return List(workers) {
            scope.launch {
                while (isActive) {
                    var result: R? = null
                    val taskInput = queue.receive()
                    try {
                        result = task(taskInput)
                    } catch (e: Exception) {
                        if (logger != null) {
                            logger.getCtxErrorLogEx(className, "worker")("Error in worker", e)
                        }
                    } finally {
                        done.send(result)
                    }
                }
            }
        }
    }

    fun reset() {
        logger?.getCtxTraceLog(className, "reset")?.let { it("Reset workers") }
        workerJobs.forEach { it.cancel() }
        workerJobs = buildWorkers()
    }

    suspend fun submit(taskInput: T) {
        queue.send(taskInput)
        jobs++
    }

    suspend fun await(timeout: Duration = Duration.INFINITE): MutableList<R?> {
        val results = mutableListOf<R?>()
        var count = 0UL
        withTimeout(timeout) {
            while (count < jobs) {
                val result = done.receive()
                count++
                if (result !is Unit)
                    results.add(result)
            }
        }
        return results

    }
}
