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

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_AWS_IOT_CREDENTIAL_PROVIDER_CLIENTS
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANGE_FILTER
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CHANGE_FILTERS
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_DESCRIPTION
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_ELEMENT_NAMES
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_LOG_LEVEL
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_META_DATA
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_NAME
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_PROTOCOL_ADAPTERS
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_PROTOCOL_ADAPTER_TYPES
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_PROTOCOL_SERVERS
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_SECRETS_MANGER
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_SOURCES
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_TARGETS
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_TARGET_SERVERS
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_TARGET_TYPES
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_TRANSFORMATIONS
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_VALUE_FILTER
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_VALUE_FILTERS
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_VERSION
import com.amazonaws.sfc.config.BaseSourceConfiguration.Companion.CONFIG_SOURCE_PROTOCOL_ADAPTER
import com.amazonaws.sfc.config.ChannelConfiguration.Companion.CONFIG_TRANSFORMATION
import com.amazonaws.sfc.config.ProtocolAdapterConfiguration.Companion.CONFIG_PROTOCOL_ADAPTER_SERVER
import com.amazonaws.sfc.config.ProtocolAdapterConfiguration.Companion.CONFIG_PROTOCOL_ADAPTER_TYPE
import com.amazonaws.sfc.data.JsonHelper
import com.amazonaws.sfc.data.ReadResultConsumer
import com.amazonaws.sfc.data.SourceValuesReader
import com.amazonaws.sfc.ipc.extensions.GrpcSourceValueAsNativeExt.asReadResult
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.MetricsConfiguration.Companion.CONFIG_METRICS
import com.amazonaws.sfc.metrics.MetricsConfiguration.Companion.CONFIG_METRICS_INTERVAL
import com.amazonaws.sfc.metrics.MetricsConfiguration.Companion.CONFIG_METRICS_NAMESPACE
import com.amazonaws.sfc.metrics.MetricsConfiguration.Companion.CONFIG_METRICS_WRITER_CONFIG
import com.amazonaws.sfc.metrics.MetricsProvider
import com.amazonaws.sfc.service.ConfigFileProvider.Companion.CONFIG_CUSTOM_CONFIG_PROVIDER
import com.amazonaws.sfc.service.ServerConnectionType
import com.amazonaws.sfc.service.addExternalSecretsConfig
import com.amazonaws.sfc.util.launch
import io.grpc.StatusException
import kotlinx.coroutines.*
import kotlin.time.Duration

/**
 * Reads input from IPC service that implements the input protocol used by the instance of the SFC controller
 */
