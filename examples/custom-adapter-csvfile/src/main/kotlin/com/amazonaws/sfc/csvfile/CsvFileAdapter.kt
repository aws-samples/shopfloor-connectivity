// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.csvfile


import com.amazonaws.csvfile.sfc.config.CsvFileAdapterFileConfiguration
import com.amazonaws.csvfile.sfc.config.CsvFileChannelConfiguration
import com.amazonaws.csvfile.sfc.config.CsvFileConfiguration
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_READS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_READ_DURATION
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_READ_SUCCESS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_VALUES_READ
import com.amazonaws.sfc.system.DateTime
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import kotlin.time.Duration


class CsvFileAdapter(private val adapterID: String, private val configuration: CsvFileConfiguration, private val logger: Logger) : ProtocolAdapter {

    private val className = this::class.java.simpleName
    private val adapterMetricDimensions = mapOf(MetricsCollector.METRICS_DIMENSION_TYPE to className)
    private val sources
        get() = configuration.fileSources.filter { it.value.protocolAdapterID in configuration.fileProtocolAdapters.keys }

    override val metricsCollector: MetricsCollector? by lazy {
        val metricsConfigurations = configuration.fileProtocolAdapters.map { it.key to (it.value.metrics ?: MetricsSourceConfiguration()) }.toMap()
        if (configuration.isCollectingMetrics) {

            logger.metricsCollectorMethod = collectMetricsFromLogger

            MetricsCollector(metricsConfig = configuration.metrics,
                metricsSourceType = MetricsSourceType.PROTOCOL_ADAPTER,
                metricsSourceConfigurations = metricsConfigurations,
                staticDimensions = ADAPTER_METRIC_DIMENSIONS,
                logger = logger)
        } else null
    }

    /**
     * Reads a values from a source
     * @param sourceID String Source ID
     * @param channels List<String>? Channels to read values for, if null then all values for the source are read
     * @return SourceReadResult
     */
    override suspend fun read(sourceID: String, channels: List<String>?): SourceReadResult {
        val log = logger.getCtxLoggers(className, "read")
        log.trace("Reading ${if (channels.isNullOrEmpty()) "all" else channels.size} channels from CSV-FILE source \"$sourceID\"")

        val metricDimensions = mapOf(METRICS_DIMENSION_SOURCE to sourceID, MetricsCollector.METRICS_DIMENSION_ADAPTER to sources.keys.toString())
        val start = DateTime.systemDateTime().toEpochMilli()
        val skipper = getSourceFileConfiguration(sourceID)?.linesToSkip
        val maxRows = getSourceFileConfiguration(sourceID)?.maxRowsPerRead
        val delim = getSourceFileConfiguration(sourceID)?.delimiter.toString()

        try {
            val readValues = mutableMapOf<String, ChannelReadValue>()
            getSourceChannelConfiguration(sourceID)?.forEach { channel ->
                val timestamp = DateTime.systemDateTime()
                val path = getSourceFileConfiguration(sourceID)?.path.toString()
                var colValueArray : Array<String> = emptyArray()
                readFileAsLinesUsingBufferedReader(path).forEach {line ->
                    colValueArray += line
                }

                if (skipper != null) {
                    if(skipper > 0){
                        colValueArray = colValueArray.drop(skipper).toTypedArray()
                    }
                }
                val value: Any?
                if(maxRows == 1) {
                    value = colValueArray
                        .last()
                        .split(delim)[channel.value.colIdx]
                } else {
                    var lastNcolValues : Array<Any?> = emptyArray()
                    getSourceFileConfiguration(sourceID)?.let { colValueArray.takeLast(it.maxRowsPerRead).forEach { row ->
                        lastNcolValues += row.split(getSourceFileConfiguration(sourceID)?.delimiter.toString())[channel.value.colIdx]
                    } }
                    value = lastNcolValues

                }
                val channelReadValue = ChannelReadValue(value, timestamp)
                readValues[channel.key] = channelReadValue
            }
            //getSourceFileConfiguration(sourceID)?.let { log.trace(it.path+": "+channelCsvColumnList.joinToString()) }
            log.trace("${readValues.size} values read from CSV file ${getSourceFileConfiguration(sourceID)?.path}")
            val readDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()
            createMetrics(adapterID, metricDimensions, readDurationInMillis, readValues)

            return SourceReadSuccess(readValues)
        } catch (e: Exception) {
            return SourceReadError("Error reading from File $sourceID, $e")
        }
    }

    /**
     * Read in lines from CSV file using java.io BufferedReader
     * @param fileName String
     * @return lines as List<String>
     */
    //TODO: check on implementing this using java.nio
    private fun readFileAsLinesUsingBufferedReader(fileName: String): List<String>
            = File(fileName).bufferedReader().readLines()

    /**
     * Get the CSVFILE Source Configuartion
     * @param sourceID String
     * @return Map<String, CsvFileChannelConfiguration>
     */
    private fun getSourceChannelConfiguration(sourceID: String): Map<String, CsvFileChannelConfiguration>? {
        return sources[sourceID]?.channels
    }


