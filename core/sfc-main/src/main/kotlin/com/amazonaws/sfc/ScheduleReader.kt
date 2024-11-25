// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc

import com.amazonaws.sfc.MainControllerService.Companion.SFC_CORE
import com.amazonaws.sfc.channels.channelSubmitEventHandler
import com.amazonaws.sfc.channels.submit
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.ChannelConfiguration.Companion.CHANNEL_SEPARATOR
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.filters.ChangeFiltersCache
import com.amazonaws.sfc.filters.ConditionFiltersCache
import com.amazonaws.sfc.filters.Filter
import com.amazonaws.sfc.filters.ValueFiltersCache
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricUnits
import com.amazonaws.sfc.metrics.MetricsCollector
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_MEMORY
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_READS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_READ_ERRORS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_READ_SUCCESS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_VALUES_READ
import com.amazonaws.sfc.metrics.MetricsDataPoint
import com.amazonaws.sfc.metrics.MetricsValue
import com.amazonaws.sfc.transformations.Transformation
import com.amazonaws.sfc.transformations.TransformationException
import com.amazonaws.sfc.transformations.invoke
import com.amazonaws.sfc.util.MemoryMonitor.Companion.getUsedMemoryMB
import com.amazonaws.sfc.util.WorkerQueue
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.isJobCancellationException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import java.io.Closeable
import java.time.Instant
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
    private val conditionFilters = ConditionFiltersCache(config.conditionFilters, logger)

    private val blockStoppedChannel = Channel<Any>()

    private var closing = false

    // true is the schedule has any transformations
    private val scheduleHasTransformations =
        transformations.isNotEmpty() || sources.values.any { s -> s.channels.any { c -> c.value.transformationID != null } }

    // coroutine for reading th data from the inputs
    private val readerWorker: Job = scope.launch(Dispatchers.Default + CoroutineName("Reader")) {
        val log = logger.getCtxLoggers(className, "readSourceValues")
        try {
            readSourceValues()
        } catch (e: Exception) {
            if (!e.isJobCancellationException)
                log.errorEx("Exception in readerWorker", e)
        }
    }

    /**
     * Closes the reader
     */
    override fun close() {
        closing = true
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
        get() = readerWorker.isActive && !closing


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

        val readResultsChannel =
            Channel<Pair<String, ReadResult?>>(config.tuningConfiguration.scheduleReaderResultsChannelSize, onBufferOverflow = BufferOverflow.SUSPEND)

        val processing = launch(Dispatchers.IO + CoroutineName("processing")) {
            processingTask(this, readResultsChannel)
        }

        val reader = launch(context = Dispatchers.IO) {
            readerTask(readResultsChannel)
        }
        listOf(reader, processing).joinAll()
    }

    private suspend fun CoroutineScope.readerTask(readResultsChannel: Channel<Pair<String, ReadResult?>>) {
        val readerLog = logger.getCtxLoggers(className, "readerTask")

        while (isActive) {
            val readerWorkerQueue =
                buildReaderWorkerQueue(readResultsChannel)

            try {
                readers.forEach { (protocolID, sourceReader) ->
                    readerWorkerQueue.submit(protocolID to sourceReader)
                }
                withTimeout(config.tuningConfiguration.allSourcesReadTimeout) {
                    readerWorkerQueue.await()
                }
            } catch (t: TimeoutCancellationException) {
                readerLog.error("Timeout waiting for source readers to complete")
            } catch (e: Exception) {
                if (!e.isJobCancellationException)
                    readerLog.errorEx("Error reading from reader", e)
                readerWorkerQueue.reset()
            }
        }
    }

    private fun buildReaderWorkerQueue(readResultsChannel: Channel<Pair<String, ReadResult?>>) =
        WorkerQueue<Pair<String, SourceValuesReader>, Unit>(
            workers = config.tuningConfiguration.maxConcurrentSourceReaders,
            capacity = readers.size,
            Dispatchers.IO,
            logger = logger
        ) { (protocolID, reader) ->
            readProtocolTask(protocolID, reader, readResultsChannel) {
                !closing
            }
        }

    private suspend fun processingTask(scope: CoroutineScope, readResultsChannel: Channel<Pair<String, ReadResult?>>) {

        val log = logger.getCtxLoggers(className, "processingTask")
        try {
            // Combined source results from all readers
            val combinedReaderResults = mutableMapOf<String, SourceReadResult>()
            val readersDone = mutableSetOf<String>()

            suspend fun processReceivedData() {

                if (combinedReaderResults.isNotEmpty()) {
                    // slice results so that if channels contain multiple ReadValues there are multiple result sets
                    // where a channel only has a single value
                    var numberOfSlices = 0
                    sliceResults(combinedReaderResults).forEach { slice ->
                        processReceivedData(ReadResult(slice))
                        numberOfSlices += 1
                    }
                    if (numberOfSlices > 1) {
                        log.trace("Result set number of slices $numberOfSlices")
                    }
                    combinedReaderResults.clear()
                    readersDone.clear()
                }
            }

            while (scope.isActive) {
                try {
                    // read result from reader
                    val result = readResultsChannel.receive()

                    val protocolAdapterID = result.first
                    val readerResult: ReadResult? = result.second

                    if (readerResult == null) {
                        log.error("No result from  $protocolAdapterID ")
                        readersDone.add(protocolAdapterID)
                        continue
                    }

                    adjustTimestamps(readerResult)

                    // reader already has result, process these first
                    if (protocolAdapterID in readersDone) {
                        processReceivedData()
                    }
                    // store results from reader in combined results
                    readersDone.add(protocolAdapterID)
                    readerResult.forEach {
                        combinedReaderResults[it.key] = it.value
                    }
                    // if data from all readers was received process the data
                    if (readersDone.size == readers.size) {
                        processReceivedData()
                    }
                } catch (e: Exception) {
                    if (!e.isJobCancellationException) log.errorEx("Error processing data from reader", e)
                }
            }
            processReceivedData()
        } catch (e: Exception) {
            if (!e.isJobCancellationException)
                log.errorEx("Exception in processing", e)
        }

    }

    private fun adjustTimestamps(readerResult: ReadResult) {

        readerResult.forEach { sourceID: String, sourceReadResult: SourceReadResult ->

            if (sourceReadResult is SourceReadSuccess) {

                val sourceTimestampAdjustment = config.sources[sourceID]?.sourceTimestampAdjustment ?: 0L

                if (sourceTimestampAdjustment != 0L) {
                    sourceReadResult.timestamp = sourceReadResult.timestamp.plusMillis(sourceTimestampAdjustment)
                }

                val channelTimestampAdjustment = config.sources[sourceID]?.channelTimestampAdjustment ?: 0L
                if (channelTimestampAdjustment != 0L) {
                    sourceReadResult.values.values.forEach { readValue ->
                        if (readValue.timestamp != null) {
                            readValue.timestamp = readValue.timestamp?.plusMillis(channelTimestampAdjustment)
                        }
                    }
                }
            }
        }
    }

    private fun readProtocolTask(protocolID: String, reader: SourceValuesReader, readResultsChannel: Channel<Pair<String, ReadResult?>>, fnStop: () -> Boolean) {
        val log = logger.getCtxLoggers(className, "readTask")
        runBlocking {
            try {
                log.trace("Worker start reading $protocolID")
                reader.read { result ->
                    log.trace("Finished reading $protocolID")
                    readResultsChannel.submit(
                        protocolID to result,
                        config.tuningConfiguration.scheduleReaderResultsChannelTimeout
                    ) { event ->
                        channelSubmitEventHandler(
                            event = event,
                            channelName = "$className:resultsChannel",
                            tuningChannelSizeName = TuningConfiguration.CONFIG_SCHEDULE_READER_RESULTS_CHANNEL_SIZE,
                            currentChannelSize = config.tuningConfiguration.scheduleReaderResultsChannelSize,
                            tuningChannelTimeoutName = TuningConfiguration.CONFIG_SCHEDULE_READER_RESULTS_CHANNEL_TIMEOUT,
                            log = log
                        )
                    }
                    fnStop()
                }
            } catch (e: Exception) {
                if (!e.isJobCancellationException)
                    log.errorEx("Error reading from $protocolID", e)
            }
        }
    }


    // Processes the data read from the input reader
    private suspend fun processReceivedData(result: ReadResult) {

        val log = logger.getCtxLoggers(className, "processReceivedData")

        val metrics = if (metricsCollector != null) mutableListOf<MetricsDataPoint>() else null

        try {

            metrics?.add(MetricsDataPoint(name = METRICS_READS, units = MetricUnits.COUNT, value = MetricsValue(1)))

            // Only process successful reads, log errors
            result.filter { it.value is SourceReadError }.forEach { source ->
                log.error("Schedule \"${schedule.name}\" error reading from source \"${source.key}\", ${source.value}")
                metrics?.add(MetricsDataPoint(name = METRICS_READ_ERRORS, units = MetricUnits.COUNT, value = MetricsValue(1)))
                return
            }

            metrics?.add(MetricsDataPoint(name = METRICS_READ_SUCCESS, units = MetricUnits.COUNT, value = MetricsValue(1)))

            //  val sourcesWithValues = result.copyValues()
            if (result.isEmpty()) {
                return
            }

            val readValues = selectSuccessfulSourceReadValues(result)

            // apply configured transformations on the input data
            val transformedData: Map<String, SourceReadSuccess> = applyTransformation(readValues)

            // map values from all sources
            val filteredData: Map<String, SourceReadSuccess> = applyFilters(transformedData)

            // compose and decompose channels
            val restructuredData: Map<String, SourceReadSuccess> = restructureChannels(filteredData)

            if (restructuredData.isNotEmpty()) {

                // send to aggregator for aggregation
                if (schedule.isAggregated) {
                    (aggregationChannel as Channel?)?.submit(restructuredData, config.tuningConfiguration.aggregatorChannelTimeout) { event ->
                        channelSubmitEventHandler(
                            event = event,
                            channelName = "$className:aggregationChannel",
                            tuningChannelSizeName = TuningConfiguration.CONFIG_AGGREGATOR_CHANNEL_SIZE,
                            currentChannelSize = config.tuningConfiguration.aggregatorChannelSize,
                            tuningChannelTimeoutName = TuningConfiguration.CONFIG_AGGREGATOR_CHANNEL_TIMEOUT,
                            log = log
                        )
                    }
                } else {
                    // no aggregation, combine with timestamps and send to output writer
                    val outputValues = buildOutputValues(schedule, restructuredData)
                    try {
                        (readerOutputChannel as Channel).submit(outputValues, config.tuningConfiguration.writerInputChannelTimeout) { event ->
                            channelSubmitEventHandler(
                                event,
                                "$className:readerOutputChannel",
                                TuningConfiguration.CONFIG_WRITER_INPUT_CHANNEL_TIMEOUT,
                                config.tuningConfiguration.writerInputChannelSize,
                                TuningConfiguration.CONFIG_WRITER_INPUT_CHANNEL_TIMEOUT,
                                log
                            )
                        }
                        val numberOfValues = outputValues.values.fold(0) { acc, v -> acc + v.channels.size }
                        metrics?.add(
                            MetricsDataPoint(
                                name = METRICS_MEMORY,
                                units = MetricUnits.MEGABYTES,
                                value = MetricsValue(getUsedMemoryMB().toDouble())
                            )
                        )
                        metrics?.add(MetricsDataPoint(name = METRICS_VALUES_READ, units = MetricUnits.COUNT, value = MetricsValue(numberOfValues)))
                    } catch (_: ClosedSendChannelException) {
                    }
                }
            }
        } finally {
            if (metrics != null) {
                metricsCollector?.put(SFC_CORE, metrics)
            }
        }
    }


    private fun restructureChannels(data: Map<String, SourceReadSuccess>): Map<String, SourceReadSuccess> {
        if (data.isEmpty()) return data
        val decomposed = decomposeChannelValues(data)
        val spread = spreadChannelValues(decomposed)
        return composeChannels(spread)
    }

    private fun isDecomposed(sourceConfig: SourceConfiguration, channelConfig: ChannelConfiguration): Boolean {
        return (sourceConfig.decompose == true && channelConfig.decompose != false) ||
                (channelConfig.decompose == true)
    }

    private fun isSpread(sourceConfig: SourceConfiguration, channelConfig: ChannelConfiguration): Boolean {
        return (sourceConfig.spread == true && channelConfig.spread != false) ||
                (channelConfig.spread == true)
    }

    private fun decomposeChannelValues(data: Map<String, SourceReadSuccess>): Map<String, SourceReadSuccess> {

        fun decomposeStructuredValue(name: String,
                                     sourceConfig: SourceConfiguration,
                                     channelConfig: ChannelConfiguration,
                                     value: Any?,
                                     timestamp: Instant?,
                                     decomposed: MutableMap<String, ChannelReadValue>) {
            if (value != null)
                if (value is Map<*, *>) {
                    (value).forEach { (k, v) ->
                        // nested structures
                        if (v is Map<*, *>) decomposeStructuredValue("$name.$k", sourceConfig, channelConfig, v, timestamp, decomposed)
                        else decomposed["$name.${k.toString()}"] = ChannelReadValue(v, timestamp)
                    }
                    decomposed.remove(name)
                } else {
                    if (value is List<*> && isSpread(sourceConfig, channelConfig)) {
                        value.forEachIndexed { i, v ->
                            decomposeStructuredValue("$name.$i", sourceConfig, channelConfig, v, timestamp, decomposed)
                            decomposed.remove(name)
                        }
                    }
                }

        }


        // map indexed by source, entry contains list of channels that need to be decomposed into separate values
        val decomposedSourceChannels: Map<String, Map<String, ChannelConfiguration>> = data.keys.associateWith { source ->
            val sourceConfig: SourceConfiguration? = sources[source]

            val decomposedSourceChannels = sourceConfig?.channels?.filter { isDecomposed(sourceConfig, it.value) } ?: emptyMap()
            decomposedSourceChannels
        }

        // no compositions, just return the data
        if (decomposedSourceChannels.values.map { it.keys }.isEmpty()) return data

        return data.map { (source, sourceData: SourceReadSuccess) ->

            val sourceDecomposedChannels: Map<String, ChannelConfiguration>? = decomposedSourceChannels[source]

            source to if (!sourceDecomposedChannels.isNullOrEmpty()) {

                val channelValues: MutableMap<String, ChannelReadValue> = sourceData.values.toMutableMap()
                sourceDecomposedChannels.forEach { ch ->
                    val value = channelValues[ch.key]
                    if (value != null) {
                        config.sources[source]?.let { sourceConfig -> decomposeStructuredValue(ch.key, sourceConfig, ch.value, value.value, value.timestamp, channelValues) }
                    }
                }

                // remove decomposed channels
                sourceDecomposedChannels.forEach {
                    if (channelValues[it.key]?.value is Map<*, *>) channelValues.remove(it.key)
                }
                SourceReadSuccess(channelValues.toMap(), sourceData.timestamp)

            } else {
                // no decompositions fot this source
                sourceData
            }
        }.toMap()
    }

    private fun spreadChannelValues(data: Map<String, SourceReadSuccess>): Map<String, SourceReadSuccess> {

        // map indexed by source, entry contains list of channels that need to be decomposed into separate values
        val spreadSourceChannels: Map<String, Map<String, ChannelConfiguration>> = data.keys.associateWith { source ->
            val sourceConfig: SourceConfiguration? = sources[source]
            val spreadSourceChannels: Map<String, ChannelConfiguration> = sourceConfig?.channels?.filter { isSpread(sourceConfig, it.value) } ?: emptyMap()
            spreadSourceChannels
        }

        // no spreads, just return the data
        if (spreadSourceChannels.values.map { it.keys }.flatten().isEmpty()) return data

        return data.map { (source, sourceData: SourceReadSuccess) ->

            val sourceSpreadChannels = spreadSourceChannels[source]

            source to if (!sourceSpreadChannels.isNullOrEmpty()) {

                val channelValues: MutableMap<String, ChannelReadValue> = sourceData.values.toMutableMap()
                sourceSpreadChannels.forEach { ch ->
                    val value = channelValues[ch.key]
                    if (value != null) {
                        val v = value.value
                        if ((v != null) && (v is List<*>)) {
                            v.forEachIndexed { i, sv ->
                                if (sv is Map<*, *>)
                                    channelValues["${ch.key}.$i"] = ChannelReadValue(sv, value.timestamp)
                            }
                        }
                    }
                }

                // remove decomposed channels
                sourceSpreadChannels.forEach {
                    if (channelValues[it.key]?.value is List<*>) channelValues.remove(it.key)
                }
                SourceReadSuccess(channelValues.toMap(), sourceData.timestamp)

            } else {
                // no decompositions fot this source
                sourceData
            }
        }.toMap()
    }


    private fun composeChannels(dec: Map<String, SourceReadSuccess>): Map<String, SourceReadSuccess> {
        val compositionChannels: List<String> = dec.keys.flatMap { source -> config.sources[source]?.compose?.values ?: emptyList() }.flatten()

        if (compositionChannels.isEmpty()) return dec


        return dec.map { (source: String, sourceData: SourceReadSuccess) ->

            val sourceCompositions = config.sources[source]?.compose ?: emptyMap()

            if (sourceCompositions.isNotEmpty()) {

                val sourceValues = sourceData.values.toMutableMap()

                sourceCompositions.forEach { (structureName, channelIDs) ->
                    val struct = buildStructure(channelIDs, sourceValues, source)
                    if (struct.isNotEmpty()) {
                        sourceValues[structureName] = ChannelReadValue(struct, sourceData.timestamp)
                    }
                }

                // remove channels used by composition
                compositionChannels.forEach {
                    sourceValues.remove(it)
                }

                source to SourceReadSuccess(sourceValues, sourceData.timestamp)

            } else {
                source to dec[source]!!
            }

        }.toMap()
    }


    private fun buildStructure(channelIDs: List<String>,
                               sourceValues: MutableMap<String, ChannelReadValue>,
                               source: String) = sequence {
        channelIDs.forEach { channelID ->
            val channelValue = sourceValues[channelID]
            if (channelValue != null) {
                val channelConfiguration = sources[source]?.channels?.get(channelID)
                val name = channelConfiguration?.name ?: channelID
                yield(name to channelValue.value)
            }
        }
    }.toMap()

    private fun selectSuccessfulSourceReadValues(readResult: ReadResult) =
        readResult.filter { it.value is SourceReadSuccess && (it.value as SourceReadSuccess).values.isNotEmpty() }.map {
            it.key to it.value as SourceReadSuccess
        }.toMap()


    private fun applyFilters(data: Map<String, SourceReadSuccess>): Map<String, SourceReadSuccess> {

        if (data.isEmpty()) return data

        val noChangeFiltersConfigured = config.changeFilters.isEmpty()
        val noValueFiltersConfigured = config.valueFilters.isEmpty()
        val noConditionFiltersConfigured = config.conditionFilters.isEmpty()
        val noFiltersConfigured = noChangeFiltersConfigured && noValueFiltersConfigured && noConditionFiltersConfigured


        if (noFiltersConfigured) return data

        val trace = if (logger.level == LogLevel.TRACE) logger.getCtxTraceLog(className, "applyFilters") else null

        val sourceOutputData: Map<String, SourceReadSuccess> = data.map { (sourceID, sourceValues) ->

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
        }.toMap().filter { it.value.values.isNotEmpty() }

        return if (noConditionFiltersConfigured)
            sourceOutputData
        else
            applyConditionFilters(sourceOutputData)
    }

    private fun applyConditionFilters(sourceOutputData: Map<String, SourceReadSuccess>) =
        sourceOutputData.map { (sourceID: String, sourceData: SourceReadSuccess) ->
            val trace = logger.getCtxTraceLog(className, "applyConditionFilters")
            val filtered = sourceData.values.filter { (channelID: String, _: ChannelReadValue) ->

                val conditionFilterID = config.sources[sourceID]?.channels?.get(channelID)?.conditionFilterID
                var filter: Filter? = null

                val conditionFilterResult = if (conditionFilterID == null) true else {
                    filter = conditionFilters[conditionFilterID]
                    filter == null || filter.apply(sourceData.valuesMap)
                }

                if (!conditionFilterResult) {
                    val value = sourceData.values[channelID]?.value
                    trace("Source \"$sourceID\", Channel \"$channelID\", Value $value (${value!!::class.java.simpleName}) filtered out by condition filter \"$conditionFilterID\" ($filter)")
                }

                conditionFilterResult
            }
            sourceID to SourceReadSuccess(timestamp = sourceData.timestamp, values = filtered)
        }.toMap().filter { it.value.values.isNotEmpty() }


    private fun applyValueFilter(sourceID: String, channelID: String, value: Any): Pair<Filter?, Boolean> {
        val filterName = config.sources[sourceID]?.channels?.get(channelID)?.valueFilterID
        val filter = if (filterName != null) valueFilters[filterName] else null
        val result = filter?.apply(value) ?: true
        return filter to result
    }

    // Applies configured transformation on the input data
    private suspend fun applyTransformation(data: Map<String, SourceReadSuccess>): Map<String, SourceReadSuccess> {

        if (data.isEmpty()) return data

        val context = buildScope("ApplyTransformations")

        // No transformations, return input data
        if (!scheduleHasTransformations) {
            return data
        }

        // Apply transformations
        return data.map { result ->

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


    private suspend fun transformValues(
        context: CoroutineScope,
        sourceID: String,
        channels: Map<String, ChannelConfiguration>,
        result: Map.Entry<String, SourceReadSuccess>
    ): SourceReadSuccess {

        // map channel values for this source
        val transformedValues = result.value.values.map { (channelID, channelReadValue) ->


            val ids = mutableListOf(channelID)
            if (channelID.contains(CHANNEL_SEPARATOR)) {
                ids.add(channelID.split(CHANNEL_SEPARATOR)[0])
            }

            // get the ID of the transformation for the channel
            val channelIDForTransformation: String? = ids.find {
                val channelTransformation = channels[it]?.transformationID
                transformations.containsKey(channelTransformation)
            }

            val (transformationID, transformationForChannel) = if (channelIDForTransformation != null) {
                val id = channels[channelIDForTransformation]?.transformationID
                id to transformations[id]
            } else null to null


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
                    errLog("Error applying transformation \"$transformationID\" value $channelReadValue for source \"$sourceID\", channel \"${channelID}\", ${e.message} ${e.operator}")
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
                    val id = channelID.split(CHANNEL_SEPARATOR)[0]
                    val channelMetadata = config.sources[sourceID]?.channels?.get(id)?.metadata
                    channelID to ChannelOutputData(channelValue.value!!, channelTimestamp, channelMetadata)
                }.toMap(),

                timestamp = sourceTimestamp,
                isAggregated = false
            )
        }.toMap().filter { it.value.channels.isNotEmpty() }
    }

    companion object {

        fun sliceResults(data: MutableMap<String, SourceReadResult>): Sequence<Map<String, SourceReadSuccess>> {

            var completed = false
            var i = 0

            return sequence {
                while (!completed) {
                    val slice = sequence {
                        data.filter { it.value is SourceReadSuccess }.forEach { (source, sd) ->
                            val s = channelsForSource((sd as SourceReadSuccess).values, i)
                            if (s.isNotEmpty()) {
                                yield(source to SourceReadSuccess(s, sd.timestamp))
                            }
                        }
                    }.toMap()

                    if (slice.isNotEmpty()) {
                        yield(slice)
                        i += 1
                    } else {
                        completed = true
                    }
                }
            }
        }

        private fun channelsForSource(sourceData: Map<String, ChannelReadValue>, i: Int): Map<String, ChannelReadValue> =

            sequence {
                sourceData.forEach { (channel, channelValue) ->

                    if (i == 0) {
                        if (channelValue.value is List<*>) {
                            val valueList = (channelValue.value as List<*>)
                            if (valueList.first() is ChannelReadValue) {
                                yield(channel to valueList.first() as ChannelReadValue)
                            } else {
                                yield(channel to channelValue)
                            }
                        } else {
                            yield(channel to channelValue)
                        }
                    } else {
                        if (channelValue.value is List<*> && (channelValue.value as List<*>).first() is ChannelReadValue && (i < (channelValue.value as List<*>).size)) {
                            yield(channel to ((channelValue.value as List<*>)[i]) as ChannelReadValue)
                        }
                    }
                }

            }.toMap()
    }

}
