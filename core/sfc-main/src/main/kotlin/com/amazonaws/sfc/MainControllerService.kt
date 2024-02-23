
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc


import com.amazonaws.sfc.MainControllerService.Companion.createController
import com.amazonaws.sfc.SourceReaderFactory.Companion.createSourceReaderFactory
import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.ConfigReader.Companion.createConfigReader
import com.amazonaws.sfc.data.SourceValuesReader
import com.amazonaws.sfc.data.TargetWriter
import com.amazonaws.sfc.ipc.IpcMetricsWriter
import com.amazonaws.sfc.ipc.IpcSourceReader.Companion.createIpcReader
import com.amazonaws.sfc.ipc.IpcTargetWriter
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.secrets.SecretsManager.Companion.createSecretsManager
import com.amazonaws.sfc.service.HealthProbeService
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.targets.TargetWriterFactory
import com.amazonaws.sfc.util.buildScope
import kotlinx.coroutines.*
import kotlin.system.exitProcess
import kotlin.time.DurationUnit
import kotlin.time.toDuration


/**
 * Main SFC controller. Creates and executes processors for the configured schedules
 * @property configReader ConfigReader Reader for configuration
 * @property configuration ControllerServiceConfig Configuration for SFC controller
 * @see ControllerServiceConfiguration
 * @see createController
 */
