
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.opcua


import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
import com.amazonaws.sfc.opcua.config.OpcuaConfiguration
import com.amazonaws.sfc.opcua.config.OpcuaConfiguration.Companion.OPC_UA_ADAPTER
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration


/**
 * OPCUA source data adapter
 * @property configuration OpcuaConfiguration Configuration for adapter
 * @property logger Logger
 */
class OpcuaAdapter(private val adapterID: String, private val configuration: OpcuaConfiguration, private val logger: Logger) : ProtocolAdapter {


    private val className = this::class.java.simpleName
    private val adapterMetricDimensions = mapOf(MetricsCollector.METRICS_DIMENSION_TYPE to className)

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val sources = configuration.sources.filter { configuration.protocolAdapters[it.value.protocolAdapterID]?.protocolAdapterType == OPC_UA_ADAPTER }

    private var clientHandleAtomic = AtomicInteger(1)


    override val metricsCollector: MetricsCollector? by lazy {
        val metricsConfigurations = configuration.opcuaProtocolAdapters.map { it.key to (it.value.metrics ?: MetricsSourceConfiguration()) }.toMap()
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
                    val dimensions = mapOf(MetricsCollector.METRICS_DIMENSION_SOURCE to adapterID) + adapterMetricDimensions
                    val dataPoints = metricsList.map { MetricsDataPoint(it.metricsName, dimensions, it.metricUnit, it.metricsValue) }
                    runBlocking {
                        metricsCollector?.put(adapterID, dataPoints)
                    }
                } catch (e: java.lang.Exception) {
                    logger.getCtxErrorLog(this::class.java.simpleName, "collectMetricsFromLogger")("Error collecting metrics from logger, $e")
                }
            }
        } else null


    private val adapterSources = sequence {
        configuration.sources.keys.forEach { sourceID ->
            val source = try {
                OpcuaSource(sourceID, configuration, clientHandleAtomic, logger, metricsCollector, adapterMetricDimensions)
            } catch (_: Exception) {
                null
            }
            if (source != null) {
                yield(sourceID to source)
            }
        }
    }.toMap()


    /**
     * Reads values for a sources/channels
     * @param sourceID String ID of the source to read from
     * @param channels List<String>? IDs of the channels for the source to read
     * @return SourceReadResult
     */
    override suspend fun read(sourceID: String, channels: List<String>?): SourceReadResult {


        val source = adapterSources[sourceID]

        return source?.read(channels)
               ?: SourceReadError("OPCUA source $sourceID does not exist, available $OPC_UA_ADAPTER type sources are ${sources.keys}")
    }


    /**
     * Stops the adapter
     * @param timeout Duration Period to wait for the adapter to stop
     * @return Unit
     */
    override suspend fun stop(timeout: Duration): Unit = coroutineScope {

        withTimeoutOrNull(timeout) {
            adapterSources.values.forEach { source -> source.close() }
        }
    }


    companion object {

        private val newInstanceMutex = Mutex()

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any?): SourceValuesReader? =
            newInstance(createParameters[0] as ConfigReader, createParameters[1] as String, createParameters[2] as String, createParameters[3] as Logger)

        @JvmStatic
        fun newInstance(configReader: ConfigReader, scheduleName: String, adapterID: String, logger: Logger): SourceValuesReader? {

            runBlocking {
                newInstanceMutex.withLock {
                    if (protocolAdapter == null) {
                        protocolAdapter = createOpcuaAdapter(adapterID, configReader, logger)
                    }
                }
            }

            val config = configReader.getConfig<OpcuaConfiguration>()
            val schedule = config.schedules.firstOrNull { it.name == scheduleName }
            val sourcesForAdapter = schedule?.sources?.filter { (config.sources[it.key]?.protocolAdapterID ?: "") == adapterID } ?: return null

            return InProcessSourcesReader.createInProcessSourcesReader(schedule, protocolAdapter!!, sourcesForAdapter, config.metrics, logger)
        }

        private var protocolAdapter: ProtocolAdapter? = null

        /**
         * Creates an OPCUA adapter from its configuration
         * @param configReader ConfigReader Reader for configuration
         * @param logger Logger Logger for output
         * @return ProtocolAdapter
         * @throws Exception
         */
        fun createOpcuaAdapter(adapterID: String, configReader: ConfigReader, logger: Logger): ProtocolAdapter {

            // obtain opcua configuration
            val config: OpcuaConfiguration = try {
                configReader.getConfig()
            } catch (e: Exception) {
                throw ProtocolAdapterException("Error loading configuration: ${e.message}")
            }
            return OpcuaAdapter(adapterID, config, logger)
        }


        const val OPC_UA_NAMESPACE = 0

        private val ADAPTER_METRIC_DIMENSIONS = mapOf(
            METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER)

    }

}


