/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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