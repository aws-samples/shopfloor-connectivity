/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc

import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.ControllerServiceConfiguration
import com.amazonaws.sfc.config.ScheduleConfiguration
import com.amazonaws.sfc.data.SourceOutputData
import com.amazonaws.sfc.data.SourceReadSuccess
import com.amazonaws.sfc.data.SourceValuesReader
import com.amazonaws.sfc.data.TargetWriter
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricsCollector
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.Closeable
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Class to handle reading from source protocol, aggregation and transformation and writing to the targets for a single SFC schedule
 * @property reader ScheduleReader Reader for configuration
 * @property aggregator ScheduleAggregator? Aggregator for input data
 * @property writer ScheduleWriter Writer to write data to targets
 */
class ScheduleController(private val reader: ScheduleReader, private val aggregator: ScheduleAggregator?, private val writer: ScheduleWriter) : Closeable {

    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Closes the controller
     */
    override fun close() {
        reader.close()
        runBlocking {
            stopped.join()
        }
    }

    /**
     * Waits until all workers managed by the controller are stopped.
     */
    private val stopped
        get() = scope.async {
            withTimeoutOrNull(WAIT_FOR_STOP_DURATION_TOTAL) {
                reader.waitUntilStopped(WAIT_FOR_STOP_DURATION)
                aggregator?.waitUntilStopped(WAIT_FOR_STOP_DURATION)
                writer.waitUntilStopped(WAIT_FOR_STOP_DURATION)
            }
        }

    val isRunning: Boolean
        get() = reader.isRunning && aggregator?.isRunning ?: true && writer.isRunning


    /**
     * Blocks until the scheduler controller is stopped
     */
    suspend fun blockUntilStopped() {
        reader.blockUntilStopped()
        writer.blockUntilStopped()
    }


    companion object {
        fun createScheduleController(
            configReader: ConfigReader,
            config: ControllerServiceConfiguration,
            scheduleName: String,
            sourceReaders: Map<String, SourceValuesReader>,
            logger: Logger,
            metricsCollector: MetricsCollector?,
            targetWriters: Map<String, TargetWriter>
        ): ScheduleController {


            // Schedule to control
            val schedule: ScheduleConfiguration = config.schedules.first { it.name == scheduleName }

            // Channel for sending data to targets
            val writerInputChannel = Channel<Map<String, SourceOutputData>>(READER_BUFFER_SIZE)

            // Channel for sending data to aggregator
            val aggregatorInputChannel = if (schedule.isAggregated) Channel<Map<String, SourceReadSuccess>>(schedule.aggregationSize) else null

            return ScheduleController(

                // Worker for reading data from the source protocol
                reader = ScheduleReader(
                    config = config,
                    schedule = schedule,
                    readers = sourceReaders,
                    aggregationChannel = aggregatorInputChannel,
                    readerOutputChannel = writerInputChannel,
                    metricsCollector = metricsCollector,
                    logger = logger
                ),

                // Worker for optionally aggregating the data
                aggregator = if (aggregatorInputChannel != null) ScheduleAggregator(
                    config = config,
                    schedule = schedule,
                    aggregationOutputChannel = writerInputChannel,
                    aggregationInputChannel = aggregatorInputChannel,
                    logger = logger
                ) else null,

                // Worker for writing data to targets
                writer = ScheduleWriter(
                    configReader = configReader,
                    config = config,
                    schedule = schedule,
                    targets = targetWriters,
                    writerInputChannel = writerInputChannel,
                    metricsCollector = metricsCollector,
                    logger = logger
                ),

                )
        }


        val WAIT_FOR_STOP_DURATION = 10.toDuration(DurationUnit.SECONDS)
        val WAIT_FOR_STOP_DURATION_TOTAL = WAIT_FOR_STOP_DURATION * 3
        private const val READER_BUFFER_SIZE = 100
    }

}
