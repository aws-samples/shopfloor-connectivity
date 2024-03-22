// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.mqtt

import com.amazonaws.sfc.channels.channelSubmitEventHandler
import com.amazonaws.sfc.channels.submit
import com.amazonaws.sfc.config.ChannelConfiguration.Companion.CHANNEL_SEPARATOR
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.data.JsonHelper.Companion.gsonExtended
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_CONNECTIONS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_CONNECTION_ERRORS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
import com.amazonaws.sfc.mqtt.config.MqttAdapterConfiguration
import com.amazonaws.sfc.mqtt.config.MqttAdapterConfiguration.Companion.DEFAULT_RECEIVED_DATA_CHANNEL_SIZE
import com.amazonaws.sfc.mqtt.config.MqttAdapterConfiguration.Companion.DEFAULT_RECEIVED_DATA_CHANNEL_TIMEOUT
import com.amazonaws.sfc.mqtt.config.MqttChannelConfiguration
import com.amazonaws.sfc.mqtt.config.MqttConfiguration
import com.amazonaws.sfc.mqtt.config.MqttSourceConfiguration
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.MemoryMonitor.Companion.getUsedMemoryMB
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.launch
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.time.Instant
import java.util.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


class MqttAdapter(private val adapterID: String, private val configuration: MqttConfiguration, private val logger: Logger) : ProtocolAdapter {

    /**
     * Internal class to pass information for received data
     * @property sourceID String Source
     * @property channelID String Channel, this is the channel ID
     * @property channel MqttChannelConfiguration MQtt channel configuration
     * @property topic String Topic the data was received from
     * @property message String The received message
     * @property timestamp Instant Time when message was received
     */
    inner class ReceivedData(
        val sourceID: String,
        val channelID: String,
        val channel: MqttChannelConfiguration,
        val topic: String,
        val message: String,
        val timestamp: Instant = DateTime.systemDateTime()
    )

    private val className = this::class.java.simpleName

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val adapterMetricDimensions = mapOf(MetricsCollector.METRICS_DIMENSION_TYPE to className)

    private val adapterConfiguration = configuration.mqttProtocolAdapters[adapterID]

    // clients for each broker used by the sources
    private val sourceClients = mutableMapOf<String, MqttClient?>()

    private val sources
        get() = configuration.sources.filter { it.value.protocolAdapterID in configuration.mqttProtocolAdapters.keys }

