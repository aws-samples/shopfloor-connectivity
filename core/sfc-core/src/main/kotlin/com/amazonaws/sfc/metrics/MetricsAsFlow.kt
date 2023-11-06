
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.metrics


import com.amazonaws.sfc.log.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.measureTime

class MetricsAsFlow(private var metricsReader: MetricsCollectorReader?,
                    private val interval: Duration,
                    private val logger: Logger) {

    private val className = this::class.java.simpleName

    val metricsFlow: Flow<MetricsData> by lazy {

        val log = logger.getCtxLoggers(className, "metricsFlow")

        flow {

            var cancelled = false

            while (!cancelled) {

                // measure time it takes to handle a read cycle
                val duration = measureTime {
                    if (metricsReader != null) {
                        val availableMetrics = metricsReader?.read()

                        if (!availableMetrics.isNullOrEmpty()) {
                            // emit result in flow
                            try {
                                log.trace("Emitting ${availableMetrics.flatMap { it.dataPoints }.size} metrics data points")
                                emitAll(availableMetrics.asFlow())
                            } catch (e: Throwable) {
                                log.error("Error emitting metrics, $e")
                                cancelled = true
                            }
                        }
                    }
                }
                // wait for next iteration
                if (duration <= interval) {
                    delay(interval - duration)
                }
            }
        }
    }
}

