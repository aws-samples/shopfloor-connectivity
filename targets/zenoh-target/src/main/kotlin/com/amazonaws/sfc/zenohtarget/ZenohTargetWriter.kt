// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.zenohtarget


import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.data.JsonHelper.Companion.extendedJsonException
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_BYTES_SEND
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_MESSAGES
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITES
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_DURATION
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_ERRORS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_SIZE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_SUCCESS
import com.amazonaws.sfc.targets.TargetDataChannel
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.*
import com.amazonaws.sfc.util.TemplateRenderer.containsPlaceHolders
import com.amazonaws.sfc.util.TemplateRenderer.getPlaceHolders
import com.amazonaws.sfc.util.TemplateRenderer.render
import com.amazonaws.sfc.zenohtarget.config.ZenohTargetConfiguration
import com.amazonaws.sfc.zenohtarget.config.ZenohTargetConfiguration.Companion.CONFIG_KEYEXPR
import com.amazonaws.sfc.zenohtarget.config.ZenohTargetConfiguration.Companion.CONFIG_ALTERNATE_KEYEXPR
import com.amazonaws.sfc.zenohtarget.config.ZenohWriterConfiguration
import com.amazonaws.sfc.zenohtarget.config.ZenohWriterConfiguration.Companion.ZENOH_TARGET
import com.google.gson.JsonSyntaxException
import io.zenoh.Config
import io.zenoh.Session
import io.zenoh.Zenoh
import io.zenoh.bytes.Encoding
import io.zenoh.keyexpr.KeyExpr
import io.zenoh.pubsub.Publisher
import io.zenoh.pubsub.PublisherOptions
import io.zenoh.qos.CongestionControl
import io.zenoh.qos.Reliability
import io.zenoh.session.SessionInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.measureTime


