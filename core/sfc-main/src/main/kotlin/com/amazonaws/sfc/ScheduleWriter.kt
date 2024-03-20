// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc


import com.amazonaws.sfc.MainControllerService.Companion.SFC_CORE
import com.amazonaws.sfc.config.ChannelConfiguration.Companion.CHANNEL_SEPARATOR
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.ControllerServiceConfiguration
import com.amazonaws.sfc.config.ScheduleConfiguration
import com.amazonaws.sfc.config.SourceConfiguration
import com.amazonaws.sfc.data.ChannelOutputData
import com.amazonaws.sfc.data.SourceOutputData
import com.amazonaws.sfc.data.TargetData
import com.amazonaws.sfc.data.TargetWriter
import com.amazonaws.sfc.data.TargetWriter.Companion.TIMOUT_TARGET_WRITE
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricUnits
import com.amazonaws.sfc.metrics.MetricsCollector
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_MEMORY
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_MESSAGES
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITES
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_ERRORS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_SUCCESS
import com.amazonaws.sfc.metrics.MetricsDataPoint
import com.amazonaws.sfc.metrics.MetricsValue
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.util.MemoryMonitor.Companion.getUsedMemoryMB
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.isJobCancellationException
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import java.util.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Handles writing of output data to targets of a schedule
 * @property configReader ConfigReader Reader for configuration
 * @property config ControllerServiceConfig Controller configuration
 * @property schedule Schedule The Schedule for which writer is processing
 * @property writerInputChannel ReceiveChannel<Mapping<String, Mapping<String, Any?>>> Input data from reader or aggregator worker to write to targets
 * @property logger Logger Logger for output
 */
