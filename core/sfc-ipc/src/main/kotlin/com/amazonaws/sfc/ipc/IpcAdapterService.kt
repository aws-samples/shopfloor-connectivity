
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.ipc


import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.SecretsManagerConfiguration.Companion.CONFIG_CLOUD_SECRETS
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.ipc.ProtocolServerCommandLine.Companion.OPTION_PROTOCOL_ADAPTER
import com.amazonaws.sfc.ipc.extensions.GrpcSourceValueFromNativeExt.asReadValuesReply
import com.amazonaws.sfc.ipc.extensions.grpcMetricsDataMessage
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricsAsFlow
import com.amazonaws.sfc.secrets.SecretsManager
import com.amazonaws.sfc.service.HealthProbeService
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.isJobCancellationException
import io.grpc.BindableService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess
import kotlin.time.DurationUnit
import kotlin.time.toDuration

typealias CreateProtocolAdapterMethod = (String, ConfigReader, Logger) -> ProtocolAdapter

/**
 * Service for GRPC IPC protocol adapter server
 * @property protocolAdapter ProtocolAdapter Protocol adapter used by the service
 * @property logger Logger Logger for output
 */
class IpcAdapterService(
    private val adapterID: String?,
    serverConfig: ServerConfiguration,
    private val tuningConfiguration: TuningConfiguration,
    logger: Logger,
    private val createAdapter: CreateProtocolAdapterMethod) : IpcBaseService(serverConfig, logger), Service {

    override val serviceImplementation: BindableService
        get() = ProtocolService()

    private val className = this::class.java.simpleName
    private val serverScope = buildScope(className)

    private var _initialized = false
    val initialized
        get() = _initialized

    var useCompressionForData: Boolean = false

    private val startTime = DateTime.systemDateTime()

    private var healthProbeService: HealthProbeService? = null

    private var _protocolAdapter: ProtocolAdapter? = null
    private val protocolAdapter: ProtocolAdapter
        get() {
            if (_protocolAdapter == null) {
                throw ProtocolAdapterException("Protocol adapter has not been initialized")
            }
            return _protocolAdapter!!
        }


    /**
     * Starts the server
     */
    override suspend fun start() {

        val log = logger.getCtxLoggers(className, "start")

        grpcServer.start()
        val addressAndPort = grpcServer.listenSockets.first() as InetSocketAddress
        val addressAndPortStr = "${addressAndPort.address.hostAddress}:${addressAndPort.port}"
        log.info("IPC protocol service started, listening on $addressAndPortStr, connection type is ${serverConfig.serverConnectionType}")

        Runtime.getRuntime().addShutdownHook(
            Thread {
                log.info("Shutting down IPC protocol server")
                runBlocking {
                    stop()
                }
            })
    }

    /**
     * Stops the server
     */
    override suspend fun stop() {
        _initialized = false
        grpcServer.shutdownNow().awaitTermination(15, TimeUnit.SECONDS)
    }

    /**
     * Blocks until the server has shutdown
     */
    override suspend fun blockUntilShutdown() {
        // Blocks until server is stopped
        withContext(Dispatchers.IO) {
            grpcServer.awaitTermination()
        }
    }

    /**
     * Inner class that implements the GRPC service
     */
    private inner class ProtocolService : ProtocolAdapterServiceGrpcKt.ProtocolAdapterServiceCoroutineImplBase() {


        // Implements GRPC ReadValues streaming service
        override fun readValues(request: ReadValuesRequest): Flow<ReadValuesReply> {

            val log = logger.getCtxLoggers(className, "readValues")

            // Interval between reading sources
            val interval = request.interval.toDuration(DurationUnit.MILLISECONDS)

            // Mapping of sources and channels to read
            val sources = request.sourcesList.associate { source ->
                source.sourceID to source.channelsList
            }

            return flow {
                // Create a reader to feed the channel flow
                SourcesValuesAsFlow(protocolAdapter, sources, interval, logger).use { reader ->
                    // Loop reading from reader read results channel
                    reader.sourceReadResults(currentCoroutineContext(), maxConcurrentSourceReads = tuningConfiguration.maxConcurrentSourceReaders, timeout =  tuningConfiguration.allSourcesReadTimeout).buffer(100).cancellable().collect {

                        if (logger.level == LogLevel.TRACE) {
                            it.forEach { source, result ->
                                when (result) {
                                    is SourceReadSuccess -> log.trace("Source \"$source\": ${result.values.size} values read")
                                    is SourceReadError -> log.error("$source returned error ${result.error} ")
                                }
                            }
                        } else if (logger.level == LogLevel.INFO) {
                            val valueCount = it.values.filterIsInstance<SourceReadSuccess>().sumOf { w -> (w).values.size }
                            if (valueCount != 0) {
                                val sourceCount = it.values.size
                                log.info("Read $valueCount values from $sourceCount sources")
                            }
                        }

                        val reply = it.asReadValuesReply(useCompressionForData)
                        emit(reply)
                    }
                }
            }
        }


        override fun readMetrics(request: Metrics.ReadMetricsRequest): Flow<Metrics.MetricsDataMessage> = channelFlow {

            val log = logger.getCtxLoggers(className, "readMetrics")

            // Interval between reading metrics
            val interval = request.interval.toDuration(DurationUnit.MILLISECONDS)

            while (initialized) {
                MetricsAsFlow(_protocolAdapter?.metricsCollector, interval, logger).metricsFlow.buffer(100).cancellable().collect {
                    try {
                        if (!initialized) {
                            return@collect
                        }
                        val reply = it.grpcMetricsDataMessage
                        send(reply)

                    } catch (e: Exception) {
                        if (!e.isJobCancellationException)
                           log.errorEx("Error building or emitting readMetrics reply", e)
                    }
                }
            }
        }

        override suspend fun initializeAdapter(request: InitializeAdapterRequest): InitializeAdapterResponse {
            try {

                val aux = request.auxiliarySettingsMap[CONFIG_CLOUD_SECRETS]
                val secrets = aux?.settingsMap?.map { (k, v) -> k to v }?.toMap()
                logger.addSecrets(secrets)

                val log = logger.getCtxLoggers(className, "initializeAdapter")

                log.trace("Received adapter configuration ${request.adapterConfiguration}")

                _initialized = false

                val serviceConfiguration: ServiceConfiguration = ConfigReader.createConfigReader(
                    configStr = request.adapterConfiguration,
                    allowUnresolved = true,
                    secretsManager = null).getConfig()

                val secretsManager = SecretsManager.createSecretsManager(serviceConfiguration, logger)
                runBlocking {
                    secretsManager?.syncSecretsFromService(serviceConfiguration.secretsManagerConfiguration?.cloudSecrets ?: emptyList())
                }

                val configReader = ConfigReader.createConfigReader(
                    configStr = request.adapterConfiguration, allowUnresolved = false,
                    secretsManager)

                val adapter = adapterID ?: serviceConfiguration.protocolAdapters.keys.first()
                _protocolAdapter = createAdapter(adapter, configReader, logger)
                _protocolAdapter?.init()
                _initialized = true

                useCompressionForData = getServerCompression(configReader, adapter)

                initializeHealthProbeService(serviceConfiguration)


            } catch (e: java.lang.Exception) {
                return InitializeAdapterResponse.newBuilder().setInitialized(false).setError(e.message).build()
            }

            return InitializeAdapterResponse.newBuilder().setInitialized(true).build()
        }

    }

    private fun getServerCompression(configReader: ConfigReader, adapter: String): Boolean {
        val serviceConfiguration = configReader.getConfig<ServiceConfiguration>()
        val protocolAdapter = serviceConfiguration.protocolAdapters[adapter]
        val serverConfiguration = getProtocolServerConfiguration(serviceConfiguration, protocolAdapter)
        return serverConfiguration?.compression ?: false
    }

    private fun isHealthy(): Boolean {
        // give server 10 seconds to start
        return if (DateTime.systemDateTime() < startTime.plusSeconds(10)) true
        else {
            try {
                // server must be set and have listening sockets
                (_server != null && _server!!.listenSockets.isNotEmpty() && !_server!!.isTerminated)
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

    private suspend fun initializeHealthProbeService(serviceConfiguration: ServiceConfiguration) {

        healthProbeService?.stop()

        val server = serviceConfiguration.protocolAdapterServers.values.first()
        val healthProbeConfiguration = server.healthProbeConfiguration
        healthProbeService = if (healthProbeConfiguration == null) null else
            try {
                val service =
                    HealthProbeService(healthProbeConfiguration, serviceStopFunction = ::stopUnhealthyService, checkFunction = ::isHealthy, logger = logger)
                serverScope.launch {
                    delay(1.toDuration(DurationUnit.MINUTES))
                    service.restartIfInactive()
                }
                service
            } catch (e: Exception) {
                null
            }
        healthProbeService?.start()
    }

    companion object {

        fun createProtocolAdapterService(args: Array<String>,
                                         configurationStr: String,
                                         logger: Logger,
                                         createAdapter: CreateProtocolAdapterMethod): Service {

            val cmd = ProtocolServerCommandLine(args)

            val serviceConfiguration = ConfigReader.createConfigReader(
                configStr = configurationStr,
            ).getConfig<ServiceConfiguration>()


            // get protocol id which could be specified on the command line or be read from the configuration if it only has a single protocol
            val protocolAdapterID = getAdapterProtocolID(cmd, serviceConfiguration)
            val protocolConfiguration = getProtocolConfig(serviceConfiguration, protocolAdapterID)
            val protocolServerConfiguration = getProtocolServerConfiguration(serviceConfiguration, protocolConfiguration)


            val logLevel: LogLevel = cmd.logLevel ?: serviceConfiguration.logLevel
            logger.level = logLevel

            val configuration = buildServerConfiguration(cmd, protocolServerConfiguration)

            return IpcAdapterService(
                adapterID = protocolAdapterID,
                serverConfig = configuration,
                logger = logger,
                createAdapter = createAdapter,
                tuningConfiguration = serviceConfiguration.tuningConfiguration
            )
        }

        private fun getAdapterProtocolID(cmd: ProtocolServerCommandLine, serviceConfiguration: ServiceConfiguration): String? =
            cmd.protocolAdapterID
            ?: when (serviceConfiguration.protocolAdapters.size) {
                0 -> null
                1 -> serviceConfiguration.activeTargets.keys.first()
                else -> throw ProtocolAdapterException("There are multiple adapters for this adapter type in the configuration, use the \"$OPTION_PROTOCOL_ADAPTER\" parameter to specify the adapter for this service instance")
            }


        private fun getProtocolServerConfiguration(
            serviceConfiguration: ServiceConfiguration,
            protocolAdapterConfig: ProtocolAdapterConfiguration?
        ): ServerConfiguration? {

            if (protocolAdapterConfig == null) {
                return null
            }

            val protocolServerID = protocolAdapterConfig.protocolAdapterServer

            return (if (protocolServerID != null) serviceConfiguration.protocolAdapterServers[protocolServerID] else null)
                   ?: throw ProtocolAdapterException("Server \"protocolServerID\" for does not exist, existing servers are  ${serviceConfiguration.protocolAdapterServers.keys}")

        }

        private fun getProtocolConfig(serviceConfiguration: ServiceConfiguration, protocolAdapterID: String?): ProtocolAdapterConfiguration? {
            return if (serviceConfiguration.protocolAdapters.isNotEmpty()) serviceConfiguration.protocolAdapters[protocolAdapterID]
                                                                           ?: throw ProtocolAdapterException("Protocol Adapter \"$protocolAdapterID\" does not exist in configuration, existing protocols adapters are ${serviceConfiguration.protocolAdapters.keys}")
            else null

        }

    }
}
