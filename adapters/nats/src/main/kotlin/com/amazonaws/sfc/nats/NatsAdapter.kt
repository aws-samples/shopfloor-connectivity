// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.nats

import com.amazonaws.sfc.channels.channelSubmitEventHandler
import com.amazonaws.sfc.channels.submit
import com.amazonaws.sfc.config.ChannelConfiguration.Companion.CHANNEL_SEPARATOR
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.crypto.SSLHelper
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.data.JsonHelper.Companion.gsonExtended
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_CONNECTIONS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_CONNECTION_ERRORS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
import com.amazonaws.sfc.nats.config.*
import com.amazonaws.sfc.nats.config.NatsAdapterConfiguration.Companion.CONFIG_MAX_RETAIN_PERIOD
import com.amazonaws.sfc.nats.config.NatsAdapterConfiguration.Companion.CONFIG_MAX_RETAIN_SIZE
import com.amazonaws.sfc.nats.config.NatsAdapterConfiguration.Companion.DEFAULT_RECEIVED_DATA_CHANNEL_SIZE
import com.amazonaws.sfc.nats.config.NatsAdapterConfiguration.Companion.DEFAULT_RECEIVED_DATA_CHANNEL_TIMEOUT
import com.amazonaws.sfc.nats.config.NatsServerConfiguration.Companion.CONFIG_CREDENTIALS_FILE
import com.amazonaws.sfc.nats.config.NatsServerConfiguration.Companion.CONFIG_NKEY_FILE
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.*
import com.amazonaws.sfc.util.MemoryMonitor.Companion.getUsedMemoryMB
import com.google.gson.JsonSyntaxException
import io.nats.client.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


class NatsAdapter(private val adapterID: String, private val configuration: NatsConfiguration, private val logger: Logger) : ProtocolAdapter {

    inner class Subscriber(val sourceID: String, val channelID: String, val channelConfiguration: NatsChannelConfiguration)

    private val className = this::class.java.simpleName
    private val adapterScope = buildScope("NATS Protocol Handler", Dispatchers.IO)

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    // mao indexed by the source has an instant which is set after a read error until when reading for a source is paused
    private var waitUntil = ConcurrentHashMap<String, Long>()

    // map indexed by the subscriptions has the source and channel data for each subscription
    private var subscriptionMap = ConcurrentHashMap<Subscription, Subscriber>()


    fun serverConfigurationForSource(sourceID: String): Pair<NatsServerConfiguration?, String> {

        val sourceConfiguration = configuration.sources[sourceID]
        return if (sourceConfiguration == null) {
            null to "NATS Source \"$sourceID\" does not exist, available sources are ${sources.keys}"
        } else {
            val protocolAdapterID = sourceConfiguration.protocolAdapterID

            val adapterConfiguration = configuration.natsProtocolAdapters[protocolAdapterID]
            if (adapterConfiguration == null) {
                null to "Adapter \"$protocolAdapterID\" for  Source \"$sourceID\" does not exist, available adapters are ${configuration.natsProtocolAdapters.keys}"
            } else {
                val adapterServer = adapterConfiguration.servers[sourceConfiguration.sourceAdapterServerID]
                if (adapterServer == null) {
                    null to "Server \"${sourceConfiguration.sourceAdapterServerID}\" Adapter \"$protocolAdapterID\" for  Source \"$sourceID\" does not exist, available servers are ${adapterConfiguration.servers}"
                }
                adapterServer to ""
            }
        }
    }

    private val connectionCache = LookupCacheHandler<String, Connection?, NatsServerConfiguration>(
        supplier = { sourceID ->
            val (serverConfiguration, error) = serverConfigurationForSource(sourceID)
            if (serverConfiguration != null)
                runBlocking {
                    val connection = setupSourceConnection(sourceID, serverConfiguration)
                    if (connection != null) {
                        setupSourceSubscriptions(connection, sourceID)
                    }
                    connection
                } else {
                val log = logger.getCtxLoggers(className, "connectionCacheSupplier")
                log.error("Configuration: $error")
                null
            }
        }
    )

    private fun setupSourceSubscriptions(connection: Connection, sourceID: String) {
        val dispatcher = connection.createDispatcher()
        sources[sourceID]?.channels?.forEach { (channelID, channelConfig) ->
            channelConfig.subjects.forEach { subject ->

                val subscription = dispatcher.subscribe(subject) { message ->
                    runBlocking {
                        val log = logger.getCtxLoggers(className, "subscriptionHandler")
                        receivedData.submit(
                            message,
                            adapterConfiguration?.receivedDataChannelTimeout ?: DEFAULT_RECEIVED_DATA_CHANNEL_TIMEOUT.toDuration(DurationUnit.MILLISECONDS)
                        ) { event ->
                            channelSubmitEventHandler(
                                event,
                                channelName = "$className:receivedData",
                                tuningChannelSizeName = NatsAdapterConfiguration.CONFIG_RECEIVED_DATA_CHANNEL_SIZE,
                                currentChannelSize = adapterConfiguration?.receivedDataChannelSize ?: 0,
                                tuningChannelTimeoutName = NatsAdapterConfiguration.CONFIG_RECEIVED_DATA_CHANNEL_TIMEOUT,
                                log = log
                            )
                        }
                    }


                }
                subscriptionMap[subscription] = Subscriber(sourceID, channelID, channelConfig)
            }
        }
    }


