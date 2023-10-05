/*
 * Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * Licensed under the Amazon Software License (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is located at :
 *
 *     http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 *
 */

package com.amazonaws.sfc.metrics

import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

class InProcessMetricsProvider(private val metricsReader: MetricsCollectorReader,
                               private val logger: Logger) : MetricsProvider {

    private val className = this::class.java.simpleName

    var reader: Job? = null

    override suspend fun read(interval: Duration, consumer: MetricsConsumer): Unit = coroutineScope {

        val metricsProvider = MetricsAsFlow(metricsReader, interval, logger)

        reader = launch("Collect Read Results") {
            metricsProvider.metricsFlow.buffer(100).cancellable().collect {
                try {
                    if (!consumer(it)) {
                        cancel()
                    }
                } catch (e: Exception) {
                    logger.getCtxErrorLog(className, "read")
                }
            }
        }
    }

    override suspend fun close() {
        withTimeoutOrNull(1000L) {
            reader?.cancel()
            reader?.join()
        }
    }

}

