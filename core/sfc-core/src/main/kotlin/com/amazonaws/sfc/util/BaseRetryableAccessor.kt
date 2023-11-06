
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.util

class BaseRetryableAccessor {
    /**
     * Execute with retries.
     *
     * @param tries                no of retries
     * @param initialBackoffMillis backoff in milliseconds
     * @param func                 executable action
     * @param retryableExceptions  exceptions to retry on
     * @param <T>                  response
     * @param <E>                  exception
     * @return response/exception
     * @throws E exception while talking via AWS SDK
    </E></T> */

    @Suppress("UNCHECKED_CAST")
    fun <T, E : Throwable> retry(
        tries: Int, initialBackoffMillis: Int, func: CrashableSupplier<T, E>,
        retryableExceptions: Iterable<Class<out Throwable?>>
    ): T {
        var lastException: E? = null
        var tryCount = 0
        while (tryCount++ < tries) {
            try {
                return func.apply()
            } catch (e: Throwable) {
                var retryable = false
                lastException = e as E
                for (t in retryableExceptions) {
                    if (t.isAssignableFrom(e.javaClass)) {
                        retryable = true
                        break
                    }
                }

                // If not retryable, immediately throw it
                if (!retryable) {
                    throw lastException
                }

                // Sleep with backoff
                try {
                    Thread.sleep(initialBackoffMillis.toLong() * tryCount)
                } catch (ex: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }
        throw lastException!!
    }
}