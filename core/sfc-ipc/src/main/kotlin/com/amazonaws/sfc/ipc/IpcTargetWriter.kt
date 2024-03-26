
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.ipc

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_AWS_IOT_CREDENTIAL_PROVIDER_CLIENTS
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_AWS_VERSION
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_TARGETS
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_TARGET_SERVERS
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_TARGET_TYPES
import com.amazonaws.sfc.config.ConfigReader.Companion.convertExternalPlaceholders
import com.amazonaws.sfc.config.SecretsManagerConfiguration.Companion.CONFIG_CLOUD_SECRETS
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.data.JsonHelper.Companion.fromJsonExtended
import com.amazonaws.sfc.data.JsonHelper.Companion.gsonExtended
import com.amazonaws.sfc.data.TargetWriter.Companion.TIMOUT_TARGET_WRITE
import com.amazonaws.sfc.ipc.extensions.GrpcTargetValueAsNativeExt.asTargetData
import com.amazonaws.sfc.ipc.extensions.GrpcTargetValueFromNativeExt
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.log.Logger.Companion.CONF_LOG_WRITER
import com.amazonaws.sfc.metrics.MetricsConfiguration.Companion.CONFIG_METRICS
import com.amazonaws.sfc.metrics.MetricsProvider
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.amazonaws.sfc.service.addExternalSecretsConfig
import com.amazonaws.sfc.util.buildContext
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.isJobCancellationException
import io.grpc.StatusException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.flow


/**
 * Writes data to the GRPC IPC server
 * @property serverConfig ConfigReader Configuration reader
 * @property targetID String ID of the target
 * @property logger Logger Logger for output
 */
