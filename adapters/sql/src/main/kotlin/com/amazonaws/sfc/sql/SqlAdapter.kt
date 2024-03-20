// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.sql


import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
import com.amazonaws.sfc.sql.config.DbServerConfiguration
import com.amazonaws.sfc.sql.config.SqlAdapterConfiguration
import com.amazonaws.sfc.sql.config.SqlConfiguration
import com.amazonaws.sfc.sql.config.SqlConfiguration.Companion.SQL_ADAPTER
import com.amazonaws.sfc.sql.config.SqlSourceConfiguration
import com.amazonaws.sfc.sql.config.SqlSourceConfiguration.Companion.CONFIG_ADAPTER_DB_SERVER
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.system.DateTime.systemDateTime
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.InstanceFactory
import com.amazonaws.sfc.util.MemoryMonitor.Companion.getUsedMemoryMB
import com.amazonaws.sfc.util.buildScope
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.URLClassLoader
import java.sql.Connection
import java.sql.DriverManager
import kotlin.time.Duration


class SqlAdapter(private val adapterID: String, private val configuration: SqlConfiguration, private val logger: Logger) : ProtocolAdapter {

    private val className = this::class.java.simpleName

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val adapterMetricDimensions = mapOf(MetricsCollector.METRICS_DIMENSION_TYPE to className)

    private val sourceConfigurations
        get() = configuration.sources.filter { it.value.protocolAdapterID in configuration.sqlProtocolAdapters.keys }


    val sqlSources by lazy {
        sequence {
            sourceConfigurations.forEach { (sourceID) ->
                val sqlSource = createSqlSource(sourceID)
                if (sqlSource != null) yield(sourceID to sqlSource)
            }
        }.toMap()

    }

    override suspend fun init() {
        loadJdbcDrivers()
        runServerInitScrips()
    }


    private fun loadJdbcDrivers() {

        // When running in in-process mode the adapter needs to explicitly load the used database drivers, as
        // the jdbc loader required them to be in the classpath
        val log = logger.getCtxLoggers(className, "loadDrivers")

        val jars = configuration.protocolAdapterTypes[SQL_ADAPTER]?.jarFiles ?: emptyList()
        if (jars.isEmpty()) return

        // Get jar files configured for the SQL adapter, this is the place where drivers are expected by SQL adapter.
        log.trace("Loading database drivers from $jars")

        val expandedJars = InstanceFactory.expandedJarList(jars)
        val classLoader = URLClassLoader(expandedJars.map { it.toURI().toURL() }.toTypedArray(), this::class.java.classLoader)

        // Used database types
        val databaseTypes = configuration.sqlProtocolAdapters
            .flatMap {
                it.value.dbServers.values
                    .map { d -> d.dbServerType }
            }
            .toSet()

        databaseTypes.forEach {
            if (it != null) {
                try {
                    log.info("Loading driver ${it.driverClassName} for database type $it")
                    Class.forName(it.driverClassName, true, classLoader)
                } catch (e: Exception) {
                    log.errorEx("Error loading driver ${it.driverClassName} for database type $it", e)
                }
            }
        }

    }


    private suspend fun runServerInitScrips() {

        val scope = buildScope("runServerInitScrips", Dispatchers.IO)

        val log = logger.getCtxLoggers(className, "runServerInitScrips")

        usedDbServers().map { (dbServerID, dbServerConfig) ->

            scope.launch {
                val script = dbServerConfig.initSql ?: dbServerConfig.initScript?.readText()
                if (script != null) {
                    var connection: Connection? = null
                    try {
                        log.info("Connecting to server $dbServerID, ${dbServerConfig.databaseStr} for running initialization")

                        connection = connect(dbServerConfig)

                        log.info("Running initialization script for server \"$dbServerID\"")
                        val statement = connection.createStatement()
                        statement.execute(script)
                        statement.close()
                        connection.close()

                    } catch (e: Exception) {
                        log.errorEx("Error running initialization script \"${dbServerConfig.initScript}\" for server \"$dbServerID\"", e)
                    } finally {
                        connection?.close()
                    }
                }

            }.join()
        }
    }


    fun usedDbServers() = sqlSources.keys.flatMap { sourceID ->
        protocolAdapterForSource(sourceID).dbServers.entries
    }.toSet()


    private fun createSqlSource(sourceID: String): SqlSource? {
        return try {
            val (dbServerID, dbServerConfiguration) = dbServerConfigurationForSource(sourceID)
            val sqlSourceConfiguration = getSourceConfiguration(sourceID)
            SqlSource(
                sourceID = sourceID,
                dbServerID = dbServerID,
                dbServerConfiguration = dbServerConfiguration,
                sqlSourceConfiguration = sqlSourceConfiguration,
                metricsCollector = metricsCollector,
                adapterMetricDimensions = adapterMetricDimensions,
                logger = logger
            )
        } catch (e: SqlAdapterException) {
            logger.getCtxErrorLog(className, "createSqlSource")("Error creating sql source for source \"$sourceID\", ${e.message}")
            null
        }
    }