    /**
     * Get the CSVFILE Adapter FileConfiguration
     * @param sourceID String
     * @return CsvFileAdapterFileConfiguration
     */
    private fun getSourceFileConfiguration(sourceID: String): CsvFileAdapterFileConfiguration? {

        val log = logger.getCtxLoggers(className, "getSourceFileConfiguration")

        val sourceConfiguration = sources[sourceID]
        if (sourceConfiguration == null) {
            log.error("Source \"$sourceID\" does not exist, available sources are ${sources.keys}")
            return null
        }

        val adapterConfiguration = configuration.fileProtocolAdapters[sourceConfiguration.protocolAdapterID]
        if (adapterConfiguration == null) {
            log.error("Adapter \"${sourceConfiguration.protocolAdapterID}\" for  Source \"$sourceID\" does not exist, available adapters are ${configuration.fileProtocolAdapters.keys}")
            return null
        }
        val fileConfiguration = adapterConfiguration.files[sourceConfiguration.adapterCsvFile]
        if (fileConfiguration == null) {
            log.error("Device \"${sourceConfiguration.adapterCsvFile}\" Adapter \"${sourceConfiguration.protocolAdapterID}\" for  Source \"$sourceID\" does not exist, available devices are ${adapterConfiguration.files}")
            return null
        }


        return fileConfiguration
    }

    private val collectMetricsFromLogger: MetricsCollectorMethod? =
        if (configuration.isCollectingMetrics) {
            { metricsList ->
                try {
                    val dimensions = mapOf( METRICS_DIMENSION_SOURCE to adapterID) + adapterMetricDimensions
                    val dataPoints = metricsList.map { MetricsDataPoint(it.metricsName, dimensions, it.metricUnit, it.metricsValue) }
                    runBlocking {
                        metricsCollector?.put(adapterID, dataPoints)
                    }
                } catch (e: java.lang.Exception) {
                    logger.getCtxErrorLog(this::class.java.simpleName, "collectMetricsFromLogger")("Error collecting metrics from logger, $e")
                }
            }
        } else null




    private suspend fun createMetrics(protocolAdapterID: String,
                                      metricDimensions: MetricDimensions?,
                                      readDurationInMillis: Double,
                                      values: MutableMap<String, ChannelReadValue>) {
        metricsCollector?.put(protocolAdapterID,
            metricsCollector?.buildValueDataPoint(protocolAdapterID, METRICS_READS, 1.0, MetricUnits.COUNT, metricDimensions),
            metricsCollector?.buildValueDataPoint(protocolAdapterID, METRICS_READ_DURATION, readDurationInMillis, MetricUnits.MILLISECONDS, metricDimensions),
            metricsCollector?.buildValueDataPoint(protocolAdapterID, METRICS_VALUES_READ, values.size.toDouble(), MetricUnits.COUNT, metricDimensions),
            metricsCollector?.buildValueDataPoint(protocolAdapterID, METRICS_READ_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions))
    }




    /**
     * Stops the adapter
     * @param timeout Duration Timeout period to wait for adapter to stop
     */
    override suspend fun stop(timeout: Duration) {
        // TODO: not required imho...
    }



    companion object {

        private val createInstanceMutex = Mutex()

        @JvmStatic @Suppress("unused") fun newInstance(vararg createParams: Any) = newInstance(createParams[0] as ConfigReader, createParams[1] as String, createParams[2] as String, createParams[3] as Logger)


        /**
         * Creates a new reader for the File protocol adapter from its configuration
         * @param configReader ConfigReader Configuration reader for the adapter
         * @see com.amazonaws.csvfile.sfc.config.CsvFileConfiguration
         * @param scheduleName String Name of the schedule
         * @param logger Logger Logger for output
         * @return SourceValuesReader? Created reader
         */
        @JvmStatic fun newInstance(configReader: ConfigReader, scheduleName: String, adapterID: String, logger: Logger): SourceValuesReader? {


            runBlocking {
                createInstanceMutex.withLock {
                    if (adapter == null) {
                        adapter = createFileAdapter(adapterID, configReader, logger)
                    }
                }
            }

            val config = configReader.getConfig<CsvFileConfiguration>()
            val schedule = config.schedules.firstOrNull { it.name == scheduleName }
            val sourcesForAdapter = schedule?.sources?.filter { (config.fileSources[it.key]?.protocolAdapterID ?: "") == adapterID } ?: return null

            return if (adapter != null) InProcessSourcesReader.createInProcessSourcesReader(schedule, adapter!!, sourcesForAdapter, config.metrics, logger) else null

        }

        private var adapter: ProtocolAdapter? = null

        /**
         * Creates an FILE adapter from its configuration
         * @param configReader ConfigReader Reader for configuration
         * @param logger Logger Logger for output
         * @return ProtocolAdapter
         */
        fun createFileAdapter(adapterID: String, configReader: ConfigReader, logger: Logger): ProtocolAdapter {

            // obtain file configuration
            val config: CsvFileConfiguration = try {
                configReader.getConfig()
            } catch (e: Exception) {
                throw ProtocolAdapterException("Error loading configuration: ${e.message}")
            }
            return CsvFileAdapter(adapterID, config, logger)
        }

        private val ADAPTER_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER)

    }
}
