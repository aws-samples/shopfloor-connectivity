
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.ipc

import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_AWS_IOT_CREDENTIAL_PROVIDER_CLIENTS
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_AWS_VERSION
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CREDENTIAL_PROVIDER_CLIENT
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.ConfigReader.Companion.convertExternalPlaceholders
import com.amazonaws.sfc.config.ConfigWithMetrics
import com.amazonaws.sfc.config.MetricsWriterConfiguration.Companion.CONFIG_METRICS_METRICS_SERVER
import com.amazonaws.sfc.config.ServerConfiguration
import com.amazonaws.sfc.config.ServiceConfiguration
import com.amazonaws.sfc.data.JsonHelper.Companion.fromJsonExtended
import com.amazonaws.sfc.data.JsonHelper.Companion.gsonExtended
import com.amazonaws.sfc.data.TargetWriter.Companion.TIMOUT_TARGET_WRITE
import com.amazonaws.sfc.ipc.Metrics.MetricsDataMessage
import com.amazonaws.sfc.ipc.extensions.grpcMetricsDataMessage
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricsConfiguration.Companion.CONFIG_METRICS
import com.amazonaws.sfc.metrics.MetricsConfiguration.Companion.CONFIG_METRICS_WRITER_CONFIG
import com.amazonaws.sfc.metrics.MetricsData
import com.amazonaws.sfc.metrics.MetricsWriter
import com.amazonaws.sfc.service.addExternalSecretsConfig
import com.amazonaws.sfc.util.buildContext
import com.amazonaws.sfc.util.buildScope
import io.grpc.ManagedChannel
import io.grpc.StatusException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.flow
import java.util.concurrent.TimeUnit


