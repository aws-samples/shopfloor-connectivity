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

import com.amazonaws.sfc.MainControllerService.Companion.SFC_CORE
import com.amazonaws.sfc.config.ChannelConfiguration
import com.amazonaws.sfc.config.ChannelConfiguration.Companion.CHANNEL_SEPARATOR
import com.amazonaws.sfc.config.ControllerServiceConfiguration
import com.amazonaws.sfc.config.ScheduleConfiguration
import com.amazonaws.sfc.config.TimestampLevel
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.filters.ChangeFiltersCache
import com.amazonaws.sfc.filters.Filter
import com.amazonaws.sfc.filters.ValueFiltersCache
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricUnits
import com.amazonaws.sfc.metrics.MetricsCollector
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_READS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_READ_ERRORS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_READ_SUCCESS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_VALUES_READ
import com.amazonaws.sfc.metrics.MetricsDataPoint
import com.amazonaws.sfc.metrics.MetricsValue
import com.amazonaws.sfc.transformations.Transformation
import com.amazonaws.sfc.transformations.TransformationException
import com.amazonaws.sfc.transformations.invoke
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import java.io.Closeable
import kotlin.time.Duration


/**
 * Reads input data from protocol sources for a schedule
 * @property schedule Schedule The schedule to read data for
 * @property reader SourceValuesReader The reader for reading the data
 * @property aggregationChannel SendChannel<Mapping<String, SourceReadResult>>? Channel to send read data to aggregation
 * @property readerOutputChannel SendChannel<Mapping<String, Mapping<String, Any?>>>? Channel to send data directly to output if no aggregation is used
 * @property logger Logger Logger for output
 */
