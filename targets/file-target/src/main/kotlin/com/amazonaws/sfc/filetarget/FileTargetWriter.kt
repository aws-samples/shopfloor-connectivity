
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.filetarget

import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.filetarget.config.FileTargetConfiguration
import com.amazonaws.sfc.filetarget.config.FileTargetWriterConfiguration
import com.amazonaws.sfc.filetarget.config.FileTargetWriterConfiguration.Companion.FILE_TARGET
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_MESSAGES
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITES
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_DURATION
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_ERRORS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_SIZE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_SUCCESS
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.system.DateTime.systemCalendar
import com.amazonaws.sfc.system.DateTime.systemCalendarUTC
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.byteCountString
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.io.BufferedWriter
import java.io.File
import java.io.File.separator
import java.io.OutputStreamWriter
import java.util.*
import kotlin.io.path.*

/**
 * Implements a file target writer that writes messages to a logger.
 * @property logger Logger Logger for output
 */
class FileTargetWriter(
    private val targetID: String,
    private val configReader: ConfigReader,
    private val logger: Logger,
    resultHandler: TargetResultHandler?) : TargetWriter {

    private val className = this::class.java.simpleName

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val metricDimensions = mapOf(METRICS_DIMENSION_SOURCE to targetID,
        MetricsCollector.METRICS_DIMENSION_TYPE to className)

    private val targetDataChannel = Channel<TargetData>(1000)

    private val buffer = TargetDataBuffer(storeFullMessage = false)
    private val targetResults = if (resultHandler != null) TargetResultBufferedHelper(targetID, resultHandler, logger) else null

    private val metricsCollector: MetricsCollector? by lazy {
        val metricsConfiguration = config.targets[targetID]?.metrics ?: MetricsSourceConfiguration()
        if (config.isCollectingMetrics) {
            logger.metricsCollectorMethod = collectMetricsFromLogger
            MetricsCollector(metricsConfig = config.metrics,
                metricsSourceName = targetID,
                metricsSourceType = MetricsSourceType.TARGET_WRITER,
                metricsSourceConfiguration = metricsConfiguration,
                staticDimensions = TARGET_METRIC_DIMENSIONS,
                logger = logger)
        } else null
    }

    private val collectMetricsFromLogger: MetricsCollectorMethod? =
        if (config.isCollectingMetrics) {
            { metricsList ->
                try {
                    val dataPoints = metricsList.map { MetricsDataPoint(it.metricsName, metricDimensions, it.metricUnit, it.metricsValue) }
                    runBlocking {
                        metricsCollector?.put(targetID, dataPoints)
                    }
                } catch (e: java.lang.Exception) {
                    logger.getCtxErrorLog(this::class.java.simpleName, "collectMetricsFromLogger")("Error collecting metrics from logger, $e")
                }
            }
        } else null

    override val metricsProvider: MetricsProvider? by lazy {
        if (metricsCollector != null) InProcessMetricsProvider(metricsCollector!!, logger) else null
    }

    private val scope = buildScope("File Target")

    private val writer = scope.launch(context = Dispatchers.IO, name = "Writer") {

        var timer = timerJob()

        val log = logger.getCtxLoggers(FileTargetWriter::class.java.simpleName, "writer")
        log.info("File writer for target \"$targetID\" writing to directory \"${targetConfig.directory}\"")

        while (isActive) {
            try{
            select {
                targetDataChannel.onReceive { targetData ->

                    val content = targetData.toJson(config.elementNames)

                    buffer.add(targetData, content)

                    log.trace("Received message, buffer size is ${buffer.payloadSize.byteCountString}")

                    // flush if reached buffer size
                    if (buffer.payloadSize >= targetConfig.bufferSize) {
                        log.trace("${targetConfig.bufferSize.byteCountString} buffer size reached, flushing buffer")
                        timer.cancel()
                        flush()
                        timer = timerJob()
                    }
                }
                timer.onJoin {
                    log.trace("${targetConfig.interval / 1000} seconds buffer interval reached, flushing buffer")
                    flush()
                    timer = timerJob()
                }
                }
            }catch (e: CancellationException) {
                log.info("Writer stopped")
            }catch (e : Exception){
                log.error("Error in writer, $e")
            }
        }

    }

    private fun CoroutineScope.timerJob() = launch("Timeout Timer") {
        try {
            delay(targetConfig.interval.toLong())
        } catch (e: Exception) {
            // No harm done, time s just used to check for timeouts
        }
    }


    /**
     * Write message to file target buffer.
     * @param targetData TargetData
     */
    override suspend fun writeTargetData(targetData: TargetData) {
        targetDataChannel.send(targetData)
    }

    private fun flush() {

        val log = logger.getCtxLoggers(className, "flush")
        if (buffer.size == 0) {
            return
        }

        val tc = targetConfig
        val outputFile = buildFilePath(tc)
        log.info("Writing ${buffer.payloadSize.byteCountString} of data to \"$outputFile\"")

        val start = DateTime.systemDateTime().toEpochMilli()

        try {
            writeToFile(outputFile, tc)
            val writeDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()
            createMetrics(targetID, metricDimensions, writeDurationInMillis)

            targetResults?.ackBuffered()

        } catch (e: Throwable) {
            log.error("Error writing to file \"$outputFile\" for target \"$targetID\", ${e.message}")
            runBlocking { metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions) }
            targetResults?.errorBuffered()
        } finally {
            buffer.clear()
        }
    }

    private fun createMetrics(adapterID: String,
                              metricDimensions: MetricDimensions,
                              writeDurationInMillis: Double) {

        runBlocking {
            metricsCollector?.put(adapterID,
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITES, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_MESSAGES, buffer.size.toDouble(), MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_DURATION, writeDurationInMillis, MetricUnits.MILLISECONDS, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SIZE, buffer.payloadSize.toDouble(), MetricUnits.BYTES, metricDimensions))
        }

    }

    private fun removeExtension(fileName: String): String {
        val lastIndex = fileName.lastIndexOf('.')
        if (lastIndex != -1) {
            return fileName.substring(0, lastIndex)
        }
        return fileName
    }

    private fun writeToFile(outputFile: String, tc: FileTargetConfiguration) {
        val dir = Path(outputFile).parent
        if (!dir.exists()) {
            dir.createDirectories()
        }

        var firstLine = true

        val file = File(outputFile).outputStream()
        val outputStream = BufferedWriter(OutputStreamWriter(
            if (targetConfig.compressionType == CompressionType.NONE) file
            else
                try {
                    Compress.createCompression(targetConfig.compressionType, file, if (targetConfig.json) "${removeExtension(Path(outputFile).name)}.json" else "").compressionStream
                } catch (e: Exception) {
                    logger.getCtxErrorLog("Error creating compression for type ${targetConfig.compressionType.name} ($e), writing to uncompressed file $outputFile instead")
                    file
                }))

        outputStream.use { f ->
            if (tc.json) {
                f.write("[")
            }
            buffer.payloads.forEach { line ->
                if (!firstLine) {
                    if (tc.json) {
                        f.write(",")
                    }
                    f.newLine()
                } else {
                    firstLine = false
                }
                f.write(line)
            }
            if (tc.json) {
                f.write("]")
            }
            f.newLine()
        }
    }


    private fun buildExtension(targetConfig: FileTargetConfiguration): String {
        if (targetConfig.compressionType == CompressionType.NONE) return targetConfig.extension
        return targetConfig.extension.ifBlank { targetConfig.compressionType.extension }
    }

    private fun buildFilePath(targetConfig: FileTargetConfiguration): String {
        val now = if (targetConfig.utcTime) systemCalendarUTC() else systemCalendar()

        return "${targetConfig.directory.absolutePathString()}$separator${
            now.get(Calendar.YEAR)
        }$separator${
            now.get(Calendar.MONTH) + 1
        }$separator${
            now.get(Calendar.DAY_OF_MONTH)
        }$separator${
            now.get(Calendar.HOUR_OF_DAY)
        }$separator${
            now.get(Calendar.MINUTE)
        }$separator${
            UUID.randomUUID()
        }${buildExtension(targetConfig)}"
    }

    /**
     * Closes target.
     */
    override suspend fun close() {
        flush()
        writer.cancel()
    }

    private val config: FileTargetWriterConfiguration
        get() = try {
            configReader.getConfig()
        } catch (e: Exception) {
            throw TargetException("Could not load $FILE_TARGET Target configuration: ${e.message}")
        }


    private val targetConfig: FileTargetConfiguration
        get() = config.targets[targetID]
                ?: throw TargetException("Configuration for type $FILE_TARGET for target with ID \"$targetID\" does not exist, existing targets are ${config.targets.keys}")


    companion object {
        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any?) =
            newInstance(createParameters[0] as ConfigReader, createParameters[1] as String, createParameters[2] as Logger, createParameters[3] as TargetResultHandler?)

        /**
         * Creates a new instance of a file target .
         * @param configReader ConfigReader Reader for reading target configuration
         * @param targetID String ID of the target
         * @param logger Logger Logger for output
         * @return TargetWriter Created target writer
         * @throws Exception
         */
        @JvmStatic
        fun newInstance(configReader: ConfigReader, targetID: String, logger: Logger, resultHandler: TargetResultHandler?): TargetWriter {

            return FileTargetWriter(targetID, configReader, logger, resultHandler)
        }

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET)
    }

}