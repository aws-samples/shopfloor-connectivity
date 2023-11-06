
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.util

import com.amazonaws.sfc.log.Logger
import kotlinx.coroutines.delay
import java.util.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class RetryUtils {
    class RetryConfig(val initialRetryInterval: Duration = DEFAULT_INITIAL_RETRY_INTERVAL,
                      val maxRetryInterval: Duration = DEFAULT_MAX_RETRY_INTERVAL,
                      val maxAttempt: Int = DEFAULT_MAX_ATTEMPT,
                      val retryableExceptions: List<Class<*>> = emptyList())

    companion object {

        private val className = this::class.java.simpleName

        val DEFAULT_INITIAL_RETRY_INTERVAL = 1.toDuration(DurationUnit.SECONDS)
        val DEFAULT_MAX_RETRY_INTERVAL = 1.toDuration(DurationUnit.MINUTES)
        const val DEFAULT_MAX_ATTEMPT = 10

        private val random = Random()

        suspend fun <T> runWithRetry(
            retryConfig: RetryConfig,
            taskDescription: String,
            logger: Logger,
            block: () -> T): T {

            var retryInterval: Long = retryConfig.initialRetryInterval.inWholeMilliseconds
            var attempt = 1
            var lastException: Exception? = null
            while (attempt <= retryConfig.maxAttempt) {
                if (Thread.currentThread().isInterrupted) {
                    throw InterruptedException("$taskDescription task is interrupted")
                }
                try {
                    return block()
                } catch (e: Exception) {

                    if (e is InterruptedException) {
                        throw e
                    }
                    if (!retryConfig.retryableExceptions.contains(e::class.java)) {
                        throw e
                    }

                    val delayPeriod = retryInterval / 2L + random.nextInt((retryInterval / 2L + 1L).toInt()).toLong()
                    logger.getCtxTraceLog(className, "runWithRetry")("Task $taskDescription failed (${e.message}, retry $attempt in $delayPeriod ms")
                    lastException = e
                    delay(delayPeriod)

                    if (retryInterval < retryConfig.maxRetryInterval.inWholeMilliseconds) {
                        retryInterval *= 2L
                    } else {
                        retryInterval = retryConfig.maxRetryInterval.inWholeMilliseconds
                    }
                    attempt += 1
                }
            }
            throw lastException!!
        }
    }
}