class ScheduleReader(
    private val config: ControllerServiceConfiguration,
    private val schedule: ScheduleConfiguration,
    private val readers: Map<String, SourceValuesReader>,
    private val aggregationChannel: SendChannel<Map<String, SourceReadSuccess>>?,
    private val readerOutputChannel: SendChannel<Map<String, SourceOutputData>>,
    private val metricsCollector: MetricsCollector?,
    private val logger: Logger
) : Closeable {

    private val className = this::class.java.simpleName

    private val scope = buildScope("SFC Schedule Reader")

    // data transformations
    private val transformations = config.transformations

    // input data sources
    private val sources = config.sources

    private val changeFilters = ChangeFiltersCache(config)

    private val valueFilters = ValueFiltersCache(config.valueFilters)

    private val blockStoppedChannel = Channel<Any>()

    // true is the schedule has any transformations
    private val scheduleHasTransformations =
        transformations.isNotEmpty() || sources.values.any { s -> s.channels.any { c -> c.value.transformationID != null } }

    // coroutine for reading th data from the inputs
    private val readerWorker: Job = scope.launch("Reader") {
        readSourceValues()
    }

    /**
     * Closes the reader
     */
    override fun close() {
        readerWorker.cancel()
        aggregationChannel?.close()
        readerOutputChannel.close()
        runBlocking {
            blockStoppedChannel.send("stopped")
        }
    }

    suspend fun blockUntilStopped() {
        blockStoppedChannel.receive()
    }

    val isRunning: Boolean
        get() = readerWorker.isActive


    /**
     * Waits until the reader is stopped
     * @param duration Duration Period to wait until reader is stopped
     * @return Boolean True if reader was stopped within timeout period
     */
    suspend fun waitUntilStopped(duration: Duration): Boolean {
        return withTimeoutOrNull(duration) {
            readerWorker.join()
        } != null
    }

    // Reads source values using the reader for the used protocol, loops while worker is active.
    // This function can return false to signal the reader it must stop reading data from the source.
    private suspend fun readSourceValues() = coroutineScope {

        val readResultsChannel = Channel<Pair<String, ReadResult>>(readers.size)

        launch("Combine Results") {
            // Combined source results from all readers
            val combinedReaderResults = mutableMapOf<String, SourceReadResult>()
            val readersWithResults = mutableSetOf<String>()

            suspend fun processReceivedData() {

                if (combinedReaderResults.isNotEmpty()) {
                    processReceivedData(ReadResult(combinedReaderResults))
                    combinedReaderResults.clear()
                    readersWithResults.clear()
                }
            }

            while (isActive) {
                // read result from reader
                val result = readResultsChannel.receive()

                val protocolAdapterID = result.first
                val readerResult = result.second

                // reader already has result, process these first
                if (protocolAdapterID in readersWithResults) {
                    processReceivedData()
                }
                // store results from reader in combined results
                readersWithResults.add(protocolAdapterID)
                readerResult.forEach {
                    combinedReaderResults[it.key] = it.value
                }
                // if data from all readers was received process the data
                if (readersWithResults.size == readers.size) {
                    processReceivedData()
                }
            }
            processReceivedData()
        }

        val j = readers.map { (protocolID, reader) ->
            launch("Send Read Results") {
                reader.read { result ->
                    runBlocking {
                        readResultsChannel.send(protocolID to result)
                    }
                    readerWorker.isActive
                }
            }
        }

        j.forEach { it.join() }

    }


    // Processes the data read from the input reader
    private suspend fun processReceivedData(result: ReadResult) {

        val logError = logger.getCtxErrorLog(className, "processReceivedData")

        val metrics = if (metricsCollector != null) mutableListOf<MetricsDataPoint>() else null

        try {

            metrics?.add(MetricsDataPoint(name = METRICS_READS, units = MetricUnits.COUNT, value = MetricsValue(1)))

            // Only process successful reads, log errors
            result.filter { it.value is SourceReadError }.forEach { source ->
                logError("Schedule \"${schedule.name}\" error reading from source \"${source.key}\", ${source.value}")
                metrics?.add(MetricsDataPoint(name = METRICS_READ_ERRORS, units = MetricUnits.COUNT, value = MetricsValue(1)))
                return
            }

            metrics?.add(MetricsDataPoint(name = METRICS_READ_SUCCESS, units = MetricUnits.COUNT, value = MetricsValue(1)))

            val sourcesWithValues = result.copyValues()
            if (sourcesWithValues.isEmpty()) {
                return
            }

            // apply configured transformations on the input data
            val transformedData = applyTransformation(sourcesWithValues)

            // map values from all sources
            val filteredData = applyFilters(transformedData)

            if (filteredData.isNotEmpty()) {
                runBlocking {
                    // send to aggregator for aggregation
                    if (schedule.isAggregated) {
                        aggregationChannel?.send(filteredData)
                    } else {
                        // no aggregation, combine with timestamps and send to output writer
                        val outputValues = buildOutputValues(schedule, filteredData)
                        try {
                            readerOutputChannel.send(outputValues)
                            val numberOfValues = outputValues.values.fold(0) { acc, v -> acc + v.channels.size }
                            metrics?.add(MetricsDataPoint(name = METRICS_VALUES_READ, units = MetricUnits.COUNT, value = MetricsValue(numberOfValues)))
                        } catch (_: ClosedSendChannelException) {
                        }
                    }
                }
            }
        } finally {
            if (metrics != null) {
                metricsCollector?.put(SFC_CORE, metrics)
            }
        }
    }


    private fun applyFilters(data: Map<String, SourceReadSuccess>): Map<String, SourceReadSuccess> {

        val noChangeFiltersConfigured = config.changeFilters.isEmpty()
        val noValueFiltersConfigured = config.valueFilters.isEmpty()
        val noChangeAndValueFiltersConfigured = noChangeFiltersConfigured && noValueFiltersConfigured

        if (noChangeAndValueFiltersConfigured) return data

        val trace = if (logger.level == LogLevel.TRACE) logger.getCtxTraceLog(className, "applyFilters") else null

        return data.map { (sourceID, sourceValues) ->


            // apply filter on the values of a source, the filter will return the true if it passes the filter, else false
            val filteredSourceValues = sourceValues.values.filter { (channelID, channelReadValue) ->

                val value = channelReadValue.value ?: return@filter false

                // first is applied filter, second is result of filter, passed is true, filtered out is false
                var filterOutput: Pair<Filter?, Boolean>

                // Change filter
                filterOutput = if (noChangeFiltersConfigured) null to true else changeFilters.applyFilter(sourceID, channelID, value, logger)

                // If passed change filter apply value filter
                if (filterOutput.second) {
                    filterOutput = if (noValueFiltersConfigured) null to true else applyValueFilter(sourceID, channelID, value)
                }

                if (trace != null && !filterOutput.second && filterOutput.first != null) {
                    trace("Source \"$sourceID\", Channel \"$channelID\", Value $value (${value::class.java.simpleName}) filtered out by filter ${filterOutput.first}")
                }

                return@filter filterOutput.second
            }

            // new ReadSuccess for the source containing the filtered channel values
            sourceID to SourceReadSuccess(values = filteredSourceValues, timestamp = sourceValues.timestamp)
            // filter out sources that don't have channels with values left after filtering
        }.toMap().filter { it.value.values.isNotEmpty() }
    }


    private fun applyValueFilter(sourceID: String, channelID: String, value: Any): Pair<Filter?, Boolean> {
        val filterName = config.sources[sourceID]?.channels?.get(channelID)?.valueFilterID
        val filter = if (filterName != null) valueFilters[filterName] else null
        val result = filter?.apply(value) ?: true
        return filter to result
    }

    // Applies configured transformation on the input data
    private suspend fun applyTransformation(readResult: Map<String, SourceReadSuccess>): Map<String, SourceReadSuccess> {

        val context = buildScope("ApplyTransformations")


        // No transformations, return input data
        if (!scheduleHasTransformations) {
            return readResult
        }

        // Apply transformations
        return readResult.map { result ->

            val sourceID = result.key
            val channels = sources[sourceID]?.channels ?: emptyMap()

            // test if there are any transformations for this source, if not then return all its values as result of the mapping
            sourceID to if (!sourceHasTransformations(sourceID))
                result.value
            else {
                transformValues(context, sourceID, channels, result)
            }
        }.toMap()
    }

    private suspend fun transformValues(context: CoroutineScope,
                                        sourceID: String,
                                        channels: Map<String, ChannelConfiguration>,
                                        result: Map.Entry<String, SourceReadSuccess>): SourceReadSuccess {

        // map channel values for this source
        val transformedValues = result.value.values.map { (channelID, channelReadValue) ->

            // get first part of channel as additional information might be appended when channel supports wildcards
            val id = channelID.split(CHANNEL_SEPARATOR)[0]

            // get the ID of the transformation for the channel
            val transformationIdForChannel = channels[id]?.transformationID
            val transformationForChannel = if (transformationIdForChannel != null) transformations[transformationIdForChannel] else null

            channelID to if (transformationForChannel == null)
            // this channel does not require transformation, return inout value as result of mapping
                channelReadValue
            else
            // apply the transformation to the value
                try {
                    if (logger.level != LogLevel.TRACE) {
                        // when not in trace mode run transformations async
                        context.async {
                            applyTransformation(channelReadValue, channelID, transformationForChannel)
                        }
                    } else {
                        applyTransformation(channelReadValue, channelID, transformationForChannel)
                    }
                } catch (e: TransformationException) {
                    val errLog = logger.getCtxErrorLog(className, "transformValues")
                    errLog("Error applying transformation \"$transformationIdForChannel\" value $channelReadValue for source \"$sourceID\", channel \"${channelID}\", ${e.message} ${e.operator}")
                    channelReadValue
                }
        }.associate {
            it.first to
                    // result could be a deferred if transformations were run async
                    if (it.second is ChannelReadValue) it.second as ChannelReadValue
                    else (it.second as Deferred<*>).await() as ChannelReadValue
        }
        return SourceReadSuccess(values = transformedValues, timestamp = result.value.timestamp)
    }

    // Applies transformation to a channel value
    private fun applyTransformation(channelValue: ChannelReadValue, valueName: String, transformation: Transformation): ChannelReadValue {
        return if (channelValue.value != null)
            ChannelReadValue(
                transformation.invoke(channelValue.value!!, valueName, throwsException = true, logger = logger),
                channelValue.timestamp
            )
        else
        // There was no transformation needed, just return value
            channelValue

    }

    // Tests if there are any transformations configured for channel values in a source
    private fun sourceHasTransformations(sourceID: String): Boolean {
        val source = sources[sourceID] ?: return false
        return source.channels.any { it.value.transformationID != null }
    }

    // Combines the channel values with timestamps on source/channel level
    private fun buildOutputValues(schedule: ScheduleConfiguration, values: Map<String, SourceReadSuccess>): Map<String, SourceOutputData> {

        return values.map { (sourceID, readValues: SourceReadSuccess) ->

            val needSourceTimestamp = (schedule.timestampLevel == TimestampLevel.BOTH || schedule.timestampLevel == TimestampLevel.SOURCE)
            val sourceTimestamp = if (needSourceTimestamp) readValues.timestamp else null

            val needChannelTimestamp = (schedule.timestampLevel == TimestampLevel.BOTH || schedule.timestampLevel == TimestampLevel.CHANNEL)

            sourceID to SourceOutputData(
                channels = readValues.values.filter { it.value.value != null }.map { (channelID, channelValue) ->
                    val channelTimestamp = if (needChannelTimestamp) channelValue.timestamp ?: readValues.timestamp else null
                    val channelMetadata = config.sources[sourceID]?.channels?.get(channelID)?.metadata
                    channelID to ChannelOutputData(channelValue.value!!, channelTimestamp, channelMetadata)
                }.toMap(),

                timestamp = sourceTimestamp,
                isAggregated = false
            )
        }.toMap().filter { it.value.channels.isNotEmpty() }
    }

}
