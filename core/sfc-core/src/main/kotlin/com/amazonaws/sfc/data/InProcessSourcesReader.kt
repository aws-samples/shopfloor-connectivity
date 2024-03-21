// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.data


import com.amazonaws.sfc.config.ScheduleConfiguration
import com.amazonaws.sfc.config.TuningConfiguration
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.InProcessMetricsProvider
import com.amazonaws.sfc.metrics.MetricsConfiguration
import com.amazonaws.sfc.metrics.MetricsProvider
import com.amazonaws.sfc.util.isJobCancellationException
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.flowOn


/**
 * In process protocol data reader
 */
class InProcessSourcesReader(
    private val protocolAdapter: ProtocolAdapter,
    private val schedule: ScheduleConfiguration,
    private val sources: Map<String, ArrayList<String>>,
    private val tuningConfiguration: TuningConfiguration,
    private val metricsConfig: MetricsConfiguration?,
    private val logger: Logger
) : SourceValuesReader {

    /**
     * Reads data from a reader until the readResultConsumer that is called for every received read result return false
     * @param consumer readResultConsumer Consumer of read data, return true to continue reading, false to stop
     * @see ReadResultConsumer
     * @return Unit
     */
    override suspend fun read(consumer: ReadResultConsumer): Unit = coroutineScope {

        val sourcesReader = SourcesValuesAsFlow(protocolAdapter, sources, schedule.interval, logger)

        launch(context = Dispatchers.IO, name = "Collect Read Results") {
            try {
                sourcesReader.use {
                    sourcesReader.sourceReadResults(
                        currentCoroutineContext(),
                        tuningConfiguration.maxConcurrentSourceReaders,
                        tuningConfiguration.allSourcesReadTimeout
                    )
                        .buffer(1000)
                        .cancellable()
                        .collect {
                            try {
                                if (!consumer(it)) {
                                    cancel()
                                }
                            } catch (e: Exception) {
                                logger.getCtxLoggers(this::class.java.name, "read").errorEx("Exception happened in consumer", e)
                            }
                        }
                }
            } catch (e: Exception) {
                if (!e.isJobCancellationException)
                    logger.getCtxErrorLogEx(this::class.java.name, "collectReadResults")("Error reading data from sources", e)
            }
        }
    }

    override val isInitialized = true

    override val metricsProvider: MetricsProvider? by lazy {
        if (metricsConfig != null && protocolAdapter.metricsCollector != null) {
            InProcessMetricsProvider(protocolAdapter.metricsCollector!!, logger)
        } else {
            null
        }
    }


    /**
     * Closes the reader
     */
    override suspend fun close() {
    }

    companion object {
        /**A
         * Creates a in process reader from configuration data
         * @param schedule ScheduleConfiguration schedule
         * @param adapter ProtocolAdapter
         * @param sources Mapping<String, ArrayList<String>> sources with channels to read
         * @return SourceValuesReader?
         */

        fun createInProcessSourcesReader(
            schedule: ScheduleConfiguration,
            adapter: ProtocolAdapter,
            sources: Map<String, ArrayList<String>>,
            tuningConfiguration: TuningConfiguration,
            metricsConfig: MetricsConfiguration?,
            logger: Logger
        ): SourceValuesReader {
            return InProcessSourcesReader(adapter, schedule, sources, tuningConfiguration, metricsConfig, logger)
        }
    }

}
