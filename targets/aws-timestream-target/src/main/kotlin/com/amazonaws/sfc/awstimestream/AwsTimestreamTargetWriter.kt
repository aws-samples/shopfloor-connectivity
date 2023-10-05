/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.awstimestream


import com.amazonaws.sfc.awstimestream.config.AwsTimestreamDimensionConfiguration
import com.amazonaws.sfc.awstimestream.config.AwsTimestreamRecordConfiguration
import com.amazonaws.sfc.awstimestream.config.AwsTimestreamTargetConfiguration
import com.amazonaws.sfc.awstimestream.config.AwsTimestreamWriterConfiguration
import com.amazonaws.sfc.awstimestream.config.AwsTimestreamWriterConfiguration.Companion.AWS_TIMESTREAM
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.DataTypes.isNumeric
import com.amazonaws.sfc.data.TargetData
import com.amazonaws.sfc.data.TargetResultBufferedHelper
import com.amazonaws.sfc.data.TargetResultHandler
import com.amazonaws.sfc.data.TargetWriter
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_ERRORS
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.targets.AwsServiceTargetClientHelper
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.canNotReachAwsService
import com.amazonaws.sfc.util.launch
import io.burt.jmespath.Expression
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.services.timestreamwrite.TimestreamWriteClient
import software.amazon.awssdk.services.timestreamwrite.model.*
import java.time.Instant
import kotlin.math.roundToLong


/**
 * AWS Timestream Target writer
 * @property targetID String ID of target
 * @property configReader configReader for reading target configuration
 * @property logger Logger Logger for output
 * @see TargetWriter
 */