    private fun getSourceConfiguration(sourceID: String): SqlSourceConfiguration {
        return sourceConfigurations[sourceID]
            ?: throw SqlAdapterException(
                "\"$sourceID\" is not a valid sql source, " +
                        "available sql sources are ${sourceConfigurations.keys}"
            )
    }

    private fun protocolAdapterForSource(sourceID: String): SqlAdapterConfiguration {
        val sourceConfig = getSourceConfiguration(sourceID)
        return configuration.sqlProtocolAdapters[sourceConfig.protocolAdapterID]
            ?: throw SqlAdapterException(
                "\"${sourceConfig.protocolAdapterID}\" for source \"$sourceID\" is not a valid sql protocol adapter, " +
                        "available sql protocol adapters are ${configuration.sqlProtocolAdapters.keys}"
            )
    }

    private fun dbServerConfigurationForSource(sourceID: String): Pair<String, DbServerConfiguration> {
        val sourceConfig = getSourceConfiguration(sourceID)
        val sqlAdapter = protocolAdapterForSource(sourceID)
        return sourceConfig.adapterDbServerID to (sqlAdapter.dbServers[sourceConfig.adapterDbServerID]
            ?: throw SqlAdapterException("\"${sourceConfig.adapterDbServerID}\" is not a valid $CONFIG_ADAPTER_DB_SERVER for adapter \"${sourceConfig.protocolAdapterID}\" used by source \"$sourceID\", valid servers are ${sqlAdapter.dbServers.keys}"))
    }

    override val metricsCollector: MetricsCollector? by lazy {
        val metricsConfigurations = configuration.sqlProtocolAdapters.map { it.key to (it.value.metrics ?: MetricsSourceConfiguration()) }.toMap()
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


    private val scope = buildScope("SQL Protocol Handler")


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

        sqlSources
        val sqlSource = sqlSources[sourceID] ?: return SourceReadError("Invalid source configuration")

        val start = systemDateTime().toEpochMilli()

        val sourceReadResult = try {
            val sqlSourceReadData = sqlSource.read(channels) ?: emptyMap()
            val readDurationInMillis = (systemDateTime().toEpochMilli() - start).toDouble()
            createMetrics(protocolAdapterID, dimensions, readDurationInMillis, sqlSourceReadData)
            SourceReadSuccess(sqlSourceReadData, systemDateTime())
        } catch (e: Exception) {
            metricsCollector?.buildValueDataPoint(protocolAdapterID, MetricsCollector.METRICS_READ_ERRORS, 1.0, MetricUnits.COUNT, dimensions)
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

        val valueCount = if (values.size == 0) 0 else values.map { if (it.value.value is ArrayList<*>) (it.value.value as ArrayList<*>).size else 1 }.sum()
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

                sqlSources.forEach {
                    try {
                        it.value.close()
                    } catch (e: Exception) {
                        log.error("Error closing SQL source for source \"${it.key}")
                    }
                }
            } catch (t: TimeoutCancellationException) {
                log.warning("Timeout stopping SQL Adapter, $t")
            }

        }
    }


    companion object {

        private val className = this::class.java.simpleName

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParams: Any) =
            newInstance(createParams[0] as ConfigReader, createParams[1] as String, createParams[2] as String, createParams[3] as Logger)


        private val createInstanceMutex = Mutex()

        suspend fun connect(dbServerConfiguration: DbServerConfiguration): Connection {

            return withTimeout(dbServerConfiguration.connectTimeout) {


                val connection =
                    DriverManager.getConnection(dbServerConfiguration.jdbcConnectString, dbServerConfiguration.userName, dbServerConfiguration.password)
                connection
            }
        }


        @JvmStatic
        fun newInstance(configReader: ConfigReader, scheduleName: String, adapterID: String, logger: Logger): SourceValuesReader? {

            runBlocking {
                createInstanceMutex.withLock {
                    if (adapter == null) {
                        adapter = createSqlAdapter(adapterID, configReader, logger)
                    }
                }
            }

            val config = configReader.getConfig<SqlConfiguration>()
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

        fun createSqlAdapter(adapterID: String, configReader: ConfigReader, logger: Logger): ProtocolAdapter {

            val config: SqlConfiguration = try {
                configReader.getConfig()
            } catch (e: Exception) {
                throw TargetException("Error loading configuration: ${e.message}")
            }
            return SqlAdapter(adapterID, config, logger)
        }


        private val ADAPTER_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
        )
    }

}