class ZenohTargetWriter(
    private val targetID: String,
    private val configReader: ConfigReader,
    private val targetConfig: ZenohTargetConfiguration,
    private val logger: Logger,
    private val resultHandler: TargetResultHandler?
) : TargetWriter {

    private val className = this::class.java.simpleName

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val metricDimensions = mapOf(
        METRICS_DIMENSION_SOURCE to targetID,
        MetricsCollector.METRICS_DIMENSION_TYPE to className
    )

    val targetContext = buildContext("ZENOH-TARGET")

    private val buffers = ConcurrentHashMap<String, TargetDataBuffer>()
    private val timers = ConcurrentHashMap<String, Job>()
    private val timerChannel = Channel<String>(capacity = 100)

    // TODO remove batching ?
    private val doesBatching by lazy { targetConfig.batchSize != 0 || targetConfig.batchCount != 0 || targetConfig.batchInterval != Duration.INFINITE }
    private val usesCompression = targetConfig.compressionType != CompressionType.NONE


    // will hold the connetion/session
    private var _zenohSession: Session? = null
    private suspend fun getSession(): Session? {

        val log = logger.getCtxLoggers(className, "getClient")

        val connectionName = "SFC-ZENOH-$targetID-${getHostName()}-${UUID.randomUUID()}"

        var retries = 0
        val zenohServerConfiguration = targetConfig.zenohConnectEndpoints
        // TODO retries in config?
        // while (_zenohConnection == null && targetContext.isActive && retries < zenohServerConfiguration.connectRetries) {
        while (_zenohSession == null && targetContext.isActive) {
            try {
                Zenoh.initLogFromEnvOr("error")
                val config = Config.loadDefault()
                // The zenoh session mode. "peer" or "client" if set
                if (targetConfig.zenohMode != null) {
                    config.insertJson5("mode", "\"${targetConfig.zenohMode}\"")
                    log.info("Zenoh mode is set to '${targetConfig.zenohMode}'")
                }
                // Endpoints to connect to
                // TODO support more than one endpoint and then do a proper json encoding
                if (targetConfig.zenohConnectEndpoints != null) {
                    config.insertJson5("connect/endpoints", "[\"${targetConfig.zenohConnectEndpoints}\"]")
                    log.info("Zenoh connect endpoints set to '${targetConfig.zenohConnectEndpoints}'")
                }
                // Endpoints to listen on
                // TODO support more than one endpoint and then do a proper json encoding
                if (targetConfig.zenohListenEndpoints != null) {
                    config.insertJson5("listen/endpoints", "[\"${targetConfig.zenohListenEndpoints!!}\"]")
                    log.info("Zenoh listen endpoints set to '${targetConfig.zenohListenEndpoints}'")
                }
                // Disable multicast scouting
                if (targetConfig.zenohDisableMulticastScouting) {
                    config.insertJson5("scouting/multicast/enabled", "false")
                    log.info("Zenoh multicast souting disabled")
                }
                _zenohSession = Zenoh.open(config)
                log.info("Connected or initiated to Zenoh server or peer, connection name id $connectionName")
                val info: SessionInfo = _zenohSession!!.info()
                log.info("Zenoh zid: ${info.zid()}, routers zid ${info.routersZid()}, peers zid: ${info.peersZid()}")
            } catch (e: Exception) {
                logger.getCtxErrorLog(className, "zenohConnection")("Error creating Zenoh connection, ${e.message}")
            }
            if (_zenohSession == null) {
                // TODO add delay configuration
                //log.info("Waiting ${zenohServerConfiguration.waitAfterConnectError} before trying to create Zenoh connection")
                //delay(zenohServerConfiguration.waitAfterConnectError)
                retries++
            }
        }
        return _zenohSession
    }

    /**
     * Writes message to the target.
     * @param targetData TargetData
     */
    override suspend fun writeTargetData(targetData: TargetData) {
        // accept data and send to worker for further processing
        targetDataChannel.submit(targetData, logger.getCtxLoggers(className, "writeTargetData"))
    }

    /**
     * Closes the writer
     */
    override suspend fun close() {
        try {
            targetContext.cancel()
            _zenohSession?.close()
        } catch (e: Exception) {
            if (!e.isJobCancellationException) {
                logger.getCtxErrorLogEx(className, "close")("Error closing NATS writer", e)
            }
        }
    }


    private val targetResults = if (resultHandler != null) TargetResultBufferedHelper(targetID, resultHandler, logger) else null
    private val config: ZenohWriterConfiguration by lazy { configReader.getConfig() }
    private val scope = buildScope("Zenoh Target")

    // channel to pass data to the coroutine that publishes the data to the keyexpr
    private val targetDataChannel = TargetDataChannel.create(targetConfig, "$className:targetDataChannel")

    // Coroutine publishing the messages to the target keyexpr
    private val writer = scope.launch(context = Dispatchers.IO, name = "Writer") {
        val log = logger.getCtxLoggers(className, "writer")

        try {
            log.info("Zenoh Writer for target \"$targetID\" writer publishing to keyexprs on target $targetID")
            while (isActive) {
                try {
                    select {

                        targetDataChannel.channel.onReceive { targetData ->

                            targetResults?.add(targetData)

                            val keyexprMessages = mapTargetDataToKeyexpr(targetData)
                            if (keyexprMessages.size > 1 && containsPlaceHolders(targetConfig.keyexpr)){
                                log.trace("Message ${targetData.serial} mapped to keyexprs ${keyexprMessages.keys}")
                            }

                            keyexprMessages.forEach { (keyexpr, keyexprTargetData) ->

                                val keyexprBuffer = buffers.computeIfAbsent(keyexpr) { TargetDataBuffer(storeFullMessage = false) }
                                val timer = timers.computeIfAbsent(keyexpr) { createTimer(keyexpr) }

                                val messagePayload = buildPayload(keyexprTargetData)
                                publishZenohMessage(messagePayload, keyexpr, timer)
                            }
                        }
                    }


                } catch (e: Exception) {
                    if (!e.isJobCancellationException)
                        log.errorEx("Error in writer", e)

                    timers.keys.forEach {
                        timers[it]?.cancel()
                        timers[it] = createTimer(it)
                    }
                }
            }

        } catch (e: Exception) {
            logger.getCtxErrorLogEx(className, "targetWriter")("Error in target writer", e)
        }

    }


    private fun createTimer(channel: String): Job {
        return scope.launch {
            try {
                delay(targetConfig.batchInterval)
                timerChannel.send(channel)
            } catch (e: Exception) {
                // no harm done, timer is just used to guard for timeouts
            }
        }
    }

    private fun bufferReachedMaxSizeOrMessages(buffer: TargetDataBuffer, keyexpr: String, log: Logger.ContextLogger): Boolean {
        val reachedBufferCount = if (targetConfig.batchCount > 0) (buffer.size >= targetConfig.batchCount) else false
        if (reachedBufferCount) log.trace("${targetConfig.batchCount} batch count reached")

        val reachedBufferSize = (targetConfig.batchSize > 0) && (buffer.payloadSize + (2 * (buffer.size - 1)) >= targetConfig.batchSize)

        if (reachedBufferSize) log.trace("${targetConfig.batchSize.byteCountString} batch size for keyexpr $keyexpr reached")

        return reachedBufferSize || reachedBufferCount
    }


    private fun checkMessagePayloadSize(targetData: TargetData, payloadSize: Int, log: Logger.ContextLogger): Boolean {
        if (usesCompression) return true
        return if (targetConfig.maxPayloadSize != null && payloadSize > targetConfig.maxPayloadSize!!) {
            log.error("Size $payloadSize bytes of message is larger max payload size  ${targetConfig.maxPayloadSize!!.byteCountString} for target")
            TargetResultHelper(targetID, resultHandler, logger).error(targetData)
            false
        } else true
    }

    private fun exceedBufferOrMaxPayloadWhenBufferingMessage(buffer: TargetDataBuffer, payload: String): Boolean {
        if (usesCompression) return false
        val bufferedPayloadSizeWhenAddingMessage = payload.length + (2 + (buffer.size - 1)) + buffer.payloadSize
        val bufferSizeExceededWhenAddingMessage = (targetConfig.batchSize > 0) && (bufferedPayloadSizeWhenAddingMessage > targetConfig.batchSize)
        val maxPayloadSizeExceededWhenAddingMessage =
            targetConfig.maxPayloadSize != null && bufferedPayloadSizeWhenAddingMessage > targetConfig.maxPayloadSize!!
        val reachedMaxSizeWhenAddingToBuffer = (bufferSizeExceededWhenAddingMessage || maxPayloadSizeExceededWhenAddingMessage)
        return reachedMaxSizeWhenAddingToBuffer
    }

    // TODO check if we can move the publisher to the context so we don't need to create it new every time
    private suspend fun publishZenohMessage(zenohMessage: String, keyexpr: String, timer: Job): Job {

        if (timer.isActive) timer.cancel()

        val log = logger.getCtxLoggers(className, "writeBufferedMessages")

        return try {
            val duration = measureTime {
                val session = getSession()

                withTimeout(targetConfig.publishTimeout) {
                    // TODO read message encoding from config
                    val keyExpr = KeyExpr.tryFrom(keyexpr)
                    val publisherOptions = PublisherOptions()
                    publisherOptions.encoding = Encoding.ZENOH_STRING
                    publisherOptions.congestionControl = CongestionControl.BLOCK
                    publisherOptions.reliability = Reliability.RELIABLE
                    val publisher: Publisher = session!!.declarePublisher(keyExpr, publisherOptions)
                    log.info("zenohMessage ${zenohMessage.toString()}")
                    publisher.put(zenohMessage.toString())

                    log.info("publish message ${zenohMessage} to key '${keyexpr}'")
                }

                targetResults?.ackBuffered()
            }
            log.trace("Published Zenoh message to keyexpr \"$keyexpr\" in $duration")

            createMetrics(targetID, metricDimensions, zenohMessage.length.toDouble(), duration)
            createTimer(keyexpr)

        } catch (e: Exception) {
            if (!e.isJobCancellationException) {
                 metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions)
                log.error("Error publishing to keyexpr \"$keyexpr\" for target \"$targetID\", ${e.message}, $e")
                if (e is TimeoutCancellationException || _zenohSession == null) {
                    targetResults?.nackBuffered()
                } else {
                    targetResults?.errorBuffered()
                }
            }
            createTimer(keyexpr)
        }

    }

    private val metricsCollector: MetricsCollector? by lazy {
        val metricsConfiguration = config.targets[targetID]?.metrics ?: MetricsSourceConfiguration()
        if (config.isCollectingMetrics) {
            logger.metricsCollectorMethod = collectMetricsFromLogger
            MetricsCollector(
                metricsConfig = config.metrics,
                metricsSourceName = targetID,
                metricsSourceType = MetricsSourceType.TARGET_WRITER,
                metricsSourceConfiguration = metricsConfiguration,
                staticDimensions = TARGET_METRIC_DIMENSIONS,
                logger = logger
            )
        } else null
    }

    private val collectMetricsFromLogger: MetricsCollectorMethod? =
        if (config.isCollectingMetrics) {
            { metricsList ->
                try {
                    val dataPoints = metricsList.map { MetricsDataPoint(it.metricsName, metricDimensions, it.metricUnit, it.metricsValue) }
                    runBlocking {
                        metricsCollector?.put(targetID, dataPoints)
                    }
                } catch (e: java.lang.Exception) {
                    logger.getCtxErrorLogEx(this::class.java.simpleName, "collectMetricsFromLogger")("Error collecting metrics from logger", e)
                }
            }
        } else null

    override val metricsProvider: MetricsProvider? by lazy {
        if (metricsCollector != null) InProcessMetricsProvider(metricsCollector!!, logger) else null
    }

    private val transformation by lazy { if (targetConfig.template != null) OutputTransformation(targetConfig.template!!, logger) else null }

    private fun buildPayload(targetData: TargetData): String =
        if (transformation == null) targetData.toJson(config.elementNames, targetConfig.unquoteNumericJsonValues) else transformation!!.transform(targetData, config.elementNames, targetConfig.templateEpochTimestamp) ?: ""



    private fun createMetrics(
        adapterID: String,
        metricDimensions: MetricDimensions,
        payloadSize: Double,
        duration: Duration
    ) {

            metricsCollector?.put(
                adapterID,
                metricsCollector?.buildValueDataPoint(
                    adapterID,
                    MetricsCollector.METRICS_MEMORY,
                    MemoryMonitor.getUsedMemoryMB().toDouble(),
                    MetricUnits.MEGABYTES
                ),
                metricsCollector?.buildValueDataPoint(
                    adapterID,
                    MetricsCollector.METRICS_MEMORY,
                    MemoryMonitor.getUsedMemoryMB().toDouble(),
                    MetricUnits.MEGABYTES
                ),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITES, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_BYTES_SEND, payloadSize.toDouble(), MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_MESSAGES, payloadSize, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(
                    adapterID,
                    METRICS_WRITE_DURATION,
                    duration.inWholeMilliseconds.toDouble(),
                    MetricUnits.MILLISECONDS,
                    metricDimensions
                ),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SIZE, payloadSize, MetricUnits.BYTES, metricDimensions)
            )
    }


    private fun mapTargetDataToKeyexpr(targetData: TargetData): Map<String, TargetData> =
        targetData.splitDataByName(targetConfig.keyexpr, ::buildKeyexpr)


    private fun buildKeyexpr(targetData: TargetData,
                                 sourceName: String,
                                 channel: String,
                                 channelMetadata: Map<String, String>): String {

        val log = logger.getCtxLoggers(className, "buildKeyexpr")

        var keyexpr = if (containsPlaceHolders(targetConfig.keyexpr)) {
            TemplateRenderer.render(targetConfig.keyexpr, targetData.schedule, sourceName, channel, targetID, channelMetadata)
        } else targetConfig.keyexpr

        return keyexpr
    }

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun newInstance(vararg createParameters: Any?) =
            newInstance(
                createParameters[0] as ConfigReader,
                createParameters[1] as String,
                createParameters[2] as Logger,
                createParameters[3] as TargetResultHandler?
            )

        @JvmStatic
        fun newInstance(configReader: ConfigReader, targetID: String, logger: Logger, resultHandler: TargetResultHandler?): TargetWriter {

            // Obtain configuration
            val config: ZenohWriterConfiguration = readConfig(configReader)

            // Obtain configuration for used target
            val natsConfig = config.targets[targetID]
                    ?: throw TargetException("Configuration for $ZENOH_TARGET type target with ID \"$targetID\" does not exist, existing targets are ${config.targets.keys}")
            return try {
                ZenohTargetWriter(
                    configReader = configReader,
                    targetID = targetID,
                    targetConfig = natsConfig,
                    logger = logger,
                    resultHandler = resultHandler
                )
            } catch (e: Throwable) {
                throw TargetException("Error creating $ZENOH_TARGET target for target \"$targetID\", $e")
            }
        }


        private fun readConfig(configReader: ConfigReader): ZenohWriterConfiguration {
            return try {
                configReader.getConfig()
            } catch (e: JsonSyntaxException) {
                throw TargetException("Could not load NATS Target configuration, JSON syntax error, ${e.extendedJsonException(configReader.jsonConfig)}")
            } catch (e: Exception) {
                throw TargetException("Could not load NATS Target configuration: $e")
            }
        }

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
        )

    }

}

