/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc

import com.amazonaws.sfc.aggregations.AggregationConfiguration
import com.amazonaws.sfc.aggregations.Aggregator
import com.amazonaws.sfc.config.ControllerServiceConfiguration
import com.amazonaws.sfc.config.ScheduleConfiguration
import com.amazonaws.sfc.data.SourceOutputData
import com.amazonaws.sfc.data.SourceReadSuccess
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/**
 * Handles aggregation of data for an SFC schedule
 * @property schedule Schedule The Schedule te aggregator is processing
 * @property aggregationOutputChannel SendChannel<Mapping<String, Mapping<String, Any?>>> Channel to output aggregated data
 * @property aggregationInputChannel ReceiveChannel<Mapping<String, SourceReadResult>> Channel to input data to aggregate
 * @property logger Logger Logger for output
 */
class ScheduleAggregator(
    config: ControllerServiceConfiguration,
    private val schedule: ScheduleConfiguration,
    private val aggregationOutputChannel: SendChannel<Map<String, SourceOutputData>>,
    private val aggregationInputChannel: ReceiveChannel<Map<String, SourceReadSuccess>>,
    private val logger: Logger
) {

    private val scope = buildScope("SFC Schedule Aggregator")

    // transformations for aggregated data
    private val transformations = config.transformations

    // aggregations for schedule
    private val aggregation = schedule.aggregation

    // coroutine handling the aggregation of data
    private val aggregationWorker = scope.launch("Aggregator") {
        if (aggregation != null) {
            aggregateSourceValues(aggregation)
        }
    }

    val isRunning: Boolean
        get() = aggregationWorker.isActive

    // Aggregates data
    private suspend fun aggregateSourceValues(aggregation: AggregationConfiguration) {

        val aggregator = Aggregator(aggregation, transformations, logger)

        // read received data from channel
        for (data: Map<String, SourceReadSuccess> in aggregationInputChannel) {
            // if aggregation size is reached apply aggregations
            if (aggregator.add(data) >= schedule.aggregationSize) {
                val aggregatedData: Map<String, SourceOutputData> = aggregator.aggregate().map { (sourceID, sourceValues) ->
                    sourceID to
                            SourceOutputData(
                                channels = sourceValues,
                                isAggregated = true
                            )
                }.toMap()
                // send to schedule writer
                aggregationOutputChannel.send(aggregatedData)
            }
        }

    }

    /**
     * Waits until the aggregator is stopped
     * @param duration Duration Period to wait until aggregator is stopped
     * @return Boolean True if stopped within timeout
     */
    suspend fun waitUntilStopped(duration: Duration): Boolean {
        return withTimeoutOrNull(duration) {
            aggregationWorker.join()
        } != null
    }


}