class IpcTargetWriter(private val targetID: String,
                      configReader: ConfigReader,
                      serverConfig: ServerConfiguration,
                      logger: Logger,
                      private var resultHandler: TargetResultHandler?) :
        IpcClientBase<IpcTargetWriterClient>(configReader = configReader,
            serverConfig = serverConfig,
            logger,
            createClient = { managedChannel -> IpcTargetWriterClient(managedChannel) }),
        TargetWriter {

    private val className = this::class.java.simpleName

    private val useCompression: Boolean = serverConfig.compression

    private val scope = buildScope("IPC Target Writer", dispatcher = Dispatchers.IO)

    // Channel to send the data are to be sent to the coroutine that does the actual writing
    private val requestChannel = Channel<WriteValuesRequest>(100)

    // Coroutine that writes the data to the service
    private val writerWorker = scope.launch( buildContext("writer", scope)) {
        val log =  logger.getCtxLoggers(className, "writer")
        try {
            writer()
        }catch (e : Exception){
            if (!e.isJobCancellationException)
                log.error("Error in writerWorker, ${e.message}")
        }
    }

    private var client: IpcTargetWriterClient? = null

    // Writes data read from the internal request channel to the server
    private suspend fun CoroutineScope.writer() {

        val log = logger.getCtxLoggers(className, "writer")

        // while the coroutine that runs the methods is active

        while (isActive) {

            try {
                client = getIpcClient()

                // Setup target IPC service by sending an initial or new config
                if (client?.isInitialized != true) {
                    if (!initializeTargetAdapter()) {
                        delay(WAIT_AFTER_ERROR)
                        continue
                    }
                }

                // Start writing the data by passing a flow of value read from the request channel
                // A method is passed which is the handler for received results from the target
                client?.writeTargetData(
                    flow {
                        for (writeValuesRequest in requestChannel) {
                            emit(writeValuesRequest)
                        }
                    }) { resultRequest: TargetResultResponse ->
                    if (resultHandler != null) {
                        try {
                            resultHandler?.handleResult(resultRequest.asTargetResult)
                        } catch (e: Exception) {
                            logger.getCtxErrorLog(className, "writer")("Error processing target response, $resultRequest, ${e.message}")
                        }
                    }
                }

            } catch (e: Exception) {
                if (!e.isJobCancellationException) {
                    if (client?.lastError == null) {
                        client?.lastError = e
                    }
                    if (e.message?.contains("shutdownNow") == true) {
                        log.info("Target service is shutting down")
                    } else {
                        var s = "Error communicating with target IPC service on ${serverConfig.addressStr}, "
                        s += if (e is StatusException) "${e.cause?.message ?: e.message}" else e.message
                        log.error(s)
                    }
                    resetIpcClient()
                    delay(IpcSourceReader.WAIT_AFTER_ERROR)
                }
            } finally {
                resetIpcClient()
            }

        }
    }

    // Extracts the required configuration data for the adapter and send it to the IPC service to (re)initialize the target
    private suspend fun initializeTargetAdapter(): Boolean {
        val log = logger.getCtxLoggers(className, "initializeTargetWriterAdapter")

        if (client == null) {
            log.error("No client to initialize target adapter")
        }

        log.info("Initializing IPC target service  for  \"$targetID\" on server ${serverConfig.addressStr}")
        val targetConfigurationStr = extractConfigurationForTarget()
        log.info("Sending configuration \"$targetConfigurationStr\" to target \"$targetID\"")

        val request = buildInitializeRequest(targetConfigurationStr, configReader.usedSecrets)

        log.info("IPC server for target \"$targetID\" initialized")

        return try {
            if (client != null) {
                client?.initializeAdapter(request)
                true
            } else false

        } catch ( e : StatusException){
            log.error("Error IPC initializing server for target \"$targetID\" on ${serverConfig.addressStr}, ${e.cause?.message ?: e.message}")
            false
        } catch (e: Exception) {
            log.errorEx("Error IPC initializing  server for target \"$targetID\" on ${serverConfig.addressStr}", e)
            false
        }
    }


    // Extracts the sections required for the target
    private fun extractConfigurationForTarget(): String {

        // Need raw json as well as controller is not aware of all target types
        val configurationMap = fromJsonExtended(configReader.jsonConfig, Map::class.java)

        // Mapping with selected elements for target configuration, wee need to work with the raw JSON as
        // the ServiceControllerConfiguration type is not aware of specific target configuration types.
        val outputConfig = mutableMapOf<String, Any?>(CONFIG_AWS_VERSION to configuration.awsVersion)

        if (configurationMap.containsKey(CONF_LOG_WRITER)) {
            outputConfig[CONF_LOG_WRITER] = configurationMap[CONF_LOG_WRITER]
        }

        if (configurationMap.containsKey(CONFIG_METRICS)) {
            outputConfig[CONFIG_METRICS] = configurationMap[CONFIG_METRICS]
        }

        // Get the target configuration
        val targetsMap = configurationMap[CONFIG_TARGETS] as Map<*, *>

        // get all target IDs, including all sub-targets in target chain
        val usedTargetIDs = usedTargets(targetID, emptySet(), configuration)

        // Include targets
        outputConfig[CONFIG_TARGETS] = usedTargetIDs.associateWith { targetsMap[it] }

        // Build map for all targets types configured as in process targets
        val targetTypes = buildTargetTypesMap(usedTargetIDs)
        if (targetTypes.isNotEmpty()) {
            outputConfig[CONFIG_TARGET_TYPES] = targetTypes
        }

        // Build map for all targets configured as ipc targets
        val targetServers = buildTargetServersMap(usedTargetIDs)
        if (targetServers.isNotEmpty()) {
            outputConfig[CONFIG_TARGET_SERVERS] = targetServers
        }

        // build map for all aws client configurations used by targets
        val clientConfigurationMap = buildCredentialsClientConfigurationMap(configuration, usedTargetIDs)
        if (clientConfigurationMap.isNotEmpty()) {
            outputConfig[CONFIG_AWS_IOT_CREDENTIAL_PROVIDER_CLIENTS] = clientConfigurationMap
        }

        addExternalSecretsConfig(configuration, configReader, outputConfig)

        val s = gsonExtended().toJson(outputConfig)
        // replace placeholders that need to be replaced externally with normal placeholders
        return convertExternalPlaceholders(s)

    }

    // Build a map of all servers used by the target following the possible chain of targets
    private fun buildTargetServersMap(usedTargetIDs: Set<String>): Map<String?, ServerConfiguration?> {
        val targetServers = usedTargetIDs.mapNotNull {
            val c: TargetConfiguration? = configuration.targets[it]
            if (!c?.server.isNullOrEmpty()) c?.server to configuration.targetServers[c?.server] else null
        }.toMap()
        return targetServers
    }

    // Build a map of all servers types used by the target following the possible chain of targets
    private fun buildTargetTypesMap(usedTargetIDs: Set<String>): Map<String?, InProcessConfiguration?> {
        val targetTypes = usedTargetIDs.filter { it != targetID }.mapNotNull {
            val c: TargetConfiguration? = configuration.targets[it]
            if (c?.server.isNullOrEmpty()) c?.targetType to configuration.targetTypes[c?.targetType] else null
        }.toMap()
        return targetTypes
    }

    // Targets to include in the configuration, which could be chained targets as well
    private fun usedTargets(targetID: String, targetIDs: Set<String>, config: ServiceConfiguration): Set<String> {
        var targetChain = mutableSetOf(targetID)
        targetChain.addAll(targetIDs)

        val target = config.targets[targetID]
        target?.subTargets?.forEach {
            targetChain = usedTargets(it, targetChain, config) as MutableSet<String>
        }
        return targetChain
    }


    // Build a map of all used clientProviders
    private fun buildCredentialsClientConfigurationMap(configuration: ServiceConfiguration, targets: Set<String>): Map<String, Any?> {

        val s = sequence<Pair<String, Any>> {
            targets.forEach {
                val target = configuration.targets[it]
                val clientID = target?.credentialProviderClient
                if (clientID != null) {
                    val clientConfiguration = configuration.awsCredentialServiceClients[clientID]
                                              ?: throw IpcException("Client \"$clientID\" does not exist, " +
                                                                    "configured targets are ${configuration.awsCredentialServiceClients.keys}")
                    yield(clientID to clientConfiguration.asConfigurationMap())
                }
            }
        }
        return s.toMap()
    }


    // Builds the request to send to the target server to initialize it
    private fun buildInitializeRequest(targetConfigurationStr: String, secrets: MutableMap<String, String>): InitializeTargetRequest {
        val requestBuilder = InitializeTargetRequest.newBuilder()

        requestBuilder.targetID = targetID
        requestBuilder.targetConfiguration = targetConfigurationStr

        val usedSecretsInConfig = secrets.filter { targetConfigurationStr.contains(it.value) }
        requestBuilder.putAuxiliarySettings(CONFIG_CLOUD_SECRETS, AuxiliarySettings.newBuilder().putAllSettings(usedSecretsInConfig).build())

        // if there is a result handler get the result values it wants to receive on this handler called by the called target
        requestBuilder.returnedData = buildHandlerReturnDataMessage()
        return requestBuilder.build()
    }

    // Build message to specify what data is expected back by the result handler
    private fun buildHandlerReturnDataMessage(): ReturnedDataMessage {
        val returnedData: ResulHandlerReturnedData = resultHandler?.returnedData ?: ResulHandlerReturnedData.returnNoData
        val returnedDataBuilder = ReturnedDataMessage.newBuilder()
        returnedDataBuilder.ack = returnedData.ack.asMessageData()
        returnedDataBuilder.nack = returnedData.nack.asMessageData()
        returnedDataBuilder.error = returnedData.error.asMessageData()
        return returnedDataBuilder.build()
    }

    // Build MessageData fields
    private fun ResulHandlerReturnedData.ResultHandlerData?.asMessageData(): MessageData =
        if (this == null) MessageData.NONE
        else
            when (this) {
                ResulHandlerReturnedData.ResultHandlerData.NONE -> MessageData.NONE
                ResulHandlerReturnedData.ResultHandlerData.SERIALS -> MessageData.SERIALS
                ResulHandlerReturnedData.ResultHandlerData.MESSAGES -> MessageData.MESSAGES
            }


    // Build native Target result from response message
    private val TargetResultResponse.asTargetResult
        get() =
            TargetResult(
                targetID = this.target,
                ackSerialList = this.ackSerialsList,
                ackMessageList = this.ackRequestsList.map { it.asTargetData() },
                nackSerialList = this.nackSerialsList,
                nackMessageList = this.nackRequestsList.map { it.asTargetData() },
                errorSerialList = this.errorSerialsList,
                errorMessageList = this.ackRequestsList.map { it.asTargetData() }
            )


    /**
     * Sends the data as GRPC request to the server
     * @param targetData TargetData
     */
    override suspend fun writeTargetData(targetData: TargetData) = coroutineScope {
        val request: WriteValuesRequest = GrpcTargetValueFromNativeExt.newWriteValuesRequest(targetData, useCompression)
        var done = false
        var ex: Throwable? = null
        var waitFor = START_WAIT_LEN
        var timeUsed = 0L

        while (!done && timeUsed < TIMOUT_TARGET_WRITE) {
            try {
                withTimeout(waitFor) {
                    requestChannel.trySendBlocking(request)
                        .onSuccess {
                            done = true
                            ex = null
                        }
                        .onClosed { e -> throw IpcException("Error writing close to target channel, $e") }
                }
                    .onFailure { e -> throw IpcException("Error writing to target channel, $e") }
            } catch (e: TimeoutCancellationException) {
                timeUsed += waitFor
                waitFor = minOf(waitFor * 2, MAX_WAIT_LEN)
                ex = e
            }
        }

        if (ex != null) throw ex as Throwable
    }

    override val isInitialized: Boolean
        get() = client?.isInitialized ?: false


    override suspend fun close() {
        client?.close()
        writerWorker.cancel()
        writerWorker.join()
        requestChannel.close()
    }

    // Creates a metrics provider using and instance of a IpcTargetWriterClient used for reading the data
    override val metricsProvider: MetricsProvider?
        get() {

            val targetMetricsConfig = configuration.targets[targetID]?.metrics ?: MetricsSourceConfiguration()
            return if ((configuration.metrics != null) && (targetMetricsConfig.enabled))
                IpcMetricsProvider(configReader = configReader,
                    serverConfig = serverConfig,
                    isIpcServiceInitialized = { this.isInitialized },
                    logger = logger) { m -> IpcTargetWriterClient(m) }
            else null
        }


    companion object {

        private val className = this::class.java.simpleName

        /**
         * Creates a writer from its configuration
         * @param configReader ConfigReader Reader fro target configuration
         * @param targetID String  Target ID
         * @param logger Logger Logger for output
         * @return TargetWriter? Created IPC target writer
         */
        fun createIpcTargetWriter(configReader: ConfigReader,
                                  targetID: String,
                                  serverID: String,
                                  logger: Logger,
                                  targetResultHandler: TargetResultHandler? = null): TargetWriter? {

            val errLog = logger.getCtxErrorLog(className, "createIpcTargetWriter")

            val config = configReader.getConfig<ServiceConfiguration>()

            // obtain target server config
            val targetServerConfig = config.targetServers[serverID]
            if (targetServerConfig == null) {
                errLog("IPC target server \"${serverID}\" does not exist, existing servers are ${config.targetServers.keys}")
                return null
            }
            return IpcTargetWriter(targetID, configReader, targetServerConfig, logger, targetResultHandler)
        }

        const val WAIT_AFTER_ERROR = 10000L
        private const val MAX_WAIT_LEN = 1000L
        private const val START_WAIT_LEN = 50L
    }
}
