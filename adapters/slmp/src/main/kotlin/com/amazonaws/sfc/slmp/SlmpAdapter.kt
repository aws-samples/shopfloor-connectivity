// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.slmp


import com.amazonaws.sfc.config.BaseConfiguration.Companion.WILD_CARD
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
import com.amazonaws.sfc.slmp.config.SlmpAdapterConfiguration
import com.amazonaws.sfc.slmp.config.SlmpConfiguration
import com.amazonaws.sfc.slmp.config.SlmpControllerConfiguration
import com.amazonaws.sfc.slmp.config.SlmpSourceConfiguration
import com.amazonaws.sfc.slmp.config.SlmpSourceConfiguration.Companion.CONFIG_ADAPTER_CONTROLLER
import com.amazonaws.sfc.system.DateTime.systemDateTime
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.MemoryMonitor.Companion.getUsedMemoryMB
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.measureTime


class SlmpAdapter(
    private val adapterID: String,
    private val configuration: SlmpConfiguration,
    private val logger: Logger
) : ProtocolAdapter {

    private val className = this::class.java.simpleName

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val adapterMetricDimensions = mapOf(MetricsCollector.METRICS_DIMENSION_TYPE to className)

    private val sourceConfigurations: Map<String, SlmpSourceConfiguration>
        get() = configuration.sources.filter { it.value.protocolAdapterID in configuration.slmpProtocolAdapters.keys }

    private val slmpSources by lazy {
        sequence {
            sourceConfigurations.forEach { (sourceID) ->
                val slmpSource = createSlmpSource(sourceID)
                if (slmpSource != null) yield(sourceID to slmpSource)
            }
        }.toMap()
    }

    private fun createSlmpSource(sourceID: String): SlmpSource? {
        return try {
            val controllerConfiguration = controllerConfigurationForSource(sourceID)
            val slmpSourceConfiguration = getSourceConfiguration(sourceID)
            SlmpSource(
                sourceID = sourceID,
                controllerConfiguration = controllerConfiguration,
                sourceConfiguration = slmpSourceConfiguration,
                metricsCollector = metricsCollector,
                adapterMetricDimensions = adapterMetricDimensions,
                logger = logger
            )
        } catch (e: Exception) {
            logger.getCtxErrorLog(
                className,
                "createSlmpSource"
            )("Error creating SLMP source instance for source \"$sourceID\", ${e.message}")
            null
        }
    }


    private fun getSourceConfiguration(sourceID: String): SlmpSourceConfiguration {
        return sourceConfigurations[sourceID]
            ?: throw SlmpAdapterException("\"$sourceID\" is not a valid SLMP source, available SLMP sources are ${slmpSources.keys}")
    }

    private fun protocolAdapterForSource(sourceID: String): SlmpAdapterConfiguration {
        val sourceConfig = getSourceConfiguration(sourceID)
        return configuration.slmpProtocolAdapters[sourceConfig.protocolAdapterID]
            ?: throw SlmpAdapterException(
                "\"${sourceConfig.protocolAdapterID}\" for source \"$sourceID\" is not a valid SLMP protocol adapter, " +
                        "available SLMP protocol adapters are ${configuration.slmpProtocolAdapters.keys}"
            )
    }

    private fun controllerConfigurationForSource(sourceID: String): SlmpControllerConfiguration {
        val sourceConfig = getSourceConfiguration(sourceID)
        val slmpAdapterConfig = protocolAdapterForSource(sourceID)
        return (slmpAdapterConfig.controllers[sourceConfig.adapterDeviceID]
            ?: throw SlmpAdapterException("\"${sourceConfig.adapterDeviceID}\" is not a valid $CONFIG_ADAPTER_CONTROLLER for adapter \"${sourceConfig.protocolAdapterID}\" used by source \"$sourceID\", valid controller are ${slmpAdapterConfig.controllers.keys}"))
    }

    override val metricsCollector: MetricsCollector? by lazy {
        val metricsConfigurations =
            configuration.slmpProtocolAdapters.map { it.key to (it.value.metrics ?: MetricsSourceConfiguration()) }
                .toMap()
        if (configuration.isCollectingMetrics) {
            logger.metricsCollectorMethod = collectMetricsFromLogger
            MetricsCollector(
                metricsConfig = configuration.metrics,
                metricsSourceType = MetricsSourceType.PROTOCOL_ADAPTER,
                metricsSourceConfigurations = metricsConfigurations,
                staticDimensions = ADAPTER_METRIC_DIMENSIONS,
                logger = logger
            )
        } else null
    }

    private val collectMetricsFromLogger: MetricsCollectorMethod? =
        if (configuration.isCollectingMetrics) {
            { metricsList ->
                try {
                    val dimensions = mapOf(METRICS_DIMENSION_SOURCE to adapterID) + adapterMetricDimensions
                    val dataPoints =
                        metricsList.map { MetricsDataPoint(it.metricsName, dimensions, it.metricUnit, it.metricsValue) }
                    runBlocking {
                        metricsCollector?.put(adapterID, dataPoints)
                    }
                } catch (e: java.lang.Exception) {
                    logger.getCtxErrorLogEx(
                        className,
                        "collectMetricsFromLogger"
                    )("Error collecting metrics from logger", e)
                }
            }
        } else null


    override suspend fun read(sourceID: String, channels: List<String>?): SourceReadResult {

        val sourceConfiguration = sourceConfigurations[sourceID]
            ?: return SourceReadError("Source \"$sourceID\" does not exist, available SLMP sources are ${slmpSources.keys}")

        val protocolAdapterID = sourceConfiguration.protocolAdapterID

        val dimensions = mapOf(METRICS_DIMENSION_SOURCE to "$adapterID:$sourceID") + adapterMetricDimensions

        val slmpSource = slmpSources[sourceID] ?: return SourceReadError("Invalid source configuration")


        val channelsToRead = if (channels.isNullOrEmpty() || (channels.size == 1 && channels[0] == WILD_CARD)) {
            sourceConfiguration.channels.keys.toList()
        } else {
            channels
        }

        val sourceReadResult = try {
            var slmpSourceReadData: Map<String, ChannelReadValue>
            val duration = measureTime {
                slmpSourceReadData = slmpSource.read(channelsToRead)
            }
            createMetrics(protocolAdapterID, dimensions, duration, slmpSourceReadData)
            SourceReadSuccess(slmpSourceReadData, systemDateTime())
        } catch (e: Exception) {
            metricsCollector?.buildValueDataPoint(
                protocolAdapterID,
                MetricsCollector.METRICS_READ_ERRORS,
                1.0,
                MetricUnits.COUNT,
                dimensions
            )
            SourceReadError(e.toString(), systemDateTime())
        }


        return sourceReadResult
    }

    private suspend fun createMetrics(
        protocolAdapterID: String,
        metricDimensions: MetricDimensions?,
        duration: Duration,
        values: Map<String, ChannelReadValue>
    ) {

        val valueCount =
            if (values.isEmpty()) 0 else values.map { if (it.value.value is ArrayList<*>) (it.value.value as ArrayList<*>).size else 1 }
                .sum()
        metricsCollector?.put(
            protocolAdapterID,
            metricsCollector?.buildValueDataPoint(
                protocolAdapterID,
                MetricsCollector.METRICS_MEMORY,
                getUsedMemoryMB().toDouble(),
                MetricUnits.MEGABYTES,
                metricDimensions
            ),
            metricsCollector?.buildValueDataPoint(
                protocolAdapterID,
                MetricsCollector.METRICS_READS,
                1.0,
                MetricUnits.COUNT,
                metricDimensions
            ),
            metricsCollector?.buildValueDataPoint(
                protocolAdapterID,
                MetricsCollector.METRICS_READ_DURATION,
                duration.inWholeMilliseconds.toDouble(),
                MetricUnits.MILLISECONDS,
                metricDimensions
            ),
            metricsCollector?.buildValueDataPoint(
                protocolAdapterID,
                MetricsCollector.METRICS_VALUES_READ,
                valueCount.toDouble(),
                MetricUnits.COUNT,
                metricDimensions
            ),
            metricsCollector?.buildValueDataPoint(
                protocolAdapterID,
                MetricsCollector.METRICS_READ_SUCCESS,
                1.0,
                MetricUnits.COUNT,
                metricDimensions
            )
        )
    }

    /**
     * Stops the adapter
     * @param timeout Duration Timeout period to wait for adapter to stop
     */
    override suspend fun stop(timeout: Duration) {

        val log = logger.getCtxLoggers(className, "stop")

        runBlocking {

            slmpSources.forEach {
                try {
                    it.value.close()
                } catch (e: Exception) {
                    log.errorEx("Stopping SLMP Adapter Source ${it.key} failed", e)
                }
            }
        }
    }


    companion object {

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParams: Any) =
            newInstance(
                createParams[0] as ConfigReader,
                createParams[1] as String,
                createParams[2] as String,
                createParams[3] as Logger
            )

        private val createInstanceMutex = Mutex()

        @JvmStatic
        fun newInstance(
            configReader: ConfigReader,
            scheduleName: String,
            adapterID: String,
            logger: Logger
        ): SourceValuesReader? {

            runBlocking {
                createInstanceMutex.withLock {
                    if (adapter == null) {
                        adapter = createSlmpAdapter(adapterID, configReader, logger)
                    }
                }
            }

            val config = configReader.getConfig<SlmpConfiguration>()
            val schedule = config.schedules.firstOrNull { it.name == scheduleName }
            val sourcesForAdapter =
                schedule?.sources?.filter { (config.sources[it.key]?.protocolAdapterID ?: "") == adapterID }
                    ?: return null


            runBlocking {
                adapter?.init()
            }

            return if (adapter != null) InProcessSourcesReader.createInProcessSourcesReader(
                schedule = schedule,
                adapter = adapter!!,
                sources = sourcesForAdapter,
                tuningConfiguration = config.tuningConfiguration,
                metricsConfig = config.metrics,
                logger = logger
            ) else null

        }

        private var adapter: ProtocolAdapter? = null

        fun createSlmpAdapter(adapterID: String, configReader: ConfigReader, logger: Logger): ProtocolAdapter {

            val config: SlmpConfiguration = try {
                configReader.getConfig()
            } catch (e: Exception) {
                throw TargetException("Error loading configuration: ${e.message}")
            }
            return SlmpAdapter(adapterID, config, logger)
        }

        private val ADAPTER_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
        )

    }

}
