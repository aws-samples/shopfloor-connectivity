// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.rest


import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
import com.amazonaws.sfc.rest.config.RestAdapterConfiguration
import com.amazonaws.sfc.rest.config.RestConfiguration
import com.amazonaws.sfc.rest.config.RestServerConfiguration
import com.amazonaws.sfc.rest.config.RestServerConfiguration.Companion.CONFIG_SERVER
import com.amazonaws.sfc.rest.config.RestSourceConfiguration
import com.amazonaws.sfc.system.DateTime.systemDateTime
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.MemoryMonitor.Companion.getUsedMemoryMB
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration


class RestAdapter(private val adapterID: String, private val configuration: RestConfiguration, private val logger: Logger) : ProtocolAdapter {

    private val className = this::class.java.simpleName

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val adapterMetricDimensions = mapOf(MetricsCollector.METRICS_DIMENSION_TYPE to className)

    private val sourceConfigurations
        get() = configuration.sources.filter { it.value.protocolAdapterID in configuration.restProtocolAdapters.keys }


    private val restSources by lazy {
        sequence {
            sourceConfigurations.forEach { (sourceID) ->
                val restSource = createRestSource(sourceID)
                if (restSource != null) yield(sourceID to restSource)
            }
        }.toMap()

    }


    private fun createRestSource(sourceID: String): RestSource? {
        val log = logger.getCtxLoggers(className, "createRestSource")
        return try {
            val (serverID, restServerConfiguration) = serverConfigurationForSource(sourceID)
            val restSourceConfiguration = getSourceConfiguration(sourceID)

            val source = RestSource(
                sourceID = sourceID,
                restServerConfiguration = restServerConfiguration,
                restSourceConfiguration = restSourceConfiguration,
                metricsCollector = metricsCollector,
                adapterMetricDimensions = adapterMetricDimensions,
                logger = logger
            )
            log.info("Created REST source for source \"$sourceID\" reading from server \"$serverID\" at ${restServerConfiguration.server}")
            source
        } catch (e: RestAdapterException) {
            logger.getCtxErrorLog(className, "createRestSource")("Error creating REST source for source \"$sourceID\", ${e.message}")
            null
        }
    }


    private fun getSourceConfiguration(sourceID: String): RestSourceConfiguration {
        return sourceConfigurations[sourceID]
                ?: throw RestAdapterException(
                    "\"$sourceID\" is not a valid REST source, " +
                            "available REST sources are ${sourceConfigurations.keys}"
                )
    }

    private fun protocolAdapterForSource(sourceID: String): RestAdapterConfiguration {
        val sourceConfig = getSourceConfiguration(sourceID)
        return configuration.restProtocolAdapters[sourceConfig.protocolAdapterID]
                ?: throw RestAdapterException(
                    "\"${sourceConfig.protocolAdapterID}\" for source \"$sourceID\" is not a valid REST protocol adapter, " +
                            "available REST protocol adapters are ${configuration.restProtocolAdapters.keys}"
                )
    }

    private fun serverConfigurationForSource(sourceID: String): Pair<String, RestServerConfiguration> {
        val sourceConfig = getSourceConfiguration(sourceID)
        val restAdapter = protocolAdapterForSource(sourceID)
        return sourceConfig.adapterServerID to (restAdapter.servers[sourceConfig.adapterServerID]
                ?: throw RestAdapterException("\"${sourceConfig.adapterServerID}\" is not a valid $CONFIG_SERVER for adapter \"${sourceConfig.protocolAdapterID}\" used by source \"$sourceID\", valid servers are ${restAdapter.servers.keys}"))
    }

    override val metricsCollector: MetricsCollector? by lazy {
        val metricsConfigurations = configuration.restProtocolAdapters.map { it.key to (it.value.metrics ?: MetricsSourceConfiguration()) }.toMap()
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

        restSources
        val restSource = restSources[sourceID] ?: return SourceReadError("Invalid source configuration")

        val start = systemDateTime().toEpochMilli()

        val sourceReadResult = try {
            val restSourceReadData = restSource.read(channels) ?: emptyMap()
            val readDurationInMillis = (systemDateTime().toEpochMilli() - start).toDouble()
            createMetrics(protocolAdapterID, dimensions, readDurationInMillis, restSourceReadData)
            SourceReadSuccess(restSourceReadData, systemDateTime())
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

        val log = logger.getCtxLoggers(className, "stop")

        withTimeoutOrNull(timeout) {
            try {

                restSources.forEach {
                    try {
                        it.value.close()
                    } catch (e: Exception) {
                        log.error("Error closing REST source for source \"${it.key}")
                    }
                }
            } catch (t: TimeoutCancellationException) {
                log.warning("Timeout stopping REST Adapter, $t")
            }

        }
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
                        adapter = createRestAdapter(adapterID, configReader, logger)
                    }
                }
            }

            val config = configReader.getConfig<RestConfiguration>()
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

        fun createRestAdapter(adapterID: String, configReader: ConfigReader, logger: Logger): ProtocolAdapter {

            val config: RestConfiguration = try {
                configReader.getConfig()
            } catch (e: Exception) {
                throw TargetException("Error loading configuration: ${e.message}")
            }
            return RestAdapter(adapterID, config, logger)
        }


        private val ADAPTER_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
        )
    }

}
