/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.metrics


import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.util.buildScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import kotlin.time.toDuration

// Shortcut for dimensions name-value map
typealias MetricDimensions = Map<String, String>

class MetricsValueParam(val metricsName: String, val metricsValue: MetricsValue, val metricUnit: MetricUnits)
// call back method for embedded class instances without metrics collector to provide metrics values to owner class
typealias MetricsCollectorMethod = (metrics: List<MetricsValueParam>) -> Unit

// Extension method to get the size in bytes for set of dimensions
val MetricDimensions.dataSize: Int
    get() = this.entries.sumOf { e -> e.key.length + e.value.length }

/**
 * Class used for collecting metrics and buffering these until these are read
 * @property metricsConfig MetricsConfiguration? Configuration for metrics collection
 * @property metricsSourceConfigurations Map<String, MetricsSourceConfiguration> Map of source/target level metric configurations
 * @property staticDimensions Map<String, String>? Set of common dimension added to every metric
 * @property logger Logger for output
 */
class MetricsCollector(private val metricsConfig: MetricsConfiguration?,
                       private val metricsSourceType: MetricsSourceType,
                       private val metricsSourceConfigurations: Map<String, MetricsSourceConfiguration>,
                       private val staticDimensions: MetricDimensions? = null,
                       private val logger: Logger) : MetricsCollectorReader {

    private val className = this::class.java.simpleName

    // Secondary constructor for cases that only have a single metric config at target/source level
    constructor(metricsConfig: MetricsConfiguration?,
                metricsSourceName: String,
                metricsSourceConfiguration: MetricsSourceConfiguration,
                staticDimensions: MetricDimensions? = null,
                metricsSourceType: MetricsSourceType,
                logger: Logger) :
            this(metricsConfig = metricsConfig,
                metricsSourceType = metricsSourceType,
                metricsSourceConfigurations = mapOf(metricsSourceName to metricsSourceConfiguration),
                staticDimensions = staticDimensions,
                logger = logger)

    private val bufferMutex = Mutex()

    // stored metrics
    private var metricsCache = newMetricsCache()

    // builds a new metrics cache
    private fun newMetricsCache() = metricsSourceConfigurations.filterValues {
        it.enabled
    }.map { it.key to mutableListOf<MetricsDataPoint>() }.toMap()

    // tests is a metrics source is configured to collect metrics
    private fun isSourceCollectingMetrics(source: String) = metricsCache.keys.contains(source)

    val scope = buildScope("MetricsCollector")

    // this job runs with an interval to purge metrics that have not been read within the max metrics age avoiding
    // endless storage of metrics that are not read by a consumer
    private val cleaner = if (metricsConfig?.isCollectingMetrics == true) scope.launch {
        delay(METRICS_MAX_AGE.toDuration(DurationUnit.MINUTES))
        while (isActive) {
            val duration = measureTime {
                cleanUp()
            }
            delay(METRICS_CLEANUP_INTERVAL - duration)
        }
    } else null

    init {
        logger.getCtxTraceLog(className, "init")("MetricsCollector initialized for metric sources ${metricsSourceConfigurations.keys}")
    }

    // build and stores a single data point if metrics are enabled for the specified metrics source for a single value
    suspend fun put(
        metricSource: String,
        name: String,
        value: Double,
        units: MetricUnits,
        dimensions: MetricDimensions? = null,
        timestamp: Instant = metricDefaultDateTime()) {

        val dataPoint = buildValueDataPoint(
            metricSource,
            name = name,
            value = (value),
            units = units,
            dimensions = dimensions,
            timestamp = timestamp)

        if (dataPoint != null) {
            put(metricSource, dataPoint)
        }

    }

    // build a data point if metric collection is enabled for the metric source
    fun buildValueDataPoint(metricSource: String,
                            name: String,
                            value: Double,
                            units: MetricUnits,
                            dimensions: MetricDimensions? = null,
                            timestamp: Instant = metricDefaultDateTime()): MetricsDataPoint? =
        if (isSourceCollectingMetrics(metricSource))
            MetricsDataPoint(
                name = name,
                value = MetricsValue(value),
                units = units,
                dimensions = dimensions,
                timestamp = timestamp)
        else null

    // build and stores a single data point if metrics are enabled for the specified metrics source for a list of values and counts
    suspend fun put(
        metricSource: String,
        name: String,
        values: List<Double>,
        counts: List<Double>?,
        units: MetricUnits,
        dimensions: MetricDimensions?,
        timestamp: Instant = metricDefaultDateTime()
    ) {

        if (isSourceCollectingMetrics(metricSource)) {

            val dataPoint = MetricsDataPoint(
                name = name,
                value = MetricsValues(values.toList(), counts = counts?.toList()),
                units = units, dimensions = dimensions,
                timestamp = timestamp)
            put(metricSource, dataPoint)
        }
    }

    // build and stores a single data point if metrics are enabled for the specified metrics source for a statistics value
    suspend fun put(metricSource: String,
                    name: String,
                    statistics: MetricsStatistics,
                    units: MetricUnits,
                    dimensions: MetricDimensions? = null,
                    timestamp: Instant = metricDefaultDateTime()) {

        if (isSourceCollectingMetrics(metricSource)) {

            val dataPoint =
                MetricsDataPoint(
                    name = name,
                    value = statistics,
                    units = units,
                    dimensions = dimensions,
                    timestamp = timestamp)
            put(metricSource, dataPoint)
        }
    }

    // default datetime used for metrics
    private fun metricDefaultDateTime(): Instant {
        return DateTime.systemDateTimeUTC()
    }

    // stores a collection of data points
    suspend fun put(metricSource: String, vararg dataPoints: MetricsDataPoint?) {

        val data = dataPoints.filterNotNull()
        put(metricSource, data)
    }

    // stores a collection of data points
    suspend fun put(metricSource: String, dataPoints: List<MetricsDataPoint>) {

        if (dataPoints.isNotEmpty()) {
            bufferMutex.withLock {
                metricsCache[metricSource]?.addAll(dataPoints)
            }
        }
    }

    suspend fun put(metricSource: String, metricsData: MetricsData) {

        val dataPoints = metricsData.dataPoints.map {
            it.dimensions = (metricsData.commonDimensions ?: emptyMap()) + (it.dimensions ?: emptyMap())
            if (it.dimensions!!.isEmpty()) {
                it.dimensions = null
            }
            it
        }
        put(metricSource, *dataPoints.toTypedArray())

    }


    // cleanup of data points older than configured max age
    private suspend fun cleanUp() {
        val deleteBefore = DateTime.systemDateTimeUTC().minusSeconds(60L * METRICS_MAX_AGE)
        val log = logger.getCtxInfoLog(className, "cleanup")
        bufferMutex.withLock {
            metricsCache.values.forEach { entry ->
                val before = entry.size
                entry.removeIf { dataPoint -> dataPoint.timestamp.isBefore(deleteBefore) }
                val after = entry.size
                if (before != after) {
                    log("Purging uncollected data points older than $deleteBefore")
                    log("${before - after} of $before data points purged from metrics collector for metric sources ${metricsSourceConfigurations.keys.joinToString(separator = ", ") { "\"$it\"" }}")
                }
            }
        }
    }

    // reads stored metrics and clears buffer
    override suspend fun read(): List<MetricsData>? {
        if (metricsCache.isEmpty()) return null

        bufferMutex.withLock {
            val data = metricsCache.map { (source, entry) ->
                val commonDimensions =
                    ((staticDimensions ?: emptyMap()) +  // fixed adapter dimensions
                     (metricsSourceConfigurations[source]?.commonDimensions ?: emptyMap()) + // configured common dimensions for source
                     (metricsConfig?.commonDimensions ?: emptyMap())) // top level configured dimensions

                val (mergedCommonDimensions, optimizedDataPoints) = optimizeDataPoints(commonDimensions, entry)

                MetricsData(source = source,
                    sourceType = metricsSourceType,
                    dataPoints = optimizedDataPoints,
                    commonDimensions = mergedCommonDimensions.ifEmpty { null })
            }
            metricsCache = newMetricsCache()
            return data.filter { it.dataPoints.isNotEmpty() }
        }
    }

    // Find all common metrics and adds these to common dimensions to reduce data size of read data points
    private fun optimizeDataPoints(commonDimensions: MetricDimensions, dataPoints: List<MetricsDataPoint>): Pair<MetricDimensions, List<MetricsDataPoint>> {
        // count the occurrences for every dimension pair in the data points
        val dimensionOccurrences = dataPoints.flatMap { it.dimensions?.entries ?: emptyList() }.groupBy { it }.map { it.key to it.value.size }
        // get the dimensions which occur in every datapoint
        val dimensionOccurringInAllDatapoint = dimensionOccurrences.filter { it.second == dataPoints.size }.associate { it.first.key to it.first.value }

        // add these occurrence to a new list of commonDimensions
        val mergedCommonDimensions = commonDimensions + dimensionOccurringInAllDatapoint

        val optimizedDataPoints = dataPoints.map {
            // filter out dimensions that were move to common dimensions
            val dimensions = it.dimensions?.filter { d -> !dimensionOccurringInAllDatapoint.keys.contains(d.key) }
            MetricsDataPoint(
                name = it.name,
                dimensions = if (!dimensions.isNullOrEmpty()) dimensions else null,
                units = it.units,
                value = it.value,
                timestamp = it.timestamp)
        }

        return Pair(mergedCommonDimensions, optimizedDataPoints)
    }


    override fun close() {
        cleaner?.cancel()
    }


    companion object {
        const val METRICS_BYTES_RECEIVED = "BytesReceived"
        const val METRICS_BYTES_SEND = "BytesSend"
        const val METRICS_CONNECTION_ERRORS = "ConnectionErrors"
        const val METRICS_CONNECTIONS = "Connections"
        const val METRICS_ERRORS = "Errors"
        const val METRICS_MESSAGE_BUFFERED_COUNT = "MessagesBufferedCount"
        const val METRICS_MESSAGE_BUFFERED_DELETED = "MessagesBufferedDeleted"
        const val METRICS_MESSAGE_BUFFERED_SIZE = "MessagesBufferedSize"
        const val METRICS_MESSAGES = "Messages"
        const val METRICS_READ_DURATION = "ReadDuration"
        const val METRICS_READ_ERRORS = "ReadError"
        const val METRICS_READ_SUCCESS = "ReadSuccess"
        const val METRICS_READS = "Reads"
        const val METRICS_VALUES_READ = "ValuesRead"
        const val METRICS_WARNINGS = "Warnings"
        const val METRICS_WRITE_DURATION = "WriteDuration"
        const val METRICS_WRITE_ERRORS = "WriteError"
        const val METRICS_WRITE_SIZE = "BytesWritten"
        const val METRICS_WRITE_SUCCESS = "WriteSuccess"
        const val METRICS_WRITES = "Writes"

        const val METRICS_DIMENSION_ADAPTER = "Adapter"
        const val METRICS_DIMENSION_SOURCE = "Source"

        const val METRICS_DIMENSION_SOURCE_CATEGORY = "Category"
        const val METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER = "Adapter"
        const val METRICS_DIMENSION_SOURCE_CATEGORY_TARGET = "Target"
        const val METRICS_DIMENSION_SOURCE_CATEGORY_CORE = "Core"

        const val METRICS_DIMENSION_TYPE = "Type"


        val METRICS_CLEANUP_INTERVAL = 1.toDuration(DurationUnit.MINUTES)
        const val METRICS_MAX_AGE = 5

    }

}