class AwsTimestreamTargetWriter(
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

    private val clientHelper =
        AwsServiceTargetClientHelper(
            configReader.getConfig<AwsTimestreamWriterConfiguration>(),
            targetID,
            TimestreamWriteClient.builder(),
            logger
        )

    private val timestreamClient: AwsTimestreamClient
        get() = AwsTimestreamClientWrapper(clientHelper.serviceClient as TimestreamWriteClient)

    private val targetResults = if (resultHandler != null) TargetResultBufferedHelper(targetID, resultHandler, logger) else null
    private val scope = buildScope("Timestream Target")

    // Configured field names for values and timestamps
    private val elementNames = config.elementNames

    private val commonAttributes by lazy { buildCommonAttributes() }

    // channel for passing messages to coroutine that sends messages to timestream record queue
    private val targetDataChannel = Channel<TargetData>(100)

    // Buffer to collect batch of timestream records
    private val recordBuffer = mutableListOf<Record>()
    private var batchCount = 0

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


    // coroutine that writes messages to queue
    private val writer = scope.launch("Writer") {

        val log = logger.getCtxLoggers(AwsTimestreamTargetWriter::class.java.simpleName, "writer")

        log.info("AWS Timestream writer for target \"$targetID\" sending to database ${targetConfig.database}, table ${targetConfig.tableName} in region ${targetConfig.region}")

        var timer = timerJob()

        while (isActive) {
            select<Unit> {
                targetDataChannel.onReceive { targetData ->
                    timer.cancel()
                    handleTargetData(targetData)
                }
                timer.onJoin {
                    log.trace("${(targetConfig.interval.inWholeMilliseconds)} milliseconds buffer interval reached, flushing buffer")
                    flush()
                }
            }
            timer = timerJob()
        }

    }

    private fun handleTargetData(targetData: TargetData) {
        val data = targetData.toMap(config.elementNames, jmesPathCompatibleKeys = true)
        // For every configured record
        targetConfig.records.forEach { recordConfig ->

            val measuredValueAndTime = getMeasuredValueAndTime(data, recordConfig)
            if (measuredValueAndTime != null) {
                val dimensions = buildDimensions(recordConfig.dimensions, data)

                try {
                    val record = buildRecord(recordConfig, measuredValueAndTime.first, measuredValueAndTime.second, dimensions)
                    recordBuffer.add(record)
                } catch (e: Exception) {
                    logger.getCtxErrorLog(className, "handleTargetData")("Error building record $recordConfig from value ${measuredValueAndTime.first} (${measuredValueAndTime.first::class.java.simpleName}), ${e.message}")
                }
            }
        }

        targetResults?.add(targetData)
        // Keeps track of reads in batch, flush if configured value is reached
        batchCount += 1
        if (targetData.noBuffering || batchCount >= targetConfig.batchSize) {
            flush()
            batchCount = 0
        }
    }

    private fun CoroutineScope.timerJob(): Job {
        return launch("Timeout timer") {

            return@launch try {
                delay(targetConfig.interval)
            } catch (e: Exception) {
                // no harm done, timer is just used to guard for timeouts
            }
        }
    }

    /**
     * Writes message to internal Timestream target
     * @param targetData TargetData
     */
    override suspend fun writeTargetData(targetData: TargetData) {
        targetDataChannel.send(targetData)
    }

    /**
     * Closes the writer
     */
    override suspend fun close() {
        flush()
        writer.cancel()
        timestreamClient.close()
    }

    private fun buildCommonAttributes(): Record {
        val recordBuilder = Record.builder()

        // Same time unit is used for all records
        recordBuilder.timeUnit(TimeUnit.MILLISECONDS)

        // single record, measure name is common
        if (targetConfig.records.count() == 1) {
            recordBuilder.measureName(targetConfig.records[0].measureName)
        }

        // test if all records have common type for measure value
        val firstMeasureDataType = targetConfig.records.first().measureValueType
        if (targetConfig.records.all { it.measureValueType == firstMeasureDataType }) {
            recordBuilder.measureValueType(firstMeasureDataType)
        }

        // Find for dimensions that have the same name and constant value
        val firstDimensions = targetConfig.records.first().dimensions

        val commonDimensions = sequence {
            firstDimensions.filter { it.dimensionValue != null }.forEach {
                if (targetConfig.records.all { r -> r.dimensions.any { d -> ((d.dimensionValue == it.dimensionValue) && (d.dimensionName == it.dimensionName)) } }) {
                    yield(
                        Dimension.builder()
                            .value(it.dimensionValue)
                            .dimensionValueType(DimensionValueType.VARCHAR)
                            .name(it.dimensionName)
                            .build())
                }
            }
        }.toList()

        if (commonDimensions.isNotEmpty()) {
            recordBuilder.dimensions(commonDimensions)
        }

        return recordBuilder.build()
    }

    private fun buildDimensions(dimensions: List<AwsTimestreamDimensionConfiguration>?, data: Map<String, Any>) = sequence {

        val log = logger.getCtxLoggers(className, "buildDimensions")

        fun search(dimension: AwsTimestreamDimensionConfiguration): Any? =
            try {
                dimension.dimensionValuePath?.search(data)
            } catch (e: NullPointerException) {
                null
            } catch (e: Exception) {
                log.error("Error querying dimension \"$dimension\" using \"${dimension.dimensionValuePathStr}\"for target \"$targetID\", $e")
                null
            }

        dimensions?.forEach { dimension ->

            if (commonAttributes.dimensions().find {
                    it.name() == dimension.dimensionName &&
                    it.value() == dimension.dimensionValue
                } == null) {

                val dimensionValue = if (dimension.dimensionValue != null) dimension.dimensionValue else search(dimension)
                if (dimensionValue != null) {
                    yield(Dimension.builder().value(dimensionValue.toString()).name(dimension.dimensionName).build())
                } else {
                    log.warning("No value found for dimension $dimension, target \"$targetID\"")
                }
            }
        }
    }


    private fun buildRecord(
        recordConfig: AwsTimestreamRecordConfiguration,
        measureValue: Any?,
        measureTime: Instant?,
        dimensions: Sequence<Dimension>
    ): Record {

        val recordBuilder = Record.builder()

        if (commonAttributes.measureName() != recordConfig.measureName) {
            recordBuilder.measureName(recordConfig.measureName)
        }

        if (commonAttributes.measureValueType() != recordConfig.measureValueType) {
            recordBuilder.measureValueType(recordConfig.measureValueType)
        }

        if (dimensions.iterator().hasNext()) {
            recordBuilder.dimensions(dimensions.toList())
        }

        if (measureValue != null) {
            recordBuilder.measureValue(measureValueString(recordConfig.measureValueType, measureValue))
        }

        if (measureTime != null) {
            recordBuilder.time(measureTime.toEpochMilli().toString())
        }

        return recordBuilder.build()
    }


    private fun getMeasuredValueAndTime(data: Map<String, Any>, recordConfig: AwsTimestreamRecordConfiguration): Pair<Any, Instant>? {

        val trace = logger.getCtxTraceLog(className, "getMeasuredValueAndTime")

        // internal method for searching measure value and timestamp
        fun search(query: Expression<Any>?): Any? =
            try {
                query?.search(data)
            } catch (e: NullPointerException) {
                null
            } catch (e: Exception) {
                logger.getCtxErrorLog(className, "getMeasuredValueAndTime")("Error querying value for target \"$targetID\", record $recordConfig, $e")
                null
            }

        // internal method for fetching a specified value from input value this is a map
        fun valueFromMap(v: Any?, key: String): Any? = if ((v != null) && (v is Map<*, *>) && (v.containsKey(key))) v[key] else null

        // Search measure value
        val dataSearchResult = search(recordConfig.measureValuePath)
        // If the value is a map the value can be in the value field
        val measureValue: Any? = (valueFromMap(dataSearchResult, elementNames.value)) ?: dataSearchResult

        // No measure valueFromMap for this record
        if (measureValue == null) {
            trace("No measure value for \"${recordConfig.measureName}\" , using path \"${recordConfig.measureValuePathStr}")
            return null
        }
        trace("Measure value $measureValue $measureValue (${measureValue::class.java.simpleName}) for measure value ${recordConfig.measureName}")


        val timestampPath = recordConfig.measureTimePath
        // Test if there is an explicit timestamp query, in that case use it to query for the timestamp
        val measureTime: Instant? = if (timestampPath != null) {
            val timestampSearchResult = search(timestampPath) as? Instant?
            (valueFromMap(timestampSearchResult, elementNames.timestamp) as? Instant?) ?: timestampSearchResult
        } else {
            // else test if it was included as a timestamp field with the value
            (valueFromMap(dataSearchResult, elementNames.timestamp) as? Instant?)
        }
        if (measureTime == null) {
            trace("No measure time found for measure value ${recordConfig.measureName}")
            return null
        }

        trace("Measure time $measureTime found for measure value ${recordConfig.measureName}")

        return measureValue to measureTime

    }


    /**
     * Writes all buffered values to Timestream in optimized batch calls
     */
    private fun flush() {

        val log = logger.getCtxLoggers(className, "flush")

        // No buffered messages, nothing to do
        if (recordBuffer.isEmpty()) {
            return
        }

        // Builds request with max 100 records
        buildRequests().forEach { request ->
            try {

                val start = DateTime.systemDateTime().toEpochMilli()
                clientHelper.executeServiceCallWithRetries {
                    try {
                        timestreamClient.writeRecords(request)
                        val writeDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()
                        createMetrics(targetID, metricDimensions, request, writeDurationInMillis)

                        targetResults?.ackBuffered()
                    } catch (e: RejectedRecordsException) {
                        e.rejectedRecords().forEach {
                            val record = request.records()[it.recordIndex()]
                            log.error("Rejected record $record, ${it.reason()}")
                        }
                    } catch (e: AwsServiceException) {
                        log.trace("Timestream writeRecords error ${e.message}")
                        // Check the exception, it will throw an AwsServiceRetryableException if the error is recoverable
                        clientHelper.processServiceException(e)
                        // Non recoverable service exceptions
                        throw e
                    }
                }

                log.trace("Timestream WriteRecords succeeded")

            } catch (e: Exception) {
                log.error("Error writing Timestream database \"${targetConfig.database}\", table \"${targetConfig.tableName}\" , ${e.message}")
                runBlocking { metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions) }
                if (canNotReachAwsService(e)) {
                    targetResults?.nackBuffered()
                } else {
                    targetResults?.errorBuffered()
                }
            }
        }
        recordBuffer.clear()
    }

    private fun createMetrics(adapterID: String,
                              metricDimensions: MetricDimensions,
                              request: WriteRecordsRequest,
                              writeDurationInMillis: Double) {

        runBlocking {
            metricsCollector?.put(adapterID,
                metricsCollector?.buildValueDataPoint(adapterID, MetricsCollector.METRICS_WRITES, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, MetricsCollector.METRICS_MESSAGES, request.records().size.toDouble(), MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, MetricsCollector.METRICS_WRITE_DURATION, writeDurationInMillis, MetricUnits.MILLISECONDS, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, MetricsCollector.METRICS_WRITE_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, MetricsCollector.METRICS_WRITE_SIZE, request.toString().length.toDouble(), MetricUnits.BYTES, metricDimensions))
        }
    }

    /**
     *
     * @return Sequence<WriteRecordsRequest>
     */
    private fun buildRequests() = sequence<WriteRecordsRequest> {
        recordBuffer.chunked(100).forEach { records ->
            yield(
                WriteRecordsRequest.builder()
                    .databaseName(targetConfig.database)
                    .tableName(targetConfig.tableName)
                    .commonAttributes(commonAttributes)
                    .records(records)
                    .build()
            )
        }
    }

    /**
     * Gets the config for Timestream Target writer
     */
    private val config: AwsTimestreamWriterConfiguration
        get() {
            return clientHelper.writerConfig(configReader, AWS_TIMESTREAM)
        }

    /**
     * Gets the config for the Timestream Target
     */
    private val targetConfig: AwsTimestreamTargetConfiguration by lazy {
        clientHelper.targetConfig(config, targetID, AWS_TIMESTREAM)
    }


    companion object {

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any?) =
            newInstance(createParameters[0] as ConfigReader, createParameters[1] as String, createParameters[2] as Logger, createParameters[3] as TargetResultHandler?)

        @JvmStatic
        fun newInstance(configReader: ConfigReader, targetID: String, logger: Logger, resultHandler: TargetResultHandler?): TargetWriter {
            return try {
                AwsTimestreamTargetWriter(targetID, configReader, logger, resultHandler)
            } catch (e: Throwable) {
                throw TargetException("Error creating AWS Timestream target writer, ${e.message}")
            }
        }

        private fun measureValueString(dataType: MeasureValueType, value: Any): String =
            when (dataType) {
                MeasureValueType.BIGINT -> timestreamBigInt(value)
                MeasureValueType.DOUBLE -> timestreamDouble(value)
                MeasureValueType.BOOLEAN -> timestreamBoolean(value)
                else -> value.toString()
            }

        private fun timestreamBoolean(value: Any) = when (value) {
            is String -> when (value.lowercase()) {
                "true", "1", "1.0" -> true.toString()
                "false", "0", "0.0" -> false.toString()
                else -> value.toString()
            }

            (isNumeric(value::class)) -> {
                value.toString()
            }

            else -> value.toString()
        }

        private fun timestreamDouble(value: Any) = when (value) {
            is Byte -> value.toDouble().toString()
            is UByte -> value.toDouble().toString()
            is Short -> value.toDouble().toString()
            is UShort -> value.toDouble().toString()
            is Int -> value.toDouble().toString()
            is UInt -> value.toDouble().toString()
            is Long -> value.toDouble().toString()
            is Boolean -> if (value) "1.0" else "0.0"
            else -> value.toString()
        }

        private fun timestreamBigInt(value: Any) = when (value) {
            is Double -> value.roundToLong().toString()
            is Float -> value.roundToLong().toString()
            is Boolean -> if (value) "1" else "0"
            else -> value.toString()
        }

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET)

    }
}