    private val adapterMetricDimensions = mapOf(MetricsCollector.METRICS_DIMENSION_TYPE to className)

    private val adapterConfiguration = configuration.natsProtocolAdapters[adapterID]

    private val sources
        get() = configuration.sources.filter { it.value.protocolAdapterID in configuration.natsProtocolAdapters.keys }

    override val metricsCollector: MetricsCollector? by lazy {
        val metricsConfigurations = configuration.natsProtocolAdapters.map { it.key to (it.value.metrics ?: MetricsSourceConfiguration()) }.toMap()
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
                    logger.getCtxErrorLogEx(className, "collectMetricsFromLogger")("Error collecting metrics from logger", e)
                }
            }
        } else null


    // Store received data
    private val channelValueStores: Map<String, SourceDataStore<ChannelReadValue>> = sources.keys.associate { sourceID ->
        sourceID to if (adapterConfiguration?.readMode == ReadMode.KEEP_LAST)
            SourceDataValuesStore()
        else
            SourceDataMultiValuesStore<ChannelReadValue>(adapterConfiguration?.maxRetainSize ?: 0, adapterConfiguration?.maxRetainPeriod ?: 0)
            { channel, duration, size, full ->
                if (full) {
                    val ctxWarningLog = logger.getCtxWarningLog(className, "sourceDataStores")
                    if (size != null)
                        ctxWarningLog("Source \"$sourceID\", channel \"$channel\" number of kept values reached maximum of $size values, oldest values are dropped, consider a larger $CONFIG_MAX_RETAIN_SIZE for adapter or a faster reading interval.")
                    else
                        ctxWarningLog("Source \"$sourceID\", channel \"$channel\" expired items older than configured $CONFIG_MAX_RETAIN_PERIOD $duration are being dropped, consider a larger a faster reading interval.")
                } else {
                    val ctxInfoLog = logger.getCtxInfoLog(className, "sourceDataStores")
                    if (size != null)
                        ctxInfoLog("Source \"$sourceID\", channel \"$channel\" number of kept values is now again below maximum of $size values.")
                    else
                        ctxInfoLog("Source \"$sourceID\", channel \"$channel\" no more expired values older than $duration are being dropped.")
                }
            }
    }


    // channel to send data changes to the coroutine that is handling these changes
    private val receivedData = Channel<Message>(adapterConfiguration?.receivedDataChannelSize ?: DEFAULT_RECEIVED_DATA_CHANNEL_SIZE)

    private val receivedDataWorker = adapterScope.launch(context = Dispatchers.Default, name = "Receive Data Handler") {
        receivedDataTask(receivedData, this)
    }

    private suspend fun receivedDataTask(channel: Channel<Message>, scope: CoroutineScope) {
        while (scope.isActive)
            try {
                val data = channel.receive()
                handleDataReceived(data)
            } catch (e: Exception) {
                if (!e.isJobCancellationException)
                    logger.getCtxErrorLogEx(scope::class.java.simpleName, "changedDataWorker")("Error processing received data", e)
            }
    }

    /**
     * Reads a values from a source
     * @param sourceID String Source ID
     * @param channels List<String>? Channels to read values for, if null then all values for the source are read
     * @return SourceReadResult
     */
    override suspend fun read(sourceID: String, channels: List<String>?): SourceReadResult {

        val log = logger.getCtxLoggers(className, "read")

        if (waitUntil.getOrDefault(sourceID, 0L) > DateTime.systemDateTime().toEpochMilli()) {
            log.trace("Read from source \"$sourceID\" was paused")
        }

        val (serverConfiguration, configError) = serverConfigurationForSource(sourceID)

        if (serverConfiguration == null) {
            return SourceReadError(configError)
        }

        val dimensions = mapOf(METRICS_DIMENSION_SOURCE to "$adapterID:$sourceID") + adapterMetricDimensions
        val connection = connectionCache.getItemAsync(sourceID, serverConfiguration).await()

        metricsCollector?.put(adapterID, if (connection != null) METRICS_CONNECTIONS else METRICS_CONNECTION_ERRORS, 1.0, MetricUnits.COUNT, dimensions)

        if (connection == null) {
            metricsCollector?.put(adapterID, METRICS_CONNECTION_ERRORS, 1.0, MetricUnits.COUNT, dimensions)

            if (serverConfiguration.waitAfterConnectError.inWholeMilliseconds > 0) {
                waitUntil[sourceID] = DateTime.systemDateTime().toEpochMilli() + serverConfiguration.waitAfterConnectError.inWholeMilliseconds
                log.info("Pause reading from source until ${Instant.ofEpochMilli(waitUntil[sourceID]!!)} ")
            }
            return SourceReadError("Can not connect to server for source \"$sourceID\" at ${serverConfiguration.url}", DateTime.systemDateTime())
        }

        // Get the store where received values for this source are stored
        val store = channelValueStores[sourceID]

        val start = DateTime.systemDateTime().toEpochMilli()

        // Get the values and return result
        val data = (store?.read(channels) ?: emptyList()).associate {
            it.first to it.second
        }

        val readDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()

        val protocolAdapterID = sources[sourceID]?.protocolAdapterID
        protocolAdapterID?.let { createMetrics(it, dimensions, readDurationInMillis, data.size) }

        val d = data.map {
            it.key to ChannelReadValue(it.value)
        }.toMap()

        return SourceReadSuccess(d, DateTime.systemDateTime())
    }


    private fun createMetrics(
        protocolAdapterID: String,
        metricDimensions: MetricDimensions?,
        readDurationInMillis: Double,
        values: Int
    ) {
        metricsCollector?.put(
            protocolAdapterID,
            metricsCollector?.buildValueDataPoint(
                protocolAdapterID,
                MetricsCollector.METRICS_MEMORY,
                getUsedMemoryMB().toDouble(),
                MetricUnits.MEGABYTES,
                metricDimensions
            ),
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
                values.toDouble(),
                MetricUnits.COUNT,
                metricDimensions
            ),
            metricsCollector?.buildValueDataPoint(protocolAdapterID, MetricsCollector.METRICS_READ_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions)
        )
    }

    /**
     * Stops the adapter
     * @param timeout Duration Timeout period to wait for adapter to stop
     */
    override suspend fun stop(timeout: Duration) {
        adapterScope.cancel()


        withTimeoutOrNull(timeout) {
            subscriptionMap.keys.forEach { subscription ->
                try {
                    subscription.unsubscribe()
                } catch (_: Exception) {
                }
            }

            connectionCache.items.forEach { connection ->
                try {
                    connection?.close()
                } catch (_: Exception) {
                }
            }
            // clear data stores
            channelValueStores.forEach {
                it.value.clear()
            }
        }
    }


    private suspend fun setupSourceConnection(sourceID: String, serverConfig: NatsServerConfiguration): Connection? {

        val log = logger.getCtxLoggers(className, "getClient")

        val connectionName = "SFC-NATS-$sourceID-${getHostName()}-${UUID.randomUUID()}"

        var _natsConnection: Connection? = null

        var retries = 0
        while (_natsConnection == null && adapterScope.isActive && retries < serverConfig.connectRetries) {
            try {
                _natsConnection = Nats.connect(
                    {

                        buildConnectionOptions(connectionName, serverConfig)
                    }()
                )
                log.info("Connected to NATS server at ${serverConfig.url}, connection name id $connectionName")
            } catch (e: Exception) {
                logger.getCtxErrorLog(className, "natsConnection")("Error creating NATS connection, ${e.message}")
            }
            if (_natsConnection == null) {
                retries += 1
                if (retries < serverConfig.connectRetries) {
                    log.info("Waiting ${serverConfig.waitAfterConnectError} before trying to create NATS connection, number of retries is $retries")
                    delay(serverConfig.waitAfterConnectError)
                }
            }
        }
        return _natsConnection
    }

    private fun buildConnectionOptions(connectionName: String,
                                       serverConfig: NatsServerConfiguration): Options? {

        val log = logger.getCtxLoggers(className, "buildConnectionOptions")

        val builder = Options.Builder()
            .connectionName(connectionName)
            .server(serverConfig.url)

        if (serverConfig.username != null && serverConfig.password != null) {
            log.info("Using username/password authentication")
            builder.userInfo(serverConfig.username!!.toCharArray(), serverConfig.password!!.toCharArray())
        }

        if (serverConfig.token != null) {
            log.info("Using token authentication")
            builder.token(serverConfig.token!!.toCharArray())
        }

        if (serverConfig.nkeyFile != null) {
            log.info("Using NKey authentication, using $CONFIG_NKEY_FILE \"${serverConfig.nkeyFile}")
            builder.authHandler(NKeyAuthHandler(serverConfig.nkeyFile!!, logger))
        }

        if (serverConfig.credentialsFile != null) {
            log.info("Using JWT authentication, using $CONFIG_CREDENTIALS_FILE \"${serverConfig.credentialsFile}")
            builder.authHandler(Nats.credentials(serverConfig.credentialsFile!!))
        }

        if (serverConfig.tlsConfiguration != null) {
            log.info("Using SSL/TLS encryption")
            builder.sslContext(SSLHelper(serverConfig.tlsConfiguration!!, logger).sslContext)
            builder.secure()
        }

        return builder.build()
    }


    private fun handleDataReceived(message: Message) {

        val log = logger.getCtxLoggers(className, "handleDataReceived")

        val subscriber = subscriptionMap[message.subscription]
        if (subscriber == null) {
            log.error("No channel found for message from subject ${message.subscription.subject}")
            return
        }

        // Build the channel name for the received data (configured name or a mapped topic name)
        val name = subscriber.channelConfiguration.mapSubjectName(message.subject)
        // Store the value in the buffer for the source
        if (name != null) {
            val value = dataValue(subscriber, message)
            if (value != null) {
                val store = channelValueStores[subscriber.sourceID]
                store?.add("${subscriber.channelID}$CHANNEL_SEPARATOR$name", value)
            }
        } else {
            // Topic name could not be mapped to a channel name
            val trace = logger.getCtxWarningLog(className, "handleDataReceived")
            trace("Subject name \"$message.subject\" does not match with any of the Subject Mappings for \"${subscriber.sourceID}\", Channel \"${subscriber.channelID}\", data wil not be included")
        }

    }


    private fun dataValue(subscriber: Subscriber, message: Message): ChannelReadValue? {

        //TODO TIMESTAMP FROM MESSAGE????
        val timestamp = DateTime.systemDateTime()

        // Decode json to get native value
        return if (subscriber.channelConfiguration.json) {
            try {
                var value = gson.fromJson(String(message.data), Any::class.java)
                if (subscriber.channelConfiguration.selector != null) {
                    value = applyChannelValueSelector(subscriber, value)
                }
                ChannelReadValue(value, timestamp)
            } catch (e: JsonSyntaxException) {
                val log = logger.getCtxErrorLog("dataValue")
                log("Source \"${subscriber.sourceID}\", Channel \"${subscriber.channelID}\", Value \"$message\" is not valid JSON, $e")
                null
            }
        } else {
            // No json, store value as a string
            ChannelReadValue(message, timestamp)
        }

    }

    /**
     * Applies selector for node to select fields from structured types
     * @param receivedData ReceivedData Received data
     * @param value Any? The received value
     * @return Any?
     */
    private fun applyChannelValueSelector(subscriber: Subscriber, value: Any?): Any? {

        val sourceID = subscriber.sourceID
        val channelID = subscriber.channelID
        val channel = subscriber.channelConfiguration


        val selector = channel.selector
        return if (selector != null) {
            val log = logger.getCtxLoggers(className, "applyChannelValueSelector")
            try {
                val selected = selector.search(value)

                if (selected == null) {

                    log.warning("Applying selector \"${channel.selectorStr}\" for source \"$sourceID\", node \"$channelID\" on value \"$value\" returns null")
                } else {
                    log.trace("Applying selector \"${channel.selectorStr}\" for source \"${subscriber.sourceID}\", node \"$channelID\" on value \"$value\" returns \"$selected\"")
                }
                selected
            } catch (e: java.lang.Exception) {
                log.errorEx("Error applying selector \"${channel.selectorStr}\" for source \"$sourceID\", node \"$channelID\", ${e.message}", e)
            }
        } else {
            // No selector, return value
            value
        }
    }


    companion object {

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParams: Any) =
            newInstance(createParams[0] as ConfigReader, createParams[1] as String, createParams[2] as String, createParams[3] as Logger)


        private val createInstanceMutex = Mutex()


        @JvmStatic
        fun newInstance(configReader: ConfigReader, scheduleName: String, adapterID: String, logger: Logger): SourceValuesReader? {


            runBlocking {
                createInstanceMutex.withLock {
                    if (adapter == null) {
                        adapter = createNatsAdapter(adapterID, configReader, logger)
                    }
                }
            }

            val config = configReader.getConfig<NatsConfiguration>()
            val schedule = config.schedules.firstOrNull { it.name == scheduleName }
            val sourcesForAdapter = schedule?.sources?.filter { (config.sources[it.key]?.protocolAdapterID ?: "") == adapterID } ?: return null

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


        fun createNatsAdapter(adapterID: String, configReader: ConfigReader, logger: Logger): ProtocolAdapter {

            val config: NatsConfiguration = try {
                configReader.getConfig()
            } catch (e: Exception) {
                throw TargetException("Error loading configuration: ${e.message}")
            }
            return NatsAdapter(adapterID, config, logger)
        }

        private val gson by lazy { gsonExtended() }


        private val ADAPTER_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
        )
    }

}
