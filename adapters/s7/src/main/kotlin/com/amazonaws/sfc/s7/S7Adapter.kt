/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.s7


import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_READS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_READ_DURATION
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_READ_ERRORS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_READ_SUCCESS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_VALUES_READ
import com.amazonaws.sfc.s7.config.S7Configuration
import com.amazonaws.sfc.s7.config.S7ControllerConfiguration
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.util.LookupCacheHandler
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import kotlin.time.Duration


class S7Adapter(private val adapterID: String, private val configuration: S7Configuration, private val logger: Logger) : ProtocolAdapter {

    private val className = this::class.java.simpleName

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val adapterMetricDimensions = mapOf(MetricsCollector.METRICS_DIMENSION_TYPE to className)

    private val controllers = LookupCacheHandler<String, S7Controller?, S7Configuration>(
        supplier = { sourceID ->
            createS7ControllerForSource(sourceID)
        },
        isValid = { controller: S7Controller? -> (controller != null) }
    )

    private val s7Sources
        get() = configuration.sources.filter { it.value.protocolAdapterID in configuration.s7ProtocolAdapters.keys }

    private val sourceWaitUntil: MutableMap<String, Instant> = s7Sources.map {
        it.key to Instant.ofEpochSecond(0L)
    }.toMap().toMutableMap()


    override val metricsCollector: MetricsCollector? by lazy {
        val metricsConfigurations = configuration.s7ProtocolAdapters.map {
            it.key to (it.value.metrics ?: MetricsSourceConfiguration())
        }.toMap()
        if (configuration.isCollectingMetrics) {
            logger.metricsCollectorMethod = collectMetricsFromLogger
            MetricsCollector(metricsConfig = configuration.metrics,
                metricsSourceType = MetricsSourceType.PROTOCOL_ADAPTER,
                metricsSourceConfigurations = metricsConfigurations,
                staticDimensions = ADAPTER_METRIC_DIMENSIONS,
                logger = logger)
        } else null
    }

    private val collectMetricsFromLogger: MetricsCollectorMethod? =
        if (configuration.isCollectingMetrics) {
            { metricsList ->
                try {
                    val dimensions = mapOf(METRICS_DIMENSION_SOURCE to adapterID) + adapterMetricDimensions
                    val dataPoints = metricsList.map { MetricsDataPoint(it.metricsName, dimensions, it.metricUnit, it.metricsValue) }
                    runBlocking {
                        metricsCollector?.put(adapterID, dataPoints)
                    }
                } catch (e: java.lang.Exception) {
                    logger.getCtxErrorLog(this::class.java.simpleName, "collectMetricsFromLogger")("Error collecting metrics from logger, $e")
                }
            }
        } else null

    override suspend fun read(sourceID: String, channels: List<String>?): SourceReadResult {

        val log = logger.getCtxLoggers(className, "read")

        log.trace("Reading ${if (channels.isNullOrEmpty()) "all configured " else channels.size} channels from S7 source \"$sourceID\"")

        val dimensions = mapOf(METRICS_DIMENSION_SOURCE to "$adapterID:$sourceID") + adapterMetricDimensions

        return try {

            // skip reading for a period after an earlier error
            if (DateTime.systemDateTime() < sourceWaitUntil[sourceID]) {
                log.trace("Skipped reading for source \"$sourceID\" after previous read error ")
                return SourceReadSuccess(emptyMap())
            }

            val controller = getControllerForSource(sourceID)
            if (controller == null) {
                logger.getCtxErrorLog(className, "read")("Error reading from controller for adapter \"$adapterID\" source \"$sourceID\"")
                return SourceReadError("Can not read from controller for $sourceID")
            }
            val source = s7Sources[sourceID] ?: throw ProtocolAdapterException("S7 \"$sourceID\" does not exist, available sources are ${s7Sources.keys}")

            controller.lock.withLock {

                val fieldChannels = source.channels.filter { channels.isNullOrEmpty() || channels.contains(it.key) }

                val start = DateTime.systemDateTime().toEpochMilli()
                val values = controller.read(fieldChannels, dimensions, metricsCollector).map { (channelId, value) ->
                    if (value == null) {
                        log.trace("No data for channel \"$channelId\"")
                        null
                    } else {
                        val channelReadValue = ChannelReadValue(value)
                        log.trace("Value for channel \"$channelId\" is \"$channelReadValue\"")
                        channelId to channelReadValue
                    }
                }.filterNotNull().toMap()

                val readDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()

                createMetrics(adapterID, dimensions, readDurationInMillis, values)

                log.trace("${values.size} values read from S7 controller \"${source.sourceAdapterControllerID}\" for source \"$sourceID\"")
                SourceReadSuccess(values)
            }
        } catch (e: Exception) {
            sourceWaitUntil[sourceID] = DateTime.systemDateTime().plusMillis(configuration.s7ProtocolAdapters[adapterID]?.waitAfterErrors!!.inWholeMilliseconds)
            metricsCollector?.put(adapterID, METRICS_READ_ERRORS, 1.0, MetricUnits.COUNT, dimensions)
            SourceReadError("Error reading from S7 controller for source $sourceID, $e")
        }
    }

