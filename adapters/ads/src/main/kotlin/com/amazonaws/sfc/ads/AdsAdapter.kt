// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.ads


import com.amazonaws.sfc.ads.config.AdsAdapterConfiguration
import com.amazonaws.sfc.ads.config.AdsConfiguration
import com.amazonaws.sfc.ads.config.AdsDeviceConfiguration
import com.amazonaws.sfc.ads.config.AdsSourceConfiguration
import com.amazonaws.sfc.ads.config.AdsSourceConfiguration.Companion.CONFIG_ADAPTER_DEVICE
import com.amazonaws.sfc.ads.protocol.LockableTcpClient
import com.amazonaws.sfc.config.BaseConfiguration.Companion.WILD_CARD
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.TcpConfiguration
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
import com.amazonaws.sfc.system.DateTime.systemDateTime
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.LookupCacheHandler
import com.amazonaws.sfc.util.isJobCancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

// Cache for sharing lockable TCP clients as ADS only supports a singleTCP client connection
// from the same client IP address. If multiple sources read from same device these will share the same
// TCP client connection. The client can be locked to prevent overlapping read/writes for different sources.
typealias TcpClientCache = LookupCacheHandler<String, LockableTcpClient?, TcpConfiguration>

class AdsAdapter(
    private val adapterID: String,
    private val configuration: AdsConfiguration,
    private val logger: Logger
) : ProtocolAdapter {

    private val className = this::class.java.simpleName

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val tcpClientCache = TcpClientCache(
        supplier = { null },
        initializer = { _, _, tcpConfiguration ->
            // create and start a new client which may be shared between clients in ADS sources connecting to the same device
            val client = LockableTcpClient(tcpConfiguration!!, logger)
            client.start()
            client
        },
    )

    private val adapterMetricDimensions = mapOf(MetricsCollector.METRICS_DIMENSION_TYPE to className)

    private val sourceConfigurations
        get() = configuration.sources.filter { it.value.protocolAdapterID in configuration.adsProtocolAdapters.keys }

    private val adsSources by lazy {
        sequence {
            sourceConfigurations.forEach { (sourceID) ->
                val adsSource = createAdsSource(sourceID)
                if (adsSource != null) yield(sourceID to adsSource)
            }
        }.toMap()

    }

    private fun createAdsSource(sourceID: String): AdsSource? {
        return try {
            val (deviceID, deviceConfiguration) = controllerConfigurationForSource(sourceID)
            val adsSourceConfiguration = getSourceConfiguration(sourceID)
            AdsSource(
                tcpClientCache = tcpClientCache, // note the TCP client cache is used, from which the source will use a shared client
                sourceID = sourceID,
                deviceID = deviceID,
                deviceConfiguration = deviceConfiguration,
                sourceConfiguration = adsSourceConfiguration,
                metricsCollector = metricsCollector,
                adapterMetricDimensions = adapterMetricDimensions,
                logger = logger
            )
        } catch (e: Exception) {
            logger.getCtxErrorLog(
                className,
                "createAdsSource"
            )("Error creating ADS source instance for source \"$sourceID\", ${e.message}")
            null
        }
    }


    private fun getSourceConfiguration(sourceID: String): AdsSourceConfiguration {
        return sourceConfigurations[sourceID]
            ?: throw AdsAdapterException("\"$sourceID\" is not a valid ADS source, available ADS sources are ${sourceConfigurations.keys}")
    }

    private fun protocolAdapterForSource(sourceID: String): AdsAdapterConfiguration {
        val sourceConfig = getSourceConfiguration(sourceID)
        return configuration.adsProtocolAdapters[sourceConfig.protocolAdapterID]
            ?: throw AdsAdapterException(
                "\"${sourceConfig.protocolAdapterID}\" for source \"$sourceID\" is not a valid ADS protocol adapter, " +
                        "available ADS protocol adapters are ${configuration.adsProtocolAdapters.keys}"
            )
    }

    private fun controllerConfigurationForSource(sourceID: String): Pair<String, AdsDeviceConfiguration> {
        val sourceConfig = getSourceConfiguration(sourceID)
        val adsAdapterConfig = protocolAdapterForSource(sourceID)
        return sourceConfig.adapterDeviceID to (adsAdapterConfig.devices[sourceConfig.adapterDeviceID]
            ?: throw AdsAdapterException("\"${sourceConfig.adapterDeviceID}\" is not a valid $CONFIG_ADAPTER_DEVICE for adapter \"${sourceConfig.protocolAdapterID}\" used by source \"$sourceID\", valid controller are ${adsAdapterConfig.devices.keys}"))
    }

    override val metricsCollector: MetricsCollector? by lazy {
        val metricsConfigurations =
            configuration.adsProtocolAdapters.map { it.key to (it.value.metrics ?: MetricsSourceConfiguration()) }
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


    override suspend fun read(sourceID: String, channels: List<String>?): SourceReadResult {

        val sourceConfiguration = sourceConfigurations[sourceID]
            ?: return SourceReadError("Source \"$sourceID\" does not exist, available ADS sources are ${sourceConfigurations.keys}")

        val protocolAdapterID = sourceConfiguration.protocolAdapterID

        val dimensions = mapOf(METRICS_DIMENSION_SOURCE to "$adapterID:$sourceID") + adapterMetricDimensions

        val adsSource = adsSources[sourceID] ?: return SourceReadError("Invalid source configuration")

        val start = systemDateTime().toEpochMilli()

        val channelsToRead = if (channels.isNullOrEmpty() || (channels.size == 1 && channels[0] == WILD_CARD)) {
            sourceConfiguration.channels.keys.toList()
        } else {
            channels
        }

        val sourceReadResult = try {
            val adsSourceReadData = adsSource.read(channelsToRead)
            val readDurationInMillis = (systemDateTime().toEpochMilli() - start).toDouble()
            createMetrics(protocolAdapterID, dimensions, readDurationInMillis, adsSourceReadData)
            SourceReadSuccess(adsSourceReadData, systemDateTime())
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

                adsSources.forEach {
                    try {
                        it.value.close()
                    } catch (e: Exception) {
                        log.error("Error closing ADS source for source \"${it.key}")
                    }
                }
            } catch (e : Exception) {
                if (e.isJobCancellationException) {
                    log.warning("ADS Adapter stopped, $e")
                }
                else {
                    log.warning("Timeout stopping ADS Adapter, $e")
                }
            }

        }
        // close all tcp connections in the cache which were used by sources
        tcpClientCache.items.forEach { it?.close(timeout) }
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
                        adapter = createAdsAdapter(adapterID, configReader, logger)
                    }
                }
            }

            val config = configReader.getConfig<AdsConfiguration>()
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

        fun createAdsAdapter(adapterID: String, configReader: ConfigReader, logger: Logger): ProtocolAdapter {

            val config: AdsConfiguration = try {
                configReader.getConfig()
            } catch (e: Exception) {
                throw TargetException("Error loading configuration: ${e.message}")
            }
            return AdsAdapter(adapterID, config, logger)
        }


        private val ADAPTER_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
        )
    }

}