class ScheduleWriter(
    private val configReader: ConfigReader,
    private val config: ControllerServiceConfiguration,
    private val schedule: ScheduleConfiguration,
    private val targets: Map<String, TargetWriter>,
    private val writerInputChannel: ReceiveChannel<Map<String, SourceOutputData>>,
    private val metricsCollector: MetricsCollector?,
    private val logger: Logger
) {

    private val className = this::class.java.simpleName

    private val scope = buildScope("SFC Schedule Writer")

    // Sources
    private val sources: Map<String, SourceConfiguration> = config.sources

    private val blockStoppedChannel = Channel<Any>()

    // Metadata that will be added to output data
    private val sourceMetadata: Map<String, Map<String, String>> by lazy {
        config.sources.filter {
            it.value.metadata.isNotEmpty()
        }.map {
            it.key to it.value.metadata
        }.toMap()
    }

    private val channelMetadata: Map<String, Map<String, Map<String, String>>> by lazy {
        config.sources.map {
            it.key to it.value.channels.map { ch -> ch.key to ch.value.metadata }.toMap()
        }.toMap()
    }


    // Coroutine that writes data to all targets for a schedule
    private val writerWorker = scope.launch("Writer") {
        try {
            writeTargetValues()
        } catch (e: Exception) {
            logger.getCtxErrorLogEx(className, "writerWorker")("Error writing targets", e)
        }
    }

    val isRunning: Boolean
        get() = writerWorker.isActive

    /**
     * Blocks until the writer stopped
     */
    suspend fun blockUntilStopped() {
        blockStoppedChannel.receive()
    }

    /**
     * Blocking wait until writer has been stopped
     * @param duration Duration Period to wait until writer stops
     * @return Boolean True if writer was stopped within timeout period
     */
    suspend fun waitUntilStopped(duration: Duration): Boolean {
        return withTimeoutOrNull(duration) {
            writerWorker.join()
        } != null
    }


    // writes data targets
    private suspend fun writeTargetValues() {

        val log = logger.getCtxLoggers(className, "writeTargetValues")

        if (targets.isEmpty()) {
            log.error("No active targets for schedule \"${schedule.name}\"")
            return
        }

        // loop reading data from input channel for this writer
        for (data in writerInputChannel) {
            // write data to all targets
            writeDataToTargets(data, targets)
        }

        // if the input channel is closed then stop all targets
        (withContext(Dispatchers.IO) {
            targets.map { t ->
                launch("Close target ${t.key}") {
                    t.value.close()
                }
            }.joinAll()
        })

        blockStoppedChannel.send("writer stopped")
    }

    // writes data to all configured targets
    private suspend fun writeDataToTargets(data: Map<String, SourceOutputData>, targets: Map<String, TargetWriter>) = coroutineScope {

        val mappedData: Map<String, SourceOutputData> = mapToNamedData(data)
        val metadata = config.metadata + schedule.metadata

        val serial = UUID.randomUUID().toString()
        val writes = targets.map { target ->
            async {
                writeDataToTarget(target.key, target.value, TargetData(schedule.name, mappedData, metadata, serial, noBuffering = false, timestamp = DateTime.systemDateTimeUTC()))
            }
        }
        val metrics = if (metricsCollector != null)
            mutableListOf(
                MetricsDataPoint(name = METRICS_MESSAGES, units = MetricUnits.COUNT, value = MetricsValue(1)),
                MetricsDataPoint(name = METRICS_WRITES, units = MetricUnits.COUNT, value = MetricsValue(writes.size)))
        else null

        try {
            withTimeout(TIMOUT_TARGET_WRITE.toDuration(DurationUnit.MILLISECONDS)) {
                writes.joinAll()
            }
        } catch (_: Exception) {
            val err = logger.getCtxErrorLog(className, "writeDataToTargets")
            err("Timeout writing to targets")
        } finally {

            if (metricsCollector != null) {
                val writesSuccessfulCount = writes.fold(0) { acc, w -> acc + if (w.isCompleted && w.await()) 1 else 0 }
                if (writesSuccessfulCount != 0) {
                    metrics?.add(MetricsDataPoint(name = METRICS_WRITE_SUCCESS, units = MetricUnits.COUNT, value = MetricsValue(writesSuccessfulCount)))
                }
                if (writesSuccessfulCount != writes.size) {
                    metrics?.add(
                        MetricsDataPoint(
                            name = METRICS_WRITE_ERRORS,
                            units = MetricUnits.COUNT,
                            value = MetricsValue(writes.size - writesSuccessfulCount)
                        )
                    )
                    metrics?.add(
                        MetricsDataPoint(
                            name = METRICS_MEMORY,
                            units = MetricUnits.MEGABYTES,
                            value = MetricsValue(getUsedMemoryMB().toDouble())
                        )
                    )
                }
                metrics?.let { metricsCollector.put(SFC_CORE, it) }
            }
        }

    }

    // maps the source and channel IDs of the output data to the names of the source or channel if these exist in the configuration
    private fun mapToNamedData(data: Map<String, SourceOutputData>): Map<String, SourceOutputData> =

        data.map { (sourceID, sourceChannelData: SourceOutputData) ->

            // get source and its name
            val source: SourceConfiguration? = sources[sourceID]
            val sourceName = source?.name ?: sourceID

            val namedValues = sourceChannelData.channels.filter { it.value.value != null }.map { (channelID, channelData: ChannelOutputData) ->

                // het value and its name
                val channel = source?.channels?.get(channelID)
                val channelName = channel?.name ?: channelID

                // mapped channel data
                channelName to ChannelOutputData(
                    value = channelData.value,
                    timestamp = channelData.timestamp,
                    metadata = channelMetadata[sourceID]?.get(channelID.split(CHANNEL_SEPARATOR)[0])
                )
            }.toMap()

            // mapped source data
            sourceName to SourceOutputData(
                channels = namedValues,
                timestamp = sourceChannelData.timestamp,
                metadata = sourceMetadata[sourceID],
                isAggregated = sourceChannelData.isAggregated
            )

        }.toMap()


    // Writes data to a single target
    private suspend fun writeDataToTarget(targetID: String, target: TargetWriter, targetData: TargetData): Boolean = coroutineScope {

        val log = logger.getCtxLoggers(className, "writeDataToTarget")

        log.trace("Writing to target \"$targetID\"")
        try {
            withTimeout(TIMOUT_TARGET_WRITE) {
                if (target.isInitialized) {
                    target.writeTargetData(targetData)
                    true
                } else {
                    log.trace("Can not write to target $targetID as it has not been initialized yet")
                    false
                }
            }

        } catch (t: TimeoutCancellationException) {
            log.error("Timeout writing to target \"${targetID}\"")
            false
        } catch (e: Exception) {
            if (!e.isJobCancellationException)
                log.errorEx("Error writing to target \"${targetID}\"", e)
            false
        }
    }


}