class IpcSourceReader(
    private val adapterID: String,
    configReader: ConfigReader,
    serverConfig: ServerConfiguration,
    private val sources: Map<String, ArrayList<String>>,
    private val adapterType: String,
    private val interval: Duration,
    logger: Logger
) : IpcClientBase<IpcSourceReadClient>(
    configReader = configReader,
    serverConfig = serverConfig,
    logger = logger,
    createClient = { managedChannel -> IpcSourceReadClient(managedChannel, configReader.usedSecrets) }), SourceValuesReader {

    private var _initialized = false
    override val isInitialized: Boolean
        get() = _initialized

    private var client: IpcSourceReadClient? = null

    /**
     * Reads the data from the IPC service and calls the reader to process the data. Reading will stop if this function returns false.
     * @param consumer Function1<ReadResult, Boolean>
     */
    override suspend fun read(consumer: ReadResultConsumer) = coroutineScope {

        var reader: Job? = null

        reader = launch("IPC Source Reader") {

            val log = logger.getCtxLoggers(IpcSourceReader::class.java.simpleName, "reader")

            // read loop, remote IPC service is streaming data
            while (isActive) {

                try {

                    log.info("Initializing IPC source adapter service on ${serverConfig.addressStr}")

                    client = getIpcClient()

                    try {
                        val adapterConfiguration = getAdapterConfiguration(configReader)
                        _initialized = client?.initializeAsync(adapterConfiguration, serverConfig, logger)?.await() == true

                    } catch (e: Exception) {
                        val errorMessage = if (e is StatusException) "${e.cause ?: e.message}" else e.message
                        log.error("Error initializing IPC source service for ${serverConfig.addressStr}, $errorMessage")
                        if (serverConfig.serverConnectionType == ServerConnectionType.PlainText && errorMessage.toString().contains("unknown reason"))
                            log.error("${ServerConnectionType.PlainText} connection type is configured for client, check if server requires ServerSide or Mutual TLS")
                        delay(WAIT_AFTER_ERROR)
                        continue
                    }


                    // read the values from the client
                    client?.readValues(sources, interval)?.collect { r: ReadValuesReply ->
                        // call handler to process the data
                        if (!consumer(r.asReadResult)) {
                            // if handler returns false stop reading from client
                            reader?.cancel()
                        }
                    }

                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        if (client?.lastError == null) {
                            client?.lastError = e
                        }
                        if (e.message?.contains("shutdownNow") == true) {
                            log.info("Source service is shutting down")
                        } else {
                            var s = "Error communicating with source connector service on ${serverConfig.addressStr}, "
                            s += if (e is StatusException) "${e.cause ?: e.message}" else e.message
                            log.error(s)
                        }
                        resetIpcClient()
                        delay(WAIT_AFTER_ERROR)
                    }
                } finally {
                    resetIpcClient()
                }
            }
        }
        reader.join()
    }


    override val metricsProvider: MetricsProvider? by lazy {

        val config = configReader.getConfig<ServiceConfiguration>()
        if (config.metrics != null)
            IpcMetricsProvider(configReader = configReader,
                serverConfig,
                isIpcServiceInitialized = { this.isInitialized },
                logger = logger) { m -> IpcSourceReadClient(m, configReader.usedSecrets) }
        else
            null
    }


    companion object {


        /**
         * Creates an IPC reader from its configuration
         * @param configReader ControllerServiceConfig IPC server configuration
         * @param logger Logger Logger for output
         * @return IpcReader? Created reader, null if service configuration is invalid or schedule does not exist
         */
        fun createIpcReader(
            configReader: ConfigReader,
            config: ControllerServiceConfiguration,
            adapterID: String,
            schedule: ScheduleConfiguration,
            logger: Logger): IpcSourceReader? {

            // Obtain IPC server config
            val protocolAdapterConfiguration = config.protocolAdapters[adapterID] ?: return null
            val adapterType = protocolAdapterConfiguration.protocolAdapterType ?: return null
            val serverID = protocolAdapterConfiguration.protocolAdapterServer ?: return null
            val serverConfig = config.protocolAdapterServers[serverID] ?: return null


            val sources = schedule.sources.filter {
                val sourceConfig = config.sources[it.key]
                val adapterConfig = config.protocolAdapters[sourceConfig?.protocolAdapterID]
                adapterConfig?.protocolAdapterType == adapterType
            }

            return IpcSourceReader(adapterID, configReader, serverConfig, sources, adapterType, schedule.interval, logger)
        }

        const val WAIT_AFTER_ERROR = 10000L
    }

    // Creates a subset of the configuration with only the required sections to initialize the protocol adapter server
    private fun getAdapterConfiguration(configReader: ConfigReader): String {

        val configRaw = gson.fromJson(configReader.jsonConfig, Any::class.java) as Map<*, *>

        //  val adaptersForAdapterType = adaptersForAdapterType(configRaw)
        val adapter = ((configRaw[CONFIG_PROTOCOL_ADAPTERS] as Map<*, *>)[adapterID] as Map<*, *>)


        val adapterSources = scheduleSourcesForAdapter(configRaw)
        val adapterConfig = mutableMapOf<String, Any?>(
            CONFIG_SOURCES to adapterSources,
            CONFIG_PROTOCOL_ADAPTERS to mapOf(adapterID to adapter)
        )

        addAdapterServer(adapter, configRaw, adapterConfig)

        addMetrics(configRaw, adapterConfig)

        configRaw.keys.filter { configSection -> configSection !in EXCLUDED_SECTIONS_FOR_ADAPTER_CONFIGURATION && !configSection.toString().startsWith("#") }
            .forEach { section ->
                adapterConfig[section as String] = configRaw[section] as Any
            }


        adapterConfig["SecretValues"] = configReader.usedSecrets

        addExternalSecretsConfig(configuration, configReader, adapterConfig)
        return ConfigReader.convertExternalPlaceholders(gson.toJson(adapterConfig))
    }

    private fun addMetrics(configRaw: Map<*, *>, adapterConfig: MutableMap<String, Any?>) {
        val metrics = configRaw[CONFIG_METRICS]
        if (metrics != null) {
            val m = (metrics as Map<*, *>).filter {
                it.key in listOf(CONFIG_METRICS_INTERVAL, CONFIG_METRICS_WRITER_CONFIG, CONFIG_METRICS_NAMESPACE)
            }
            if (m.isNotEmpty()) {
                adapterConfig[CONFIG_METRICS] = m
            }
        }
    }

    private fun addAdapterServer(adapter: Map<*, *>,
                                 configRaw: Map<*, *>,
                                 adapterConfig: MutableMap<String, Any?>) {
        val adapterServerID = adapter[CONFIG_PROTOCOL_ADAPTER_SERVER]
        val adapterServer = (configRaw[CONFIG_PROTOCOL_SERVERS] as Map<*, *>).filter { it.key == adapterServerID }
        if (adapterServer.isNotEmpty()) {
            adapterConfig[CONFIG_PROTOCOL_SERVERS] = adapterServer
        }
    }

    private fun scheduleSourcesForAdapter(configRaw: Map<*, *>): Map<Any?, Any?> {

        val sourceToServerMap = sourceToServerMap(configRaw)
        val usedServers = sources.keys.map { sourceID ->
            sourceToServerMap[sourceID]
        }

        val sources = (configRaw[CONFIG_SOURCES] as Map<*, *>).filter { source ->
            val serverForSource = sourceToServerMap[source.key]
            usedServers.contains(serverForSource)
        }

        return removeChannelDataNotUsedByAdapter(sources)

    }

    private fun removeChannelDataNotUsedByAdapter(sources: Map<Any?, Any?>): Map<Any?, Any?> {
        sources.forEach { source ->
            val channels = (source.value as Map<*, *>)[BaseConfiguration.CONFIG_CHANNELS] as Map<*, *>
            channels.map { ch ->
                val channel = ch.value as MutableMap<*, *>
                channel.remove(CONFIG_NAME)
                channel.remove(CONFIG_DESCRIPTION)
                channel.remove(CONFIG_TRANSFORMATION)
                channel.remove(CONFIG_META_DATA)
                channel.remove(CONFIG_CHANGE_FILTER)
                channel.remove(CONFIG_VALUE_FILTER)
            }
        }
        return sources
    }

    private fun sourceToServerMap(configRaw: Map<*, *>): Map<String, String> {

        val allSources = configRaw[CONFIG_SOURCES] as Map<*, *>
        val adapterServerMap = adaptersToServerMap(configRaw)

        return allSources.map {
            val source = it.value as Map<*, *>
            val adapterID = source[CONFIG_SOURCE_PROTOCOL_ADAPTER]
            val serverID = adapterServerMap[adapterID] ?: return@map null
            it.key.toString() to serverID
        }.filterNotNull().toMap()
    }

    private fun adaptersToServerMap(configRaw: Map<*, *>) = allAdaptersForType(configRaw).map {
        val adapter = it.value as Map<*, *>
        val adapterID = it.key as String
        val serverID = adapter[CONFIG_PROTOCOL_ADAPTER_SERVER] as String
        adapterID to serverID
    }.toMap()

    private fun allAdaptersForType(configRaw: Map<*, *>) = (configRaw[CONFIG_PROTOCOL_ADAPTERS] as Map<*, *>)
        .filter {
            val adapter = (it.value as Map<*, *>)
            (adapter[CONFIG_PROTOCOL_ADAPTER_TYPE] == adapterType)
        }

    private val gson = JsonHelper.gsonExtended()

    // Sections excluded from configuration when building the configuration for the adapter
    private val EXCLUDED_SECTIONS_FOR_ADAPTER_CONFIGURATION =
        listOf(
            CONFIG_CUSTOM_CONFIG_PROVIDER,
            CONFIG_ELEMENT_NAMES,
            CONFIG_LOG_LEVEL,
            CONFIG_META_DATA,
            CONFIG_NAME,
            CONFIG_SOURCES,
            CONFIG_TARGETS,
            CONFIG_TARGET_SERVERS,
            CONFIG_TARGET_TYPES,
            CONFIG_TRANSFORMATIONS,
            CONFIG_VERSION,
            CONFIG_VALUE_FILTERS,
            CONFIG_CHANGE_FILTERS,
            CONFIG_PROTOCOL_ADAPTERS,
            CONFIG_PROTOCOL_ADAPTER_TYPES,
            CONFIG_PROTOCOL_SERVERS,
            CONFIG_METRICS,
            CONFIG_SECRETS_MANGER,
            CONFIG_AWS_IOT_CREDENTIAL_PROVIDER_CLIENTS
        )

}