class MainControllerService(private val configReader: ConfigReader,
                            private val configuration: ControllerServiceConfiguration,
                            private val logger: Logger) : Service {

    private val className = this::class.java.simpleName

    private val controllerScope = buildScope(className)

    // map contains a schedule sub-controller for each active schedule from the configuration
    private var scheduleControllers: Map<String, ScheduleController?>? = null

    private val adapterMetricProviders = mutableMapOf<String, MetricsProvider>()
    private val targetMetricProviders = mutableMapOf<String, MetricsProvider>()

    private val startTime = DateTime.systemDateTime()


    private val coreMetricsCollector =
        if (configuration.metrics?.isCoreCollectingMetrics == true) {

            logger.metricsCollectorMethod = collectMetricsFromLogger

            MetricsCollector(configuration.metrics, SFC_CORE, configuration.metrics!!, CORE_METRIC_DIMENSIONS, MetricsSourceType.SFC_CORE, logger)
        } else {
            null
        }

    private val collectMetricsFromLogger: MetricsCollectorMethod?
        get() =
            if (configuration.metrics?.isCollectingMetrics == true) {
                { metricsList ->
                    try {
                        val dataPoints = metricsList.map { MetricsDataPoint(name = it.metricsName, units = it.metricUnit, value = it.metricsValue) }
                        runBlocking {
                            coreMetricsCollector?.put(SFC_CORE, dataPoints)
                        }
                    } catch (e: java.lang.Exception) {
                        logger.getCtxErrorLog(this::class.java.simpleName, "collectMetricsFromLogger")("Error collecting metrics from logger, $e")
                    }
                }
            } else null


    private val coreMetricsProvider = if (coreMetricsCollector != null) {
        InProcessMetricsProvider(coreMetricsCollector, logger)
    } else {
        null
    }

    // Factory to create instances of target writers
    private val targetWriterFactory: TargetWriterFactory? by lazy {
        TargetWriterFactory.createTargetWriterFactory(configReader, logger)
    }

    // Factory to create instances of source readers
    private val sourceReaderFactory: SourceReaderFactory? by lazy {
        createSourceReaderFactory(configReader, logger)
    }

    private var metricsProcessor: MetricsProcessor? = null

    /**
     * Stops the controller
     */
    override suspend fun stop() = coroutineScope {
        try {

            withTimeout(CLOSING_TIMEOUT) {
                listOf(
                    stopMetricsProcessing(),
                    stopScheduleControllers(),
                ).joinAll()
            }
        } catch (_: TimeoutCancellationException) {
            logger.getCtxWarningLog(className, "stop")("Timeout stopping service")
        }

        scheduleControllers = null
    }

    private suspend fun stopScheduleControllers() = coroutineScope {
        launch {
            scheduleControllers?.map {
                launch { it.value?.close() }
            }?.forEach {
                it.join()
            }
            scheduleControllers = null
        }
    }

    private suspend fun stopMetricsProcessing() = coroutineScope {
        launch(Dispatchers.IO) {
            listOf(launch {
                adapterMetricProviders.values.map {
                    launch { it.close() }
                }.forEach { it.join() }
                adapterMetricProviders
            },
                launch {
                    targetMetricProviders.values.map {
                        launch {
                            it.close()
                        }
                    }.joinAll()
                    targetMetricProviders.clear()
                },
                launch {
                    metricsProcessor?.close()
                    metricsProcessor = null
                }
            ).joinAll()
        }
    }

    /**
     * Start the controller
     */
    override suspend fun start() {

        // list of scheduler to reader pairs
        val schedules: Map<String, Pair<Map<String, SourceValuesReader>, Map<String, TargetWriter>>> =
            // for each configured service
            configuration.schedules.filter { it.active }.mapNotNull { schedule ->
                // create a reader to read input from protocol handler
                val readers = createReadersForSchedule(schedule, configuration)
                val writers = createTargetWritersForSchedule(schedule, configuration)
                if (readers.isNotEmpty() && writers.isNotEmpty()) schedule.name to Pair(readers, writers) else null
            }.toMap()


        // create a sub-controller for each active scheduler
        scheduleControllers = schedules.map { (schedule, readersWriters) ->

            schedule to ScheduleController.createScheduleController(
                configReader = configReader,
                config = configuration,
                scheduleName = schedule,
                sourceReaders = readersWriters.first,
                targetWriters = readersWriters.second,
                metricsCollector = coreMetricsCollector,
                logger = logger
            )
        }.toMap()

        startMetricsProcessor()
        initializeHealthProbeService()


    }

    private fun startMetricsProcessor() {
        val infoLog = logger.getCtxInfoLog(className, "startMetricsProcessor")
        val anyMetricCollectors = adapterMetricProviders.isNotEmpty() || targetMetricProviders.isNotEmpty() || coreMetricsProvider != null
        if (configuration.metrics != null && anyMetricCollectors) {

            metricsProcessor = MetricsProcessor(configReader = configReader,
                logger = logger,
                metricProviders = adapterMetricProviders +
                                  targetMetricProviders +
                                  if (coreMetricsProvider != null) mapOf(SFC_CORE to coreMetricsProvider) else emptyMap()) { metricsConfiguration ->
                createMetricsWriter(metricsConfiguration)
            }

            metricsProcessor?.start()
            if (metricsProcessor != null) {
                infoLog("Metrics processor started for adapters ${adapterMetricProviders.keys} " +
                        "and targets ${targetMetricProviders.keys}" +
                        if (coreMetricsProvider != null) " and $SFC_CORE" else "")
            }
        } else {
            metricsProcessor = null
            infoLog("No adapter or target metrics are collected")
        }
    }


    private fun createMetricsWriter(metricsConfiguration: MetricsConfiguration): MetricsWriter? {
        if (metricsConfiguration.writer?.metricsServer != null) {
            return IpcMetricsWriter.createIpcMetricsWriter(configReader, logger)
        }

        val metricsWriterFactory = MetricsWriterFactory.createMetricsWriterFactory(configReader, logger)
        return metricsWriterFactory?.createInProcessWriter(configReader)
    }

    // Creates a protocol reader for a schedule.
    private fun createReadersForSchedule(
        schedule: ScheduleConfiguration,
        configuration: ControllerServiceConfiguration
    ): Map<String, SourceValuesReader> {

        val log = logger.getCtxLoggers(className, "createReadersForSchedule")

        val readers = mutableMapOf<String, SourceValuesReader>()

        schedule.sources.keys.forEach { sourceID ->

            val source: SourceConfiguration? = configuration.sources[sourceID]

            when {
                (source == null) -> {
                    log.error("Source \"$sourceID\" does not exists, valid sources are ${configuration.sources.keys}")
                }

                (source.protocolAdapterID !in configuration.protocolAdapters) -> {
                    log.error("Protocol \"${source.protocolAdapterID}\" for source \"$sourceID\" does not exists, valid sources are ${configuration.sources.keys}")
                }

                else -> {

                    if (source.protocolAdapterID !in readers.keys) {
                        val reader = buildSourceValuesReader(schedule, sourceID, source.protocolAdapterID)
                        if (reader != null) {
                            readers[source.protocolAdapterID] = reader
                            initializeMetricsForSourceAdapter(sourceID, reader)
                        }
                    }
                }
            }
        }

        return readers
    }

    // Creates a single configured target writer
    private suspend fun createTargetWritersForSchedule(schedule: ScheduleConfiguration, config: ControllerServiceConfiguration): Map<String, TargetWriter> {

        val log = logger.getCtxLoggers(className, "createTargetWritersForSchedule")

        return schedule.targets.filter { t -> config.targets[t]?.active == true }.map { targetID ->

            val targetConfiguration: TargetConfiguration? = configuration.targets[targetID]

            val server = targetConfiguration?.server

            val targetWriter = withTimeoutOrNull(60.toDuration(DurationUnit.SECONDS)) {
                when {
                    // target does not exist
                    (targetConfiguration == null) -> {
                        log.error("Target \"$targetID\" in schedule \"${schedule.name} does not exist, existing targets are ${config.targets.keys}}")
                        null
                    }

                    (!targetConfiguration.active) -> {
                        log.info("Target \"$targetID\" is not active")
                        null
                    }
                    // create writer using client to communicate with external target IPC service
                    (!server.isNullOrEmpty()) -> {
                        log.info("Creating an IPC process writer for target \"$targetID\", for server \"$server\" on server $server")
                        IpcTargetWriter.createIpcTargetWriter(configReader, targetID, server, logger, null)
                    }

                    else -> {
                        // create in process target writer
                        log.info("Creating in process target writer for target ID $targetID")
                        targetWriterFactory?.createInProcessWriter(targetID, logger, null)
                    }
                }
            }

            if (targetWriter != null) {
                initializeMetricsForTarget(targetID, targetWriter)
            }

            targetID to targetWriter

        }.mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
    }


    private fun initializeMetricsForSourceAdapter(sourceID: String,
                                                  reader: SourceValuesReader) {
        val source = configuration.sources[sourceID] ?: return
        val adapter = configuration.protocolAdapters[source.protocolAdapterID]
        val isCollectingMetricsFromAdapter = (configuration.metrics != null) && (adapter?.metrics?.enabled != false)
        if (isCollectingMetricsFromAdapter && reader.metricsProvider != null) {
            // only need a provider per adapter (adapters read data from multiple sources)
            adapterMetricProviders.computeIfAbsent(source.protocolAdapterID) { reader.metricsProvider!! }
        }
    }


    private fun initializeMetricsForTarget(targetID: String,
                                           target: TargetWriter) {
        val targetConfig = configuration.targets[targetID] ?: return
        //   val adapter = configuration.protocolAdapters[source.protocolAdapterID]
        val isCollectingMetricsFromTarget = (configuration.metrics != null) && targetConfig.metrics.enabled
        if (isCollectingMetricsFromTarget && target.metricsProvider != null) {
            targetMetricProviders[targetID] = target.metricsProvider!!
        }
    }

    private fun buildSourceValuesReader(schedule: ScheduleConfiguration, sourceID: String, adapterID: String): SourceValuesReader? {

        val protocolConfiguration = configuration.protocolAdapters[adapterID]
        val protocolServerConfiguration = configuration.protocolAdapterServers[protocolConfiguration?.protocolAdapterServer]

        return if (protocolServerConfiguration != null) {
            createIpcReader(configReader, configuration, adapterID, schedule, logger) as SourceValuesReader
        } else {
            if (protocolConfiguration?.protocolAdapterType !in configuration.protocolAdapterTypes) {
                val log = logger.getCtxLoggers(className, "buildSourceValuesReader")
                log.error("Protocol type ${protocolConfiguration?.protocolAdapterType} for protocol \"$adapterID\" used in source \"Source \"$sourceID\" does not exist, " +
                          "valid types are ${configuration.protocolAdapterTypes.keys}")
            }
            createInProcessReader(
                configuration.protocolAdapterTypes[protocolConfiguration?.protocolAdapterType]!!,
                schedule,
                adapterID
            )
        }
    }


    // create in process reader instance
    private fun createInProcessReader(conf: InProcessConfiguration,
                                      schedule: ScheduleConfiguration,
                                      adapterID: String): SourceValuesReader? {

        val log = logger.getCtxLoggers(className, "createInProcessReader")

        if (conf.jarFiles.isNullOrEmpty()) {
            log.error("No jar files configured to load in-process reader from")
            return null

        }

        // user factory to create an instance of the reader by dynamically loading the jar files for the configured reader
        log.info("Creating an in-process reader for adapter \"$adapterID\" of protocol adapter type ${configuration.protocolAdapters[adapterID]?.protocolAdapterType}")
        log.trace("jar files for reader are ${conf.jarFiles!!.joinToString()}")
        val reader = try {
            sourceReaderFactory?.createInProcessReader(schedule.name, adapterID, logger)
        } catch (e: Throwable) {
            log.error("Error creating instance of \"${conf.factoryClassName}\" from jar ${conf.jarFiles!!.joinToString()} : ${e::class.java.simpleName}, ${e.message ?: ""}")
            null
        }
        return reader
    }

    private fun isHealthy(): Boolean {
        // give server 10 seconds to start
        return if (DateTime.systemDateTime() < startTime.plusSeconds(10)) true
        else {
            try {
                (scheduleControllers?.values?.filterNotNull()?.all { it.isRunning } ?: false) &&
                (metricsProcessor?.isRunning ?: true)
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun stopUnhealthyService() {
        runBlocking {
            logger.getCtxWarningLog(className, "stopUnhealthyService")("Service will be stopped by health probe service")
            stop()
            exitProcess(1)
        }
    }

    private suspend fun initializeHealthProbeService() {

            healthProbeService?.stop()

            val healthProbeConfiguration = configuration.healthProbeConfiguration
            healthProbeService = if (healthProbeConfiguration == null) null else
                try {
                    val service =
                        HealthProbeService(healthProbeConfiguration, checkFunction = ::isHealthy, serviceStopFunction = ::stopUnhealthyService, logger = logger)
                    controllerScope.launch {
                        delay(1.toDuration(DurationUnit.MINUTES))
                        service.restartIfInactive()
                    }
                    service
                } catch (e: Exception) {
                    logger.getCtxErrorLog(className, "initializeHealthProbeService")("Error initializing health probe service : ${e.message ?: ""}")
                    null
                }
            healthProbeService?.start()

    }


    /**
     * blocks until the controller is shut down
     */
    override suspend fun blockUntilShutdown() {
        scheduleControllers?.forEach {
            it.value?.blockUntilStopped()
        }
    }

    companion object {
        /**
         * Creates an instance of the SFC main Controller from its command line arguments
         * @param args Array<String> Command line arguments
         * @param configuration String Configuration data
         * @param logger Logger Logger for output
         * @return MainController Created SFC main controller instance
         * @see ControllerServiceConfiguration
         * @throws Exception
         */

        fun createController(args: Array<String>,
                             configuration: String,
                             logger: Logger): MainControllerService {

            val cmd = ControllerCommandLineOptions(args)

            var configReader = createConfigReader(configuration, allowUnresolved = true, secretsManager = null)
            var controllerConfiguration: ControllerServiceConfiguration = configReader.getConfig()

            val logLevel: LogLevel = cmd.logLevel ?: controllerConfiguration.logLevel
            logger.level = logLevel


            val secretsManager = createSecretsManager(controllerConfiguration, logger)
            runBlocking {
                secretsManager?.syncSecretsFromService(controllerConfiguration.secretsManagerConfiguration?.cloudSecrets ?: emptyList())
            }
            configReader = createConfigReader(configuration, allowUnresolved = false, secretsManager)
            controllerConfiguration = configReader.getConfig()

            return MainControllerService(configReader, controllerConfiguration, logger)
        }

        const val CLOSING_TIMEOUT = 10000L
        const val SFC_CORE = "SfcCore"

        private var healthProbeService: HealthProbeService? = null

        val CORE_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE to SFC_CORE,
            MetricsCollector.METRICS_DIMENSION_TYPE to SFC_CORE,
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY_CORE)


    }


}