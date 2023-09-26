/*
 Copyright (c) 2021. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.debugtarget

import com.amazonaws.sfc.awsdebugtarget.BuildConfig
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.TargetConfiguration
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.debugtarget.config.DebugTargetsConfiguration
import com.amazonaws.sfc.debugtarget.config.DebugTargetsConfiguration.Companion.DEBUG_TARGET
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.targets.TargetException
import kotlinx.coroutines.runBlocking

/**
 * Implements a debug target writer that writes messages to a logger.
 * @property logger Logger Logger for output
 */
class DebugTargetWriter(private val targetID: String,
                        private val configReader: ConfigReader,
                        private val logger: Logger,
                        resultHandler: TargetResultHandler?) : TargetWriter {


    private var _targetConfig: TargetConfiguration? = null

    private val className = this::class.java.simpleName

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val metricDimensions = mapOf(METRICS_DIMENSION_SOURCE to targetID,
        MetricsCollector.METRICS_DIMENSION_TYPE to className)

    private val targetResults = if (resultHandler != null) TargetResultHelper(targetID, resultHandler, logger) else null

    private val targetConfig: TargetConfiguration
        get() {
            if (_targetConfig == null) {
                _targetConfig = getTargetConfig(targetID)
            }
            return _targetConfig as TargetConfiguration
        }


    private val config: DebugTargetsConfiguration by lazy { configReader.getConfig() }

    private fun getTargetConfig(targetID: String): TargetConfiguration {
        val writeConfiguration: DebugTargetsConfiguration = configReader.getConfig()
        // get target configuration
        return writeConfiguration.targets[targetID]
               ?: throw TargetException("Configuration for type $DEBUG_TARGET for target with ID \"$targetID\" does not exist, existing targets are ${writeConfiguration.targets.keys}")
    }

    private val transformation by lazy { if (targetConfig.template != null) OutputTransformation(targetConfig.template!!, logger) else null }

    private fun buildPayload(targetData: TargetData): String =
        if (transformation == null) targetData.toJson(config.elementNames) else transformation!!.transform(targetData, config.elementNames) ?: ""

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

    /**
     * Write message to debug target.
     * @param targetData TargetData
     */
    override suspend fun writeTargetData(targetData: TargetData) {
        val data = buildPayload(targetData)
        val start = DateTime.systemDateTime().toEpochMilli()

        output(data.toString())
        val writeDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()
        createMetrics(targetID, metricDimensions, data, writeDurationInMillis)

        targetResults?.ack(targetData)
    }

    private fun createMetrics(adapterID: String,
                              metricDimensions: MetricDimensions,
                              data: String,
                              writeDurationInMillis: Double) {

        runBlocking {
            metricsCollector?.put(adapterID,
                metricsCollector?.buildValueDataPoint(adapterID, MetricsCollector.METRICS_WRITES, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, MetricsCollector.METRICS_MESSAGES, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, MetricsCollector.METRICS_WRITE_DURATION, writeDurationInMillis, MetricUnits.MILLISECONDS, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, MetricsCollector.METRICS_WRITE_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, MetricsCollector.METRICS_WRITE_SIZE, data.length.toDouble(), MetricUnits.BYTES, metricDimensions))
        }
    }

    /**
     * Closes target.
     */
    override suspend fun close() {
    }

    // logger function to write messages to logger info output
    val output = logger.getCtxInfoLog(this, "write")

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any?) =
            newInstance(createParameters[0] as ConfigReader, createParameters[1] as String, createParameters[2] as Logger, createParameters[3] as TargetResultHandler?)

        /**
         * Creates a new instance of a debugger target.
         * @param configReader ConfigReader Reader for reading target configuration
         * @param targetID String ID of the target
         * @param logger Logger Logger for output
         * @return TargetWriter Created target writer
         * @throws Exception
         */
        @JvmStatic
        fun newInstance(configReader: ConfigReader, targetID: String, logger: Logger, resultHandler: TargetResultHandler?): TargetWriter {

            val config: DebugTargetsConfiguration = try {
                configReader.getConfig()
            } catch (e: Exception) {
                throw TargetException("Could not load configuration for debug writer: ${e.message}")
            }


            if (config.targets.isEmpty() || (!config.targets.containsKey(targetID))) {
                throw TargetException("There are no targets configured for type \"$DEBUG_TARGET\" for target \"$targetID\"")
            }

            return DebugTargetWriter(targetID, configReader, logger, resultHandler)
        }

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET)


    }
}