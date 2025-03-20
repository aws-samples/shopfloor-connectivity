// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.simulator


import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
import com.amazonaws.sfc.simulator.config.SimulationAdapterConfiguration
import com.amazonaws.sfc.simulator.config.SimulationConfiguration
import com.amazonaws.sfc.simulator.config.SimulationSourceConfiguration
import com.amazonaws.sfc.system.DateTime.systemDateTime
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.MemoryMonitor.Companion.getUsedMemoryMB
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import kotlin.time.Duration


class SimulationAdapter(private val adapterID: String, private val configuration: SimulationConfiguration, private val logger: Logger) : ProtocolAdapter {

    private val className = this::class.java.simpleName

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val adapterMetricDimensions = mapOf(MetricsCollector.METRICS_DIMENSION_TYPE to className)

    private val sourceConfigurations
        get() = configuration.sources.filter { it.value.protocolAdapterID in configuration.simulationProtocolAdapters.keys }


    private val simulationSources by lazy {
        sequence {
            sourceConfigurations.forEach { (sourceID) ->
                val simulationSource = createSimulationSource(sourceID)
                if (simulationSource != null) yield(sourceID to simulationSource)
            }
        }.toMap()

    }


    private fun createSimulationSource(sourceID: String): SimulationSource? {
        val log = logger.getCtxLoggers(className, "createSimulationSource")
        return try {

            val simulationSourceConfiguration = getSourceConfiguration(sourceID)

            val source = SimulationSource(
                sourceID = sourceID,
                simulationSourceConfiguration = simulationSourceConfiguration,
                metricsCollector = metricsCollector,
                adapterMetricDimensions = adapterMetricDimensions,
                logger = logger
            )
            log.info("Created simulations source for source \"$sourceID\"")
            source
        } catch (e: SimulationException) {
            logger.getCtxErrorLog(className, "createSimulationSource")("Error creating Simulation source for source \"$sourceID\", ${e.message}")
            null
        }
    }


    private fun getSourceConfiguration(sourceID: String): SimulationSourceConfiguration {
        return sourceConfigurations[sourceID]
                ?: throw SimulationException(
                    "\"$sourceID\" is not a valid Simulation source, " +
                            "available Simulation sources are ${sourceConfigurations.keys}"
                )
    }

    private fun protocolAdapterForSource(sourceID: String): SimulationAdapterConfiguration {
        val sourceConfig = getSourceConfiguration(sourceID)
        return configuration.simulationProtocolAdapters[sourceConfig.protocolAdapterID]
                ?: throw SimulationException(
                    "\"${sourceConfig.protocolAdapterID}\" for source \"$sourceID\" is not a valid Simulation protocol adapter, " +
                            "available Simulation protocol adapters are ${configuration.simulationProtocolAdapters.keys}"
                )
    }



