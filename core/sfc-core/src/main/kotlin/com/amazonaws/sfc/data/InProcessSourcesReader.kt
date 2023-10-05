/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.data


import com.amazonaws.sfc.config.ScheduleConfiguration
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.InProcessMetricsProvider
import com.amazonaws.sfc.metrics.MetricsConfiguration
import com.amazonaws.sfc.metrics.MetricsProvider
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.cancellable


/**
 * In process protocol data reader
 */
class InProcessSourcesReader(
    private val protocolAdapter: ProtocolAdapter,
    private val schedule: ScheduleConfiguration,
    private val sources: Map<String, ArrayList<String>>,
    private val metricsConfig: MetricsConfiguration?,
    private val logger: Logger) : SourceValuesReader {

    /**
     * Reads data from a reader until the readResultConsumer that is called for every received read result return false
     * @param consumer readResultConsumer Consumer of read data, return true to continue reading, false to stop
     * @see ReadResultConsumer
     * @return Unit
     */
    override suspend fun read(consumer: ReadResultConsumer): Unit = coroutineScope {

        val sourcesReader = SourcesValuesAsFlow(protocolAdapter, sources, schedule.interval)

        launch("Collect Read Results") {
            sourcesReader.use {
                sourcesReader.sourceReadResults.buffer(100).cancellable().collect {
                    if (!consumer(it)) {
                        cancel()
                    }
                }
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
            metricsConfig: MetricsConfiguration?,
            logger: Logger): SourceValuesReader {
            return InProcessSourcesReader(adapter, schedule, sources, metricsConfig, logger)
        }
    }

}
