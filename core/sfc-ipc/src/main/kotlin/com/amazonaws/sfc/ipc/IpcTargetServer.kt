/*
 Copyright (c) 2022. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.ipc


import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.SecretsManagerConfiguration
import com.amazonaws.sfc.config.ServerConfiguration
import com.amazonaws.sfc.config.ServiceConfiguration
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.ipc.extensions.GrpcTargetValueAsNativeExt.asTargetData
import com.amazonaws.sfc.ipc.extensions.GrpcTargetValueFromNativeExt
import com.amazonaws.sfc.ipc.extensions.grpcMetricsDataMessage
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.secrets.SecretsManager
import com.amazonaws.sfc.service.HealthProbeService
import com.amazonaws.sfc.service.Service
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.getIp4NetworkAddress
import io.grpc.BindableService
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess
import kotlin.time.DurationUnit
import kotlin.time.toDuration

typealias CreateWriterMethod = (c: ConfigReader, t: String, l: Logger, m: TargetResultHandler?) -> TargetWriter

/**
 * IPC server for targets
 * @property targetID String ID of the target
 * @property serverConfig ServerConfiguration Configuration for the server
 * @property logger Logger Logger for output
 * @property createWriter CreateWriterMethod Function called by the server to create a writer using
 * rhe configuration obtained from the init rpc call
 */