    override val metricsCollector: MetricsCollector? by lazy {
        val metricsConfigurations = configuration.mqttProtocolAdapters.map { it.key to (it.value.metrics ?: MetricsSourceConfiguration()) }.toMap()
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


    private val scope = buildScope("MQTT Protocol Handler")

    // Store received data
    private val sourceDataStores = sources.keys.map { sourceID ->
        sourceID to SourceDataValuesStore<ChannelReadValue>()
    }.toMap()


    // channel to send data changes to the coroutine that is handling these changes
    private val receivedData = Channel<ReceivedData>(adapterConfiguration?.receivedDataChannelSize ?: DEFAULT_RECEIVED_DATA_CHANNEL_SIZE)

    // co-routine processing the received data

    private val changedDataWorker = scope.launch(context = Dispatchers.Default, name = "Receive Data Handler") {
        changedDataTask(receivedData, this)
    }

    private suspend fun changedDataTask(channel: Channel<ReceivedData>, scope: CoroutineScope) {
        while (scope.isActive)
            try {
                val data = channel.receive()
                handleDataReceived(data)
            } catch (e: Exception) {
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

        // Retrieve the client to set it up at first call
        val sourceConfiguration = sources[sourceID] ?: return SourceReadError("Source \"$sourceID\" does not exist, available sources are ${sources.keys}")
        val protocolAdapterID = sourceConfiguration.protocolAdapterID
        val dimensions = mapOf(METRICS_DIMENSION_SOURCE to "$adapterID:$sourceID") + adapterMetricDimensions
        val client = getClientForSource(sourceID, protocolAdapterID, metricsCollector, dimensions)

        // No client or could not connect
        if (client == null) {
            val adapterConfiguration = configuration.mqttProtocolAdapters[protocolAdapterID]
                ?: return SourceReadError("Adapter \"$protocolAdapterID\" for  Source \"$sourceID\" does not exist, available adapters are ${configuration.mqttProtocolAdapters.keys}")
            val brokerConfiguration = adapterConfiguration.brokers[sourceConfiguration.sourceAdapterbrokerID]
                ?: return SourceReadError("Broker \"${sourceConfiguration.sourceAdapterbrokerID}\" Adapter \"$protocolAdapterID\" for  Source \"$sourceID\" does not exist, available brokers are ${adapterConfiguration.brokers}")

            // Wait for next read and return read error
            val error = SourceReadError("Can not connect to broker at ${brokerConfiguration.endPoint}", DateTime.systemDateTime())
            delay(brokerConfiguration.waitAfterConnectError.inWholeMilliseconds)
            return error
        }

        // Get the store where received values for this source are stored
        val store = sourceDataStores[sourceID]

        val start = DateTime.systemDateTime().toEpochMilli()

        // Get the values and return result
        val f: List<Pair<String, ChannelReadValue>> = store?.read(channels) ?: emptyList()
        val data = f.toMap()

        val readDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()


        createMetrics(protocolAdapterID, dimensions, readDurationInMillis, data)
        return SourceReadSuccess(data, DateTime.systemDateTime())
    }

    private suspend fun createMetrics(
        protocolAdapterID: String,
        metricDimensions: MetricDimensions?,
        readDurationInMillis: Double,
        values: Map<String, ChannelReadValue>
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
                values.size.toDouble(),
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
        changedDataWorker.cancel()

        withTimeoutOrNull(timeout) {
            try {
                // for each source
                sources.forEach { (sourceID, source) ->
                    // get the client
                    val client = sourceClients[sourceID]
                    // unsubscribe from all topics
                    if (client != null) {
                        source.channels.values.forEach { channel ->
                            channel.topics.forEach { topic ->
                                client.unsubscribe(topic)
                            }
                        }
                        client.disconnect()
                    }
                }
            } catch (e: Exception) {
                val log = logger.getCtxErrorLogEx(className, "stop")
                log("Error unsubscribing or disconnecting MQTT client", e)
            }

            // clear data stores
            sourceDataStores.forEach {
                it.value.clear()
            }
        }
    }

    /**
     * Get the client for a source
     * @param sourceID String
     * @return MqttClient?
     */
    private suspend fun getClientForSource(
        sourceID: String,
        adapterID: String,
        metrics: MetricsCollector?,
        metricDimensions: Map<String, String>
    ): MqttClient? {

        var client = sourceClients[sourceID]
        if ((client != null)) {
            return client
        }

        client = createMqttClient(sourceID)
        if (client != null) {
            sourceClients[sourceID] = client
        }

        metrics?.put(adapterID, if (client != null) METRICS_CONNECTIONS else METRICS_CONNECTION_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions)
        return client
    }

    /**
     * Creates a Mqtt client for a source from its configuration, connect to it and subscribes to the topics for each channel of the source
     * @param sourceID String
     * @return MqttClient?
     */
    private fun createMqttClient(sourceID: String): MqttClient? {

        val log = logger.getCtxLoggers(className, "createServerClient")

        val sourceConfiguration = sources[sourceID]
        if (sourceConfiguration == null) {
            log.error("Source \"$sourceID\" does not exist, available sources are ${sources.keys}")
            return null
        }
        val adapterConfiguration = configuration.mqttProtocolAdapters[sourceConfiguration.protocolAdapterID]
        if (adapterConfiguration == null) {
            log.error("Adapter \"${sourceConfiguration.protocolAdapterID}\" for  Source \"$sourceID\" does not exist, available adapters are ${configuration.mqttProtocolAdapters.keys}")
            return null
        }
        val brokerConfiguration = adapterConfiguration.brokers[sourceConfiguration.sourceAdapterbrokerID]
        if (brokerConfiguration == null) {
            log.error("Broker \"${sourceConfiguration.sourceAdapterbrokerID}\" Adapter \"${sourceConfiguration.protocolAdapterID}\" for  Source \"$sourceID\" does not exist, available brokers are ${adapterConfiguration.brokers}")
            return null
        }

        log.info("Creating client for source \"$sourceID\" at  ${brokerConfiguration.endPoint}")

        // Create the client
        val id = "${this::class.simpleName}-${sourceConfiguration.protocolAdapterID}-${UUID.randomUUID()}"
        val client = try {
            MqttHelper(brokerConfiguration, logger).buildClient(id, MemoryPersistence())
        } catch (e: Exception) {
            log.errorEx("Error connecting to ${brokerConfiguration.endPoint} for source \"$sourceID\"", e)
            return null
        }

        return try {
            log.info("Connected to broker for source \"$sourceID\" at ${brokerConfiguration.endPoint} with session id \"$id\"")
            // Setup subscriptions
            setupSourceSubscriptions(sourceID, sourceConfiguration, client)
            client
        } catch (e: Exception) {
            log.errorEx("Error setting up subscription ${brokerConfiguration.endPoint} for source \"$sourceID\"", e)
            null
        }
    }

    /**
     * Sets up the subscriptions for all topics for all channels of a source
     * @param sourceID String Source ID
     * @param source MqttSourceConfiguration The Mqtt source
     * @param client MqttClient The connected client
     */
    private fun setupSourceSubscriptions(sourceID: String, source: MqttSourceConfiguration, client: MqttClient) {
        // Channels for the source have 1 or more topics they can subscribe to
        source.channels.forEach { (channelID, channel) ->
            channel.topics.forEach { t ->
                client.subscribe(t) { topic, message: MqttMessage ->
                    // The received data is sent to a buffered channel for further processing
                    subscriptionHandler(receivedData, sourceID, channelID, channel, topic, message)
                }
            }
        }
    }

    private fun subscriptionHandler(
        receivedDataChannel: Channel<ReceivedData>,
        sourceID: String,
        channelID: String,
        channel: MqttChannelConfiguration,
        topic: String,
        message: MqttMessage,
    ) {
        val log = logger.getCtxLoggers(className, "sourceSubscriber")
        runBlocking {
            receivedDataChannel.submit(
                ReceivedData(sourceID, channelID, channel, topic, message.toString()),
                adapterConfiguration?.receivedDataChannelTimeout ?: DEFAULT_RECEIVED_DATA_CHANNEL_TIMEOUT.toDuration(DurationUnit.MILLISECONDS)
            ) { event ->
                channelSubmitEventHandler(
                    event,
                    channelName = "$className:receivedData",
                    tuningChannelSizeName = MqttAdapterConfiguration.CONFIG_RECEIVED_DATA_CHANNEL_SIZE,
                    currentChannelSize = adapterConfiguration?.receivedDataChannelSize ?: 0,
                    tuningChannelTimeoutName = MqttAdapterConfiguration.CONFIG_RECEIVED_DATA_CHANNEL_TIMEOUT,
                    log = log
                )
            }
        }
    }

    /**
     * Handles the received data for all subscriptions, the data is read from a channel to which all received data is
     * sent by the subscription handler
     * @param receivedData ReceivedData
     */
    private suspend fun handleDataReceived(receivedData: ReceivedData) {

        with(receivedData) {
            // Build the channel name for the received data (configured name or a mapped topic name)
            val name = channel.mapTopicName(topic)
            // Store the value in the buffer for the source
            if (name != null) {
                val value = dataValue(receivedData)
                if (value != null) {
                    val store = sourceDataStores[sourceID]
                    store?.add("$channelID$CHANNEL_SEPARATOR$name", value)
                }
            } else {
                // Topic name could not be mapped to a channel name
                val trace = logger.getCtxWarningLog(className, "handleDataReceived")
                trace("Topic name \"$topic\" does not match with any of the TopicName Mappings for \"$sourceID\", Channel \"$channelID\", data wil not be included")
            }
        }
    }

    /**
     * Gets the data value from a received message
     * @param receivedData ReceivedData The received data
     * @return ChannelReadValue?
     */
    private fun dataValue(receivedData: ReceivedData): ChannelReadValue? {

        with(receivedData) {
            // Decode json to get native value
            return if (channel.json) {
                try {
                    var value = gson.fromJson(message, Any::class.java)
                    if (channel.selector != null) {
                        value = applyChannelValueSelector(receivedData, value)
                    }
                    ChannelReadValue(value, timestamp)
                } catch (e: JsonSyntaxException) {
                    val log = logger.getCtxErrorLogEx("dataValue")
                    log("Source \"$sourceID\", Channel \"$channelID\", Value \"$message\" is not valid JSON", e)
                    null
                }
            } else {
                // No json, store value as a string
                ChannelReadValue(message, timestamp)
            }
        }
    }

    /**
     * Applies selector for node to select fields from structured types
     * @param receivedData ReceivedData Received data
     * @param value Any? The received value
     * @return Any?
     */
    private fun applyChannelValueSelector(receivedData: ReceivedData, value: Any?): Any? {

        with(receivedData) {
            // Get the selector
            val selector = channel.selector
            return if (selector != null) {
                val log = logger.getCtxLoggers(className, "applyChannelValueSelector")
                try {
                    val selected = selector.search(value)
                    if (selected == null) {
                        log.warning("Applying selector \"${channel.selectorStr}\" for source \"$sourceID\", node \"$channelID\" on value \"$value\" returns null")
                    } else {
                        log.trace("Applying selector \"${channel.selectorStr}\" for source \"$sourceID\", node \"$channelID\" on value \"$value\" returns \"$selected\"")
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
                        adapter = createMqttAdapter(adapterID, configReader, logger)
                    }
                }
            }

            val config = configReader.getConfig<MqttConfiguration>()
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

        /**
         * Creates an MQTT adapter from its configuration
         * @param configReader ConfigReader Reader for configuration
         * @param logger Logger Logger for output
         * @return ProtocolAdapter
         */
        fun createMqttAdapter(adapterID: String, configReader: ConfigReader, logger: Logger): ProtocolAdapter {

            // obtain mqtt configuration
            val config: MqttConfiguration = try {
                configReader.getConfig()
            } catch (e: Exception) {
                throw TargetException("Error loading configuration: ${e.message}")
            }
            return MqttAdapter(adapterID, config, logger)
        }

        // Used to decode json values from received MQTT messages
        private val gson by lazy { gsonExtended() }


        private val ADAPTER_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_ADAPTER
        )
    }

}
