
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.pccc


import com.amazonaws.sfc.config.BaseConfiguration.Companion.WILD_CARD
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
import com.amazonaws.sfc.pccc.config.PcccAdapterConfiguration
import com.amazonaws.sfc.pccc.config.PcccConfiguration
import com.amazonaws.sfc.pccc.config.PcccControllerConfiguration
import com.amazonaws.sfc.pccc.config.PcccSourceConfiguration
import com.amazonaws.sfc.pccc.config.PcccSourceConfiguration.Companion.CONFIG_ADAPTER_CONTROLLER
import com.amazonaws.sfc.system.DateTime.systemDateTime
import com.amazonaws.sfc.targets.TargetException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration


class PcccAdapter(
    private val adapterID: String,
    private val configuration: PcccConfiguration,
    private val logger: Logger
) : ProtocolAdapter {

    private val className = this::class.java.simpleName

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val adapterMetricDimensions = mapOf(MetricsCollector.METRICS_DIMENSION_TYPE to className)

    private val sourceConfigurations
        get() = configuration.sources.filter { it.value.protocolAdapterID in configuration.pcccProtocolAdapters.keys }


    private val pcccSources by lazy {
        sequence {
            sourceConfigurations.forEach { (sourceID) ->
                val pcccSource = createPcccSource(sourceID)
                if (pcccSource != null) yield(sourceID to pcccSource)
            }
        }.toMap()

    }

    private fun createPcccSource(sourceID: String): PcccSource? {
        return try {
            val (controllerID, controllerConfiguration) = controllerConfigurationForSource(sourceID)
            val pcccSourceConfiguration = getSourceConfiguration(sourceID)
            PcccSource(
                sourceID = sourceID,
                controllerID = controllerID,
                controllerConfiguration = controllerConfiguration,
                sourceConfiguration = pcccSourceConfiguration,
                metricsCollector = metricsCollector,
                adapterMetricDimensions = adapterMetricDimensions,
                logger = logger
            )
        } catch (e: Exception) {
            logger.getCtxErrorLog(
                className,
                "createPcccSource"
            )("Error creating PCCC source instance for source \"$sourceID\", ${e.message}")
            null
        }
    }


    private fun getSourceConfiguration(sourceID: String): PcccSourceConfiguration {
        return sourceConfigurations[sourceID]
            ?: throw PcccAdapterException("\"$sourceID\" is not a valid PCCC source, available PCCC sources are ${sourceConfigurations.keys}")
    }

    private fun protocolAdapterForSource(sourceID: String): PcccAdapterConfiguration {
        val sourceConfig = getSourceConfiguration(sourceID)
        return configuration.pcccProtocolAdapters[sourceConfig.protocolAdapterID]
            ?: throw PcccAdapterException(
                "\"${sourceConfig.protocolAdapterID}\" for source \"$sourceID\" is not a valid PCCC protocol adapter, " +
                        "available PCCC protocol adapters are ${configuration.pcccProtocolAdapters.keys}"
            )
    }

    private fun controllerConfigurationForSource(sourceID: String): Pair<String, PcccControllerConfiguration> {
        val sourceConfig = getSourceConfiguration(sourceID)
        val pcccAdapterConfig = protocolAdapterForSource(sourceID)
        return sourceConfig.adapterControllerID to (pcccAdapterConfig.controllers[sourceConfig.adapterControllerID]
            ?: throw PcccAdapterException("\"${sourceConfig.adapterControllerID}\" is not a valid $CONFIG_ADAPTER_CONTROLLER for adapter \"${sourceConfig.protocolAdapterID}\" used by source \"$sourceID\", valid controller are ${pcccAdapterConfig.controllers.keys}"))
    }

    override val metricsCollector: MetricsCollector? by lazy {
        val metricsConfigurations =
            configuration.pcccProtocolAdapters.map { it.key to (it.value.metrics ?: MetricsSourceConfiguration()) }
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
                    logger.getCtxErrorLog(
                        this::class.java.simpleName,
                        "collectMetricsFromLogger"
                    )("Error collecting metrics from logger, $e")
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


        val sourceConfiguration = sourceConfigurations[sourceID]
            ?: return SourceReadError("Source \"$sourceID\" does not exist, available sources are ${sourceConfigurations.keys}")

        val protocolAdapterID = sourceConfiguration.protocolAdapterID

        val dimensions = mapOf(METRICS_DIMENSION_SOURCE to "$adapterID:$sourceID") + adapterMetricDimensions

        val pcccSource = pcccSources[sourceID] ?: return SourceReadError("Invalid source configuration")

        val start = systemDateTime().toEpochMilli()

        val channelsToRead = if (channels.isNullOrEmpty() || (channels.size == 1 && channels[0] == WILD_CARD)) {
            sourceConfiguration.channels.keys.toList()
        } else {
            channels
        }

        val sourceReadResult = try {
            val pcccSourceReadData = pcccSource.read(channelsToRead)
            val readDurationInMillis = (systemDateTime().toEpochMilli() - start).toDouble()
            createMetrics(protocolAdapterID, dimensions, readDurationInMillis, pcccSourceReadData)
            SourceReadSuccess(pcccSourceReadData, systemDateTime())
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
        readDurationInMillis: Double,
        values: Map<String, ChannelReadValue>
    ) {

        val valueCount =
            if (values.isEmpty()) 0 else values.map { if (it.value.value is ArrayList<*>) (it.value.value as ArrayList<*>).size else 1 }
                .sum()
        metricsCollector?.put(
            protocolAdapterID,
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
                readDurationInMillis,
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

        withTimeoutOrNull(timeout) {
            try {

                pcccSources.forEach {
                    try {
                        it.value.close()
                    } catch (e: Exception) {
                        log.error("Error closing PCCC source for source \"${it.key}")
                    }
                }
            } catch (t: TimeoutCancellationException) {

                log.warning("Timeout stopping PCCC Adapter, $t")
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
                        adapter = createPcccAdapter(adapterID, configReader, logger)
                    }
                }
            }

            val config = configReader.getConfig<PcccConfiguration>()
            val schedule = config.schedules.firstOrNull { it.name == scheduleName }
            val sourcesForAdapter =
                schedule?.sources?.filter { (config.sources[it.key]?.protocolAdapterID ?: "") == adapterID }
                    ?: return null

            runBlocking {
                adapter?.init()
            }

            return if (adapter != null) InProcessSourcesReader.createInProcessSourcesReader(
                schedule,
                adapter!!,
                sourcesForAdapter,
                config.metrics,
                logger
            ) else null

        }

        private var adapter: ProtocolAdapter? = null

        fun createPcccAdapter(adapterID: String, configReader: ConfigReader, logger: Logger): ProtocolAdapter {

            val config: PcccConfiguration = try {
                configReader.getConfig()
            } catch (e: Exception) {
                throw TargetException("Error loading configuration: ${e.message}")
            }
            return PcccAdapter(adapterID, config, logger)
        }


        private val ADAPTER_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
        )
    }

}