class IpcTargetServer(
    private var targetID: String?,
    private var targetType: String,
    serverConfig: ServerConfiguration,
    logger: Logger,
    private val createWriter: CreateWriterMethod
) : IpcBaseService(serverConfig, logger), Service, TargetResultHandler {

    // writer is created from configuration passed in init service call
    private var _writer: TargetWriter? = null

    private val className = this::class.java.simpleName
    val serverScope = buildScope(className)

    private var _resulHandlerReturnedData: ResulHandlerReturnedData? = null

    val resultChannel = Channel<TargetResult>(100)

    private var serverActive = true

    private val writer: TargetWriter?
        get() {
            return _writer!!
        }

    private var useCompressionForReplies = false

    private var healthProbeService: HealthProbeService? = null
    private val startTime = DateTime.systemDateTime()

    override val serviceImplementation: BindableService
        get() = IpcTargetService()

    /**
     * Starts the server
     */
    override suspend fun start() {

        val log = logger.getCtxLoggers(className, "start")

        grpcServer.start()
        val addressAndPort = grpcServer.listenSockets.first() as InetSocketAddress
        val addressAndPortStr = "${addressAndPort.address.hostAddress}:${addressAndPort.port}"
        log.info("Target IPC service started, listening on  $addressAndPortStr, connection type is ${serverConfig.serverConnectionType})")

        Runtime.getRuntime().addShutdownHook(shutdownTask(log))
    }

    /**
     * Executed by GRPC server when shutting down
     * @param log ContextLogger
     * @return Thread
     */
    private fun shutdownTask(log: Logger.ContextLogger) =
        Thread {
            log.info("Shutting down IPC-Server")
            runBlocking {
                withTimeoutOrNull(15.toDuration(DurationUnit.SECONDS)) {
                    _writer?.close()
                }
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
    }

    // Blocks and waits for the server to shut down
    override suspend fun blockUntilShutdown() {
        // Blocks until server is stopped
        withContext(Dispatchers.IO) {
            grpcServer.awaitTermination()
        }
    }

    /**
     * Inner class that implements the logic of a target service
     */
    private inner class IpcTargetService : TargetAdapterServiceGrpcKt.TargetAdapterServiceCoroutineImplBase() {

        private var _initialized = false

        private var elementNames: com.amazonaws.sfc.config.ElementNamesConfiguration = com.amazonaws.sfc.config.ElementNamesConfiguration.DEFAULT_TAG_NAMES

        /**
         * Handles the writeValues method of the target service
         * @param requests Flow<WriteValuesRequest> Flow to collect the write requests from as this is a client streaming method.
         * @return Empty
         */
        override fun writeValues(requests: Flow<WriteValuesRequest>): Flow<TargetResultResponse> = flow {

            val scope = buildScope("READ-REQUEST")
            scope.launch {
                requests.collect { request: WriteValuesRequest ->
                    // convert the request to a map and pass it to the writer of the target
                    writer?.writeTargetData(request.asTargetData())
                }
            }

            while (serverActive) {
                val results = resultChannel.receive()
                val response = buildTargetResultResponse(results, useCompressionForReplies)
                emit(response)
            }
        }

        override fun readMetrics(request: Metrics.ReadMetricsRequest): Flow<Metrics.MetricsDataMessage> = channelFlow {
            val interval = request.interval.toDuration(DurationUnit.SECONDS)

            // no writer yet, send empty metrics
            while (_writer == null) {
                send(Metrics.MetricsDataMessage.getDefaultInstance())
                delay(interval)
            }

            writer?.metricsProvider?.read(interval) { metricData ->
                try {
                    runBlocking {
                        send(metricData.grpcMetricsDataMessage)
                    }
                } catch (e: Exception) {
                    logger.getCtxErrorLog(className, "readMetrics")
                }
                true
            }

        }


        private fun buildTargetResultResponse(results: TargetResult, compression: Boolean): TargetResultResponse {
            val responseBuilder = TargetResultResponse.newBuilder()

            responseBuilder.target = results.targetID

            if (results.ackSerialList != null) {
                responseBuilder.addAllAckSerials(results.ackSerialList)
            }

            if (results.ackMessageList != null) {
                responseBuilder.addAllAckRequests(results.ackMessageList!!.map { GrpcTargetValueFromNativeExt.newWriteValuesRequest(it, compression) })
            }

            if (results.nackSerialList != null) {
                responseBuilder.addAllNackSerials(results.nackSerialList)
            }

            if (results.nackMessageList != null) {
                responseBuilder.addAllNackRequests(results.nackMessageList!!.map { GrpcTargetValueFromNativeExt.newWriteValuesRequest(it, compression) })
            }
            if (results.errorSerialList != null) {
                responseBuilder.addAllErrorSerials(results.errorSerialList)
            }

            if (results.errorMessageList != null) {
                responseBuilder.addAllErrorRequests(results.errorMessageList!!.map { GrpcTargetValueFromNativeExt.newWriteValuesRequest(it, compression) })
            }
            return responseBuilder.build()
        }


        override suspend fun initializeTarget(request: InitializeTargetRequest): InitializeTargetResponse = coroutineScope {

            val log = logger.getCtxLoggers(className, "initializeTarget")
            try {

                _initialized = false

                val requestTargetID = request.targetID

                // create a reader for the received configuration data from the request and read the configuration
                val configReader = ConfigReader.createConfigReader(request.targetConfiguration)

                val aux = request.auxiliarySettingsMap[SecretsManagerConfiguration.CONFIG_CLOUD_SECRETS]
                val secrets = aux?.settingsMap?.map { (k, v) -> k to v }?.toMap()
                logger.addSecrets(secrets)

                val config: ServiceConfiguration = configReader.getConfig()

                log.info("Received target configuration \"${request.targetConfiguration}")

                elementNames = config.elementNames


                // If a target ID was specified from the command line or the config file check if is the expected target
                if (targetID != null && requestTargetID != targetID) {
                    throw IpcException("Configuration target ID \"$requestTargetID\" does not match expected target ID \"$targetID\"")
                }
                targetID = requestTargetID

                // Get the configuration data for the target and check if it is the expected type for this target
                val targetConfiguration = config.targets[targetID]
                                          ?: throw IpcException("Target type \"$targetID\" does not exist in configuration, available targets are ${config.targets.keys}")

                if (targetConfiguration.targetType != targetType) {
                    throw IpcException("Configuration target type \"${targetConfiguration.targetType}\" does not match expected target type \"$targetType\"")
                }

                log.info(
                    "Handling target initialization request for target \"$targetID\" of type \"${targetConfiguration.targetType}\" " +
                    "using configuration \"${request.targetConfiguration}\""
                )

                if (_writer != null) {
                    withTimeoutOrNull(10.toDuration(DurationUnit.SECONDS)) {
                        _writer?.close()

                    } ?: log.warning("Timeout closing writer")
                }

                useCompressionForReplies = getTargetServerConfiguration(config, targetID)?.compression ?: false

                _resulHandlerReturnedData = request.returnedData.asResulHandlerReturnedData()
                val handler: TargetResultHandler? = if (_resulHandlerReturnedData?.returnsAnyData == true) this@IpcTargetServer else null
                _writer = createWriter(configReader, requestTargetID, logger, handler)

                _initialized = true

                initializeHealthProbeService(configReader)

                log.info("Target writer for target \"$targetID\" of type \"${targetConfiguration.targetType}\" created")

            } catch (e: java.lang.Exception) {
                log.error("Error initializing target from configuration \"${request.targetConfiguration}\", ${e.message}")
                InitializeTargetResponse.newBuilder().setInitialized(false).setError(e.message).build()
            }

            InitializeTargetResponse.newBuilder().setInitialized(true).build()
        }

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

    private suspend fun initializeHealthProbeService(configReader: ConfigReader) {

        healthProbeService?.stop()

        val config = configReader.getConfig<ServiceConfiguration>()
        val adapter = config.targets[targetID]
        val targetServerID = adapter?.server
        val targetServerConfiguration = config.targetServers[targetServerID]
        val healthProbeConfiguration = targetServerConfiguration?.healthProbeConfiguration

        healthProbeService = if (healthProbeConfiguration == null) null else
            try {
                val service =
                    HealthProbeService(targetServerConfiguration.healthProbeConfiguration!!, serviceStopFunction = ::stopUnhealthyService, checkFunction = ::isHealthy, logger = logger)
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

        private fun MessageData?.asResultHandlerData(): ResulHandlerReturnedData.ResultHandlerData =
            if (this == null) ResulHandlerReturnedData.ResultHandlerData.NONE
            else
                when (this) {
                    MessageData.NONE -> ResulHandlerReturnedData.ResultHandlerData.NONE
                    MessageData.SERIALS -> ResulHandlerReturnedData.ResultHandlerData.SERIALS
                    MessageData.MESSAGES -> ResulHandlerReturnedData.ResultHandlerData.MESSAGES
                    else -> ResulHandlerReturnedData.ResultHandlerData.NONE
                }

        fun ReturnedDataMessage?.asResulHandlerReturnedData(): ResulHandlerReturnedData =
            if (this == null) ResulHandlerReturnedData.returnNoData
            else
                ResulHandlerReturnedData(
                    ack = this.ack.asResultHandlerData(),
                    nack = this.nack.asResultHandlerData(),
                    error = this.error.asResultHandlerData())


        /**
         * Creates instance of target server
         * @param args Array<String> Command line arguments
         * @param targetType String Target type
         * @param logger Logger Logger for output
         * @param createWriter CreateWriterMethod Method called to create instance of the writer for the Target service
         * @return Service
         */
        fun createIpcTargetServer(
            args: Array<String>,
            configurationStr: String,
            targetType: String,
            logger: Logger,
            createWriter: CreateWriterMethod
        ): Service {

            // process command line parameters
            val cmd = IpcTargetServerCommandLine(args)

            var serviceConfiguration = ConfigReader.createConfigReader(
                configStr = configurationStr,
                allowUnresolved = true,
                secretsManager = null).getConfig<ServiceConfiguration>()

            val secretsManager = SecretsManager.createSecretsManager(serviceConfiguration, logger)
            runBlocking {
                secretsManager?.syncSecretsFromService(serviceConfiguration.secretsManagerConfiguration?.cloudSecrets ?: emptyList())
            }
            val configReader = ConfigReader.createConfigReader(configurationStr, allowUnresolved = false, secretsManager)
            serviceConfiguration = configReader.getConfig()

            // set log level from command line of config file
            val logLevel: LogLevel = cmd.logLevel ?: serviceConfiguration.logLevel
            logger.level = logLevel

            // get target id which could be specified on the command line or be read from the configuration if it only has a single target
            val targetID = getTargetID(cmd, serviceConfiguration)
            val targetServerConfiguration = getTargetServerConfiguration(serviceConfiguration, targetID)

            // get port number which could be specified on the command line or read from the configuration, if there are more than one targets in the configuration
            // the target id needs to be specified on the command line
            val port = getPort(cmd, targetServerConfiguration)

            val address = getAddress(cmd)

            // key and cert when using SSL
            val key = cmd.key ?: targetServerConfiguration?.serverPrivateKey?.absolutePath
            val cert = cmd.cert ?: targetServerConfiguration?.serverCertificate?.absolutePath

            // create configuration just for this server
            val configuration = ServerConfiguration.create(address = address,
                port = port,
                serverPrivateKey = key,
                serverCertificate = cert)
            configuration.validate()

            return IpcTargetServer(
                targetID = targetID,
                targetType = targetType,
                serverConfig = configuration,
                logger = logger,
                createWriter = createWriter
            )
        }

        private fun getAddress(cmd: IpcTargetServerCommandLine) = (getIp4NetworkAddress(cmd.networkInterface)
                                                                   ?: throw ProtocolAdapterException("No IP4 network address for interface ${cmd.networkInterface}"))

        private fun getTargetID(cmd: IpcTargetServerCommandLine, serviceConfiguration: ServiceConfiguration): String? {
            return cmd.targetID ?: if (serviceConfiguration.activeTargets.size == 1) serviceConfiguration.activeTargets.keys.first() else null

        }

        private fun getPort(cmd: IpcTargetServerCommandLine, targetServerConfiguration: ServerConfiguration?): Int {
            var port = cmd.port
            if (port == null) {
                port = targetServerConfiguration?.port
            }
            if (port == null) throw IpcException("Unable to find service port from commandline or configuration file")
            return port
        }


        private fun getTargetServerConfiguration(serviceConfiguration: ServiceConfiguration, targetID: String?): ServerConfiguration? {

            val targetConfig = if (serviceConfiguration.activeTargets.isNotEmpty())
                serviceConfiguration.activeTargets[targetID]
                ?: throw IpcException("Target ID \"$targetID\" does not exist or is not active in configuration, " +
                                      "existing active targets are ${serviceConfiguration.activeTargets.keys}")
            else null

            if (targetConfig != null) {
                val targetServerID = targetConfig.server

                return (if (targetServerID != null) serviceConfiguration.targetServers[targetServerID] else null)
                       ?: throw IpcException("Server for target ID \"$targetServerID\" does not exist, " +
                                             "existing servers are  ${serviceConfiguration.targetServers.keys}")
            }
            return null
        }
    }

    override fun handleResult(targetResult: TargetResult) {
        runBlocking {
            resultChannel.send(targetResult)
        }
    }

    override val returnedData: ResulHandlerReturnedData?
        get() = _resulHandlerReturnedData


}