class IpcMetricsWriter(
    private val configReader: ConfigReader,
    private val serverConfig: ServerConfiguration,
    private var logger: Logger,
) : MetricsWriter {

    private val className = this::class.java.simpleName

    // Get server address and port
    private val serverAddress = serverConfig.address
    private val serverPort = serverConfig.port

    val scope = buildScope("IPC Metrics Writer", dispatcher = Dispatchers.IO)

    private var initialized = false

    // Channel to send the data are to be sent to th coroutine that does the actual writing
    private val metricsDataChannel = Channel<MetricsDataMessage>(1)

    // GRPC IPC client
    private var ipcClient: MetricsWriterServiceGrpcKt.MetricsWriterServiceCoroutineStub? = null

    // Coroutine that writes the data to the service
    private val writerWorker = scope.launch(buildContext("writer", scope)) {
        try {
            writer()
        }catch (e : Exception){
            logger.getCtxErrorLogEx(className, "writer")("Error writing metrics to IPC server", e)
        }
    }

    private val configuration by lazy { configReader.getConfig<ServiceConfiguration>() }

    // get a client
    private var grpcClient: MetricsWriterServiceGrpcKt.MetricsWriterServiceCoroutineStub? = null

    // Writes data read from the internal request channel to the server
    private suspend fun CoroutineScope.writer() {

        val log = logger.getCtxLoggers(className, "writer")

        // while the coroutine that runs the methods is active
        while (isActive) {

            val serverAddress = "$serverAddress:$serverPort"
            try {
                // use the client or create a new one to reconnect after an error has occurred
                if (grpcClient == null) {

                    grpcClient = getClient()
                    if (grpcClient == null) {
                        log.error("Can not create GRPC client to send metrics to server on $serverAddress")
                        delay(WAIT_AFTER_ERROR)
                        continue
                    }

                    try {
                        log.info("Initializing IPC metrics service for server $serverAddress")
                        val metricsConfigurationStr = extractConfigurationForMetrics()
                        log.info("Sending configuration \"$metricsConfigurationStr\" to metrics Server")
                        sendInitializationConfiguration(metricsConfigurationStr)
                        log.info("IPC server for metrics writer initialized")
                    }catch (e : StatusException){
                        val errorMessage = "${e.cause?.message ?: e.message}"
                        log.error("Error IPC initializing  metrics writer server on $serverAddress, $errorMessage")
                        grpcClient = null
                        delay(WAIT_AFTER_ERROR)
                        continue
                    } catch (e: Exception) {
                        log.errorEx("Error IPC initializing  metrics writer server on $serverAddress", e)
                        grpcClient = null
                        delay(WAIT_AFTER_ERROR)
                        continue
                    }
                }
                writMetrics(grpcClient)

            } catch (e: Exception) {
                if (e::class.java.simpleName == "JobCancellationException") {
                    log.trace("Writer cancelled")
                } else {
                    var s = "Error communicating with metrics writer service on $serverAddress, "
                    s += if (e is StatusException) "${e.cause?.message ?: e.message}" else e.message
                    grpcClient = null
                    log.errorEx(s, e)
                    delay(WAIT_AFTER_ERROR)
                }
            }
        }

        // then the coroutine writing the values becomes inactive close the GRPC channel
        (withContext(Dispatchers.Default) {
            (ipcClient?.channel as ManagedChannel).shutdownNow().awaitTermination(15, TimeUnit.SECONDS)
        })
    }


    private fun extractConfigurationForMetrics(): String {

        // Need raw json as well as controller is not aware of all target types
        val configurationMap = fromJsonExtended(configReader.jsonConfig, Map::class.java)

        // Mapping with selected elements for target configuration, wee need to work with the raw JSON as
        // the ServiceControllerConfiguration type is not aware of specific target configuration types.
        val outputConfig = mutableMapOf<String, Any?>(CONFIG_AWS_VERSION to configuration.awsVersion)

        val metrics = (configurationMap[CONFIG_METRICS] as Map<*, *>).toMutableMap()

        val writerServer = (metrics[CONFIG_METRICS_WRITER_CONFIG] as MutableMap<*, *>?)?.filter { it.key == CONFIG_METRICS_METRICS_SERVER }
        if (!writerServer.isNullOrEmpty()) metrics[CONFIG_METRICS_WRITER_CONFIG] = writerServer

        // Get the target configuration
        outputConfig[CONFIG_METRICS] = metrics

        if (configurationMap.containsKey(Logger.CONF_LOG_WRITER)) {
            outputConfig[Logger.CONF_LOG_WRITER] = configurationMap[Logger.CONF_LOG_WRITER]
        }

        addMetricsCredentialClient(metrics, outputConfig)
        addExternalSecretsConfig(configuration, configReader, outputConfig)

        val s = gsonExtended().toJson(outputConfig)
        // replace placeholders that need to be replaced externally with normal placeholders
        return convertExternalPlaceholders(s)

    }

    private fun addMetricsCredentialClient(metrics: Map<*, *>, outputConfig: MutableMap<String, Any?>) {

        fun Map<*, *>.hasValueWithName(name: String): Any? {
            this.forEach { (key, value) ->
                if (key == name && value != null) return value
                if (value is Map<*, *>) {
                    val v = value.hasValueWithName(name)
                    if (v != null) return v
                }
            }
            return null
        }

        if (configuration.awsCredentialServiceClients.isNotEmpty()) {
            val clientCredentialClientID = metrics.hasValueWithName(CONFIG_CREDENTIAL_PROVIDER_CLIENT).toString()
            val clientCredentialClient = configuration.awsCredentialServiceClients[clientCredentialClientID]
            if (clientCredentialClient != null) {
                outputConfig[CONFIG_AWS_IOT_CREDENTIAL_PROVIDER_CLIENTS] = mapOf(clientCredentialClientID to clientCredentialClient.asConfigurationMap())
            }
        }
    }


    private suspend fun sendInitializationConfiguration(metricsConfigurationString: String) {

        val requestBuilder = InitializeMetricsWriterRequest.newBuilder()

        requestBuilder.metricsConfiguration = metricsConfigurationString

        val initResult = grpcClient!!.initializeMetricsWriter(requestBuilder.build())
        if (!initResult.initialized) {
            throw IpcException("Error initializing metrics writer on $serverAddress:$serverPort, ${initResult.error}")
        }
        initialized = true
    }


    // writes all data that is read from the internal channel to a flow that is required by the GRPC streaming client method
    private suspend fun writMetrics(grpcClient: MetricsWriterServiceGrpcKt.MetricsWriterServiceCoroutineStub?) {

        grpcClient?.writeMetrics(flow {
            for (request in metricsDataChannel) {
                emit(request)
            }
        })
    }


    // creates a GRPC client
    private fun getClient(): MetricsWriterServiceGrpcKt.MetricsWriterServiceCoroutineStub? {

        // if the client is set to null and the channel is still active close it first
        if ((ipcClient?.channel as? ManagedChannel) != null) {
            ((ipcClient?.channel as? ManagedChannel)?.shutdownNow()?.awaitTermination(15, TimeUnit.SECONDS))
        }

        ipcClient = IpcClientBuilder.createIpcClient(serverConfig, logger) { channel ->
            MetricsWriterServiceGrpcKt.MetricsWriterServiceCoroutineStub(channel)
        }

        return ipcClient

    }


    override suspend fun writeMetricsData(metricsData: MetricsData) {
        val request: MetricsDataMessage = metricsData.grpcMetricsDataMessage
        var done = false
        var ex: Throwable? = null
        var waitFor = START_WAIT_LEN
        var timeUsed = 0L

        while (!done && timeUsed < TIMOUT_TARGET_WRITE) {
            try {
                withTimeout(waitFor) {
                    metricsDataChannel.trySendBlocking(request)
                        .onSuccess {
                            done = true
                            ex = null
                        }
                        .onClosed { e -> throw IpcException("Error writing close to metrics channel, $e") }
                }
                    .onFailure { e -> throw IpcException("Error writing to metrics channel, $e") }
            } catch (e: TimeoutCancellationException) {
                timeUsed += waitFor
                waitFor = minOf(waitFor * 2, MAX_WAIT_LEN)
                ex = e
            }
        }

        if (ex != null) throw ex as Throwable
    }

    fun isInitialized(): Boolean {
        return initialized
    }


    /**
     * Closes the writer
     */
    /**
     * Closes the writer
     */
    override suspend fun close() {

        if (initialized) {
            writerWorker.cancel()
            (ipcClient?.channel as ManagedChannel).shutdownNow().awaitTermination(15, TimeUnit.SECONDS)
            metricsDataChannel.close()
        }
        writerWorker.cancel()

    }

    companion object {

        private val className = this::class.java.simpleName

        fun createIpcMetricsWriter(configReader: ConfigReader, logger: Logger): MetricsWriter? {

            val errLog = logger.getCtxErrorLog(className, "createIpcMetricsWriter")

            val serverConfig = configReader.getConfig<ConfigWithMetrics>().metrics?.writer?.metricsServer


            if (serverConfig == null) {
                errLog("No server configured for IPC Metrics writer")
                return null
            }

            return IpcMetricsWriter(configReader, serverConfig, logger)
        }

        const val WAIT_AFTER_ERROR = 10000L
        private const val MAX_WAIT_LEN = 1000L
        private const val START_WAIT_LEN = 50L
    }
}