    private suspend fun createMetrics(adapterID: String,
                                      metricDimensions: MetricDimensions?,
                                      readDurationInMillis: Double,
                                      values: Map<String, ChannelReadValue>) {
        metricsCollector?.put(adapterID,
            metricsCollector?.buildValueDataPoint(adapterID, METRICS_READS, 1.0, MetricUnits.COUNT, metricDimensions),
            metricsCollector?.buildValueDataPoint(adapterID, METRICS_READ_DURATION, readDurationInMillis, MetricUnits.MILLISECONDS, metricDimensions),
            metricsCollector?.buildValueDataPoint(adapterID, METRICS_VALUES_READ, values.size.toDouble(), MetricUnits.COUNT, metricDimensions),
            metricsCollector?.buildValueDataPoint(adapterID, METRICS_READ_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions))
    }

    override suspend fun stop(timeout: Duration) {
        controllers.items.forEach {
            withTimeoutOrNull(timeout) { it }?.close()
        }
    }

    private fun createS7ControllerForSource(sourceID: String): S7Controller? {
        val controllerConfiguration = getSourceControllerConfigurationForSource(sourceID) ?: return null
        return S7Controller(adapterID, sourceID, controllerConfiguration, logger)
    }

    private suspend fun getControllerForSource(sourceID: String): S7Controller? {
        return controllers.getItemAsync(sourceID, configuration).await()
    }

    private fun getSourceControllerConfigurationForSource(sourceID: String): S7ControllerConfiguration? {

        val log = logger.getCtxLoggers(className, "getSourceControllerConfiguration")

        val sourceConfiguration = s7Sources[sourceID]
        if (sourceConfiguration == null) {
            log.error("Source \"$sourceID\" does not exist, available sources are ${s7Sources.keys}")
            return null
        }

        val adapterConfiguration = configuration.s7ProtocolAdapters[sourceConfiguration.protocolAdapterID]
        if (adapterConfiguration == null) {
            log.error("Adapter \"${sourceConfiguration.protocolAdapterID}\" for  Source \"$sourceID\" does not exist, " +
                      "available adapters are ${configuration.s7ProtocolAdapters.keys}")
            return null
        }
        val controllerConfiguration = adapterConfiguration.controllers[sourceConfiguration.sourceAdapterControllerID]
        if (controllerConfiguration == null) {
            log.error("Device \"${sourceConfiguration.sourceAdapterControllerID}\" Adapter \"${sourceConfiguration.protocolAdapterID}\" " +
                      "for  Source \"$sourceID\" does not exist, available devices are ${adapterConfiguration.controllers.keys}")
            return null
        }

        return controllerConfiguration
    }


    companion object {

        // needed to keep driver PLC4J driver loaded
        private val _s7 = CustomS7Driver { }

        private val createInstanceMutex = Mutex()


        @JvmStatic @Suppress("unused")
        fun newInstance(vararg createParams: Any) = newInstance(createParams[0] as ConfigReader, createParams[1] as String, createParams[2] as String, createParams[3] as Logger)

        @JvmStatic fun newInstance(configReader: ConfigReader, scheduleName: String, adapterID: String, logger: Logger): SourceValuesReader? {

            runBlocking {
                createInstanceMutex.withLock {
                    if (adapter == null) {
                        adapter = createS7Adapter(adapterID, configReader, logger)
                    }
                }
            }

            val config = configReader.getConfig<S7Configuration>()
            val schedule = config.schedules.firstOrNull { it.name == scheduleName }
            val sourcesForAdapter = schedule?.sources?.filter { (config.sources[it.key]?.protocolAdapterID ?: "") == adapterID } ?: return null

            return if (adapter != null) InProcessSourcesReader.createInProcessSourcesReader(schedule, adapter!!, sourcesForAdapter, config.metrics, logger) else null

        }

        private var adapter: ProtocolAdapter? = null


        fun createS7Adapter(adapterID: String, configReader: ConfigReader, logger: Logger): ProtocolAdapter {

            val config: S7Configuration = try {
                configReader.getConfig()
            } catch (e: Exception) {
                throw ProtocolAdapterException("Error loading configuration: ${e.message}")
            }
            return S7Adapter(adapterID, config, logger)
        }

        val ADAPTER_METRIC_DIMENSIONS = mapOf(
            METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER)

    }
}
