package com.amazonaws.sfc.util

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

class WorkerQueue<T, R>(workers: Int, capacity: Int = Channel.UNLIMITED, context: CoroutineContext = Dispatchers.Default, task: (T) -> R) {

    private val queue = Channel<T>(capacity)
    private val done = Channel<R?>(capacity)
    private var jobs : ULong= 0UL

    private val scope = CoroutineScope(context)

    private val workers = List<Job>(workers) {
        scope.launch {
            while (isActive) {
                var result : R? = null
                val taskInput = queue.receive()
                try {
                    result = task(taskInput)
                } catch (e: Exception) {
                    println(e)
                } finally {
                    done.send(result)
                }
            }
        }
    }

    suspend fun submit(taskInput: T) {
        queue.send(taskInput)
        jobs++
    }


    suspend fun await(timeout: Duration = Duration.INFINITE): MutableList<R?> {
        val results = mutableListOf<R?>()
        try {

            var count = 0UL
            withTimeout(timeout) {
                while(count < jobs) {
                    val result = done.receive()
                    count++
                    if (result !is Unit)
                        results.add(result)
                }
            }
        } finally {
            workers.forEach { it.cancel() }
            scope.cancel()
        }
        return results

    }
}