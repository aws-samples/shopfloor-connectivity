
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.metrics

import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.cancellable
import kotlin.time.Duration

class InProcessMetricsProvider(private val metricsReader: MetricsCollectorReader,
                               private val logger: Logger) : MetricsProvider {

    private val className = this::class.java.simpleName

    var reader: Job? = null

    override suspend fun read(interval: Duration, consumer: MetricsConsumer): Unit = coroutineScope {

        val metricsProvider = MetricsAsFlow(metricsReader, interval, logger)

        reader = launch(context = Dispatchers.Default, name = "Collect Read Results") {
            metricsReaderTask(metricsReader, interval, consumer)
        }
    }

    private suspend fun metricsReaderTask(metricsReader:MetricsCollectorReader, interval: Duration, consumer: MetricsConsumer ) {
        try {
            val metricsProvider = MetricsAsFlow(metricsReader, interval, logger)
            metricsProvider.metricsFlow.buffer(100).cancellable().collect {
                try {
                    if (!consumer(it)) {
                        return@collect
                    }
                } catch (e: Exception) {
                    logger.getCtxErrorLog(className, "read")
                }
            }
        }catch (e : Exception) {
            logger.getCtxErrorLog(className, "reader")
        }
    }

    override suspend fun close() {
        withTimeoutOrNull(1000L) {
            reader?.cancel()
            reader?.join()
        }
    }

}

