
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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
import kotlinx.coroutines.Dispatchers
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
    private val aggregationWorker = scope.launch(context = Dispatchers.IO, name = "Aggregator") {
        try {
            if (aggregation != null) {
                aggregateSourceValues(aggregation)
            }
        } catch ( e: Exception) {
            logger.getCtxErrorLog(this::class.simpleName.toString(), "aggregationWorker")("Error aggregating data, $e")
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