    override val metricsCollector: MetricsCollector? by lazy {
        val metricsConfigurations = configuration.simulationProtocolAdapters.map { it.key to (it.value.metrics ?: MetricsSourceConfiguration()) }.toMap()
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
                    val dataPoints = metricsList.map { MetricsDataPoint(it.metricsName, dimensions, it.metricUnit, it.metricsValue) }
                    runBlocking {
                        metricsCollector?.put(adapterID, dataPoints)
                    }
                } catch (e: java.lang.Exception) {
                    logger.getCtxErrorLogEx(this::class.java.simpleName, "collectMetricsFromLogger")("Error collecting metrics from logger", e)
                }
            }
        } else null


    /**
     * Reads a values from a source
     * @param sourceID String Source ID
     * @param channels List<String>? Channels to read values for, if null then all values for the source are read
     * @return SourceReadResult
     */
    override suspend fun read(sourceID: String, channels: List<String>?): SourceReadResult {

        // Retrieve the client to set it up at first call
        val sourceConfiguration =
            sourceConfigurations[sourceID] ?: return SourceReadError("Source \"$sourceID\" does not exist, available sources are ${sourceConfigurations.keys}")
        val protocolAdapterID = sourceConfiguration.protocolAdapterID
        val dimensions = mapOf(METRICS_DIMENSION_SOURCE to "$adapterID:$sourceID") + adapterMetricDimensions

        val simulationSource = simulationSources[sourceID] ?: return SourceReadError("Invalid source configuration")

        val start = systemDateTime().toEpochMilli()

        val sourceReadResult = try {
            val simulationSourceReadData = simulationSource.read(channels) ?: emptyMap()
            val readDurationInMillis = (systemDateTime().toEpochMilli() - start).toDouble()
            createMetrics(protocolAdapterID, dimensions, readDurationInMillis, simulationSourceReadData)
            SourceReadSuccess(simulationSourceReadData, systemDateTime())
        } catch (e: Exception) {
            metricsCollector?.buildValueDataPoint(protocolAdapterID, MetricsCollector.METRICS_READ_ERRORS, 1.0, MetricUnits.COUNT, dimensions)
            SourceReadError(e.toString(), systemDateTime())
        }

        return sourceReadResult
    }

    private fun createMetrics(
        protocolAdapterID: String,
        metricDimensions: MetricDimensions?,
        readDurationInMillis: Double,
        values: Map<String, ChannelReadValue>
    ) {

        val valueCount = if (values.isEmpty()) 0 else values.map { if (it.value.value is ArrayList<*>) (it.value.value as ArrayList<*>).size else 1 }.sum()
        metricsCollector?.put(
            protocolAdapterID, metricsCollector?.buildValueDataPoint(
                protocolAdapterID,
                MetricsCollector.METRICS_MEMORY,
                getUsedMemoryMB().toDouble(),
                MetricUnits.MEGABYTES,
                metricDimensions
            ), metricsCollector?.buildValueDataPoint(
                protocolAdapterID,
                MetricsCollector.METRICS_READS,
                1.0,
                MetricUnits.COUNT, metricDimensions
            ),
            metricsCollector?.buildValueDataPoint(
                protocolAdapterID,
                MetricsCollector.METRICS_READ_DURATION,
                readDurationInMillis,
                MetricUnits.MILLISECONDS,
                metricDimensions
            ), metricsCollector?.buildValueDataPoint(
                protocolAdapterID,
                MetricsCollector.METRICS_VALUES_READ,
                valueCount.toDouble(),
                MetricUnits.COUNT,
                metricDimensions
            ), metricsCollector?.buildValueDataPoint(protocolAdapterID, MetricsCollector.METRICS_READ_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions)
        )
    }

    /**
     * Stops the adapter
     * @param timeout Duration Timeout period to wait for adapter to stop
     */
    override suspend fun stop(timeout: Duration) {
    }

    companion object {


        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParams: Any) =
            newInstance(createParams[0] as ConfigReader, createParams[1] as String, createParams[2] as String, createParams[3] as Logger)


        private val createInstanceMutex = Mutex()


        @JvmStatic
        fun newInstance(configReader: ConfigReader, scheduleName: String, adapterID: String, logger: Logger): SourceValuesReader? {

            runBlocking {
                createInstanceMutex.withLock {
                    if (adapter == null) {
                        adapter = createSimulationAdapter(adapterID, configReader, logger)
                    }
                }
            }

            val config = SimulatorAdapterConfigReader(configReader).getConfig<SimulationConfiguration>()
            val schedule = config.schedules.firstOrNull { it.name == scheduleName }
            val sourcesForAdapter = schedule?.sources?.filter { (config.sources[it.key]?.protocolAdapterID ?: "") == adapterID } ?: return null

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

        fun createSimulationAdapter(adapterID: String, configReader: ConfigReader, logger: Logger): ProtocolAdapter {

            val simulationAdapterConfigReader = SimulatorAdapterConfigReader(configReader)

            val config: SimulationConfiguration = try {
                simulationAdapterConfigReader.getConfig()
            } catch (e: Exception) {
                throw TargetException("Error loading configuration: ${e.message}")
            }
            return SimulationAdapter(adapterID, config, logger)
        }


        private val ADAPTER_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
        )
    }

}
