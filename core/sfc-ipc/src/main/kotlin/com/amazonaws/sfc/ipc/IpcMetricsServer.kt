/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.ipc

import com.amazonaws.sfc.config.BaseConfigurationWithMetrics
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.ConfigWithMetrics
import com.amazonaws.sfc.config.ServerConfiguration
import com.amazonaws.sfc.data.ProtocolAdapterException
import com.amazonaws.sfc.ipc.extensions.asNativeMetricsData
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricsWriter
import com.amazonaws.sfc.secrets.SecretsManager
import com.amazonaws.sfc.service.HealthProbeService
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.system.DateTime.systemDateTime
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.getIp4NetworkAddress
import com.google.protobuf.Empty
import io.grpc.BindableService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess
import kotlin.time.DurationUnit
import kotlin.time.toDuration

typealias CreateMetricsWriterMethod = (c: ConfigReader, l: Logger) -> MetricsWriter


class IpcMetricsServer(
    serverConfig: ServerConfiguration,
    logger: Logger,
    private val createMetricsWriter: CreateMetricsWriterMethod
) : IpcBaseService(serverConfig, logger), Service {

    private val className = this::class.java.simpleName

    private val serviceScope = buildScope(className)

    // writer is created from configuration passed in init service call
    private var _writer: MetricsWriter? = null
    private var healthProbeService: HealthProbeService? = null

    private var serverActive = true
    private val startTime = systemDateTime()

    private val writer: MetricsWriter
        get() {
            if (_writer == null) {
                throw IpcException("Writer has not been initialized")
            }
            return _writer!!
        }
    override val serviceImplementation: BindableService
        get() = IpcMetricsWriterService()

    /**
     * Starts the server
     */
    override suspend fun start() {


        val log = logger.getCtxLoggers(className, "start")

        grpcServer.start()
        val addressAndPort = grpcServer.listenSockets.first() as InetSocketAddress
        val addressAndPortStr = "${addressAndPort.address.hostAddress}:${addressAndPort.port}"
        log.info("Metrics IPC service started, listening on $addressAndPortStr, connection type is ${serverConfig.serverConnectionType}")

        Runtime.getRuntime().addShutdownHook(shutdownTask(log))
    }

    /**
     * Executed by GRPC server when shutting down
     * @param log ContextLogger
     * @return Thread
     */
    private fun shutdownTask(log: Logger.ContextLogger) =
        Thread {
            log.info("Shutting down Metrics IPC-Server")
            runBlocking {
                _writer?.close()
            }
            runBlocking {
                stop()
            }
        }

    /**
     * Stops the server
     */
    override suspend fun stop() {
        // Stop gRPC server
        serverActive = false
        grpcServer.shutdownNow().awaitTermination(15, TimeUnit.SECONDS)
        healthProbeService?.stop()
    }

    // Blocks and waits for the server to shut down
    override suspend fun blockUntilShutdown() {
        // Blocks until server is stopped
        withContext(Dispatchers.IO) {
            grpcServer.awaitTermination()
        }
    }

    private inner class IpcMetricsWriterService : MetricsWriterServiceGrpcKt.MetricsWriterServiceCoroutineImplBase() {

        override suspend fun writeMetrics(requests: Flow<Metrics.MetricsDataMessage>): Empty {

            val log = logger.getCtxLoggers(className, "writeMetrics")
            requests.collect { request: Metrics.MetricsDataMessage ->

                log.trace("Received ${request.dataPointsCount} data points to write to ${_writer!!::class.java.simpleName}")
                writer.writeMetricsData(request.asNativeMetricsData)
            }

            return Empty.getDefaultInstance()

        }


        override suspend fun initializeMetricsWriter(request: InitializeMetricsWriterRequest): InitializeMetricsWriterResponse {

            val log = logger.getCtxLoggers(className, "initializeMetricsWriter")

            log.info("Received metrics configuration \"${request.metricsConfiguration}\"")
            return try {

                val configReader = ConfigReader.createConfigReader(request.metricsConfiguration)

                if (_writer != null) {
                    withTimeoutOrNull(10.toDuration(DurationUnit.SECONDS)) {
                        _writer?.close()

                    } ?: log.warning("Timeout closing metrics writer")
                }

                _writer = createMetricsWriter(configReader, logger)

                initializeHealthProbeService(configReader)

                log.info("Metrics writer of type ${writer::class.java.simpleName} created")

                InitializeMetricsWriterResponse.newBuilder().setInitialized(true).build()

            } catch (e: Exception) {
                log.error("Error metrics from configuration \"${request.metricsConfiguration}\", ${e.message}")
                InitializeMetricsWriterResponse.newBuilder().setInitialized(false).setError(e.message).build()
            }

        }
    }

    private fun isHealthy(): Boolean {
        // give server 10 seconds to start
        return if (systemDateTime() < startTime.plusSeconds(10)) true
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


    private suspend fun initializeHealthProbeService(configReader: ConfigReader) {

        healthProbeService?.stop()

        val config = configReader.getConfig<ConfigWithMetrics>()
        val healthProbeConfiguration = config.metrics?.writer?.metricsServer?.healthProbeConfiguration
        healthProbeService = if (healthProbeConfiguration == null) null else
            try {
                val service =
                    HealthProbeService(healthProbeConfiguration, serviceStopFunction = ::stopUnhealthyService, checkFunction = ::isHealthy, logger = logger)
                serviceScope.launch {
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


        fun createIpcMetricsServer(
            args: Array<String>,
            configurationStr: String,
            logger: Logger,
            createMetricsWriter: CreateMetricsWriterMethod
        ): Service {

            // process command line parameters
            val cmd = IpcMetricsServerCommandLine(args)

            var serviceConfiguration = ConfigReader.createConfigReader(
                configStr = configurationStr,
                allowUnresolved = true,
                secretsManager = null).getConfig<BaseConfigurationWithMetrics>()

            val secretsManager = SecretsManager.createSecretsManager(serviceConfiguration, logger)
            runBlocking {
                secretsManager?.syncSecretsFromService(serviceConfiguration.secretsManagerConfiguration?.cloudSecrets ?: emptyList())
            }

            val configReader = ConfigReader.createConfigReader(configurationStr, allowUnresolved = false, secretsManager)
            serviceConfiguration = configReader.getConfig()

            // set log level from command line of config file
            val logLevel: LogLevel = cmd.logLevel ?: serviceConfiguration.logLevel
            logger.level = logLevel

            val metricsConfiguration = serviceConfiguration.metrics
            val metricsServerConfiguration = metricsConfiguration?.writer?.metricsServer

            val port = getPort(cmd, metricsServerConfiguration)
            val address = getAddress(cmd)

            // key and cert when using SSL
            val key = cmd.key ?: metricsServerConfiguration?.serverPrivateKey?.absolutePath
            val cert = cmd.cert ?: metricsServerConfiguration?.serverCertificate?.absolutePath

            // create configuration just for this server
            val configuration = ServerConfiguration.create(address = address, port = port, serverPrivateKey = key, serverCertificate = cert)
            configuration.validate()

            return IpcMetricsServer(
                serverConfig = configuration,
                logger = logger,
                createMetricsWriter = createMetricsWriter
            )
        }


        private fun getPort(cmd: IpcMetricsServerCommandLine, metricsServerConfiguration: ServerConfiguration?): Int {
            var port = cmd.port
            if (port == null) {
                port = metricsServerConfiguration?.port
            }
            if (port == null) throw IpcException("Unable to find metrics service port from commandline or configuration file")
            return port
        }

        private fun getAddress(cmd: IpcMetricsServerCommandLine): String {
            return getIp4NetworkAddress(cmd.networkInterface)
                   ?: throw ProtocolAdapterException("No IP4 network address for interface ${cmd.networkInterface}")
        }


    }

}