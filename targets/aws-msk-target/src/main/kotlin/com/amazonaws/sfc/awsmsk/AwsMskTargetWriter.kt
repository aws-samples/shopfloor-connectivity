// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsmsk


import com.amazonaws.sfc.awsiot.AwsIoTCredentialSessionProvider
import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.awsmsk.config.AwsMskTargetConfiguration
import com.amazonaws.sfc.awsmsk.config.AwsMskWriterConfiguration
import com.amazonaws.sfc.awsmsk.config.AwsMskWriterConfiguration.Companion.AWS_MSK_TARGET
import com.amazonaws.sfc.awsmsk.config.Serialization
import com.amazonaws.sfc.config.BaseConfiguration
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.data.*
import com.amazonaws.sfc.ipc.extensions.GrpcTargetValueFromNativeExt
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_MESSAGES
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITES
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_DURATION
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_ERRORS
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_SIZE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_SUCCESS
import com.amazonaws.sfc.system.DateTime
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.getHostName
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.errors.TimeoutException
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.msk.auth.iam.IAMClientCallbackHandler
import software.amazon.msk.auth.iam.IAMLoginModule
import java.time.Instant
import kotlin.time.DurationUnit
import kotlin.time.toDuration


class AwsMskTargetWriter(
    private val targetID: String,
    private val configReader: ConfigReader,
    private val logger: Logger,
    resultHandler: TargetResultHandler?
) : TargetWriter {

    private val className = this::class.java.simpleName

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val scope = CoroutineScope(Dispatchers.Default) + CoroutineName("MSK Target")

    // channel to pass messages to coroutine that sends data to the MSK topic
    private val targetDataChannel = Channel<TargetData>(100)

    private var wasAnyDataSent = false

    private val targetResults = if (resultHandler != null) TargetResultHelper(targetID, resultHandler, logger) else null

    // Mutex for r/w consistency credential elements
    private val credentialsLock = Mutex()
    private var lastCredentials: AwsCredentials? = null

    // Configuration loaded for the MSK target writer
    private val mskWriterConfig: AwsMskWriterConfiguration by lazy {
        try {
            configReader.getConfig()
        } catch (e: Exception) {
            throw ConfigurationException("Could not load $AWS_MSK_TARGET target configuration: ${e.message}", BaseConfiguration.CONFIG_TARGETS)
        }
    }

    // Configuration loaded for this MSK target
    private val mskTargetConfig: AwsMskTargetConfiguration by lazy {
        mskWriterConfig.targets[targetID]
            ?: throw ConfigurationException(
                "Configuration for type $AWS_MSK_TARGET for target with ID \"$targetID\" does not exist, existing targets are ${mskWriterConfig.targets.keys}",
                BaseConfiguration.CONFIG_TARGETS
            )
    }

    private val credentialClientConfig: AwsIotCredentialProviderClientConfiguration? by lazy {
        if (!mskTargetConfig.credentialProviderClient.isNullOrEmpty()) {
            val cc = mskWriterConfig
            cc.awsCredentialServiceClients[mskTargetConfig.credentialProviderClient]
                ?: throw ConfigurationException(
                    "Configuration for \"${mskTargetConfig.credentialProviderClient}\" does not exist, configured clients are ${mskWriterConfig.awsCredentialServiceClients.keys}",
                    BaseConfiguration.CONFIG_CREDENTIAL_PROVIDER_CLIENT
                )
        } else null
    }


    // Get the credentials provider, which can be the SFC provider using the AwsIot credentials service or the default SDK credentials chain
    private val credentialsProvider by lazy {
        val log = logger.getCtxLoggers(className, "credentialsProvider")
        val config = credentialClientConfig
        if (config == null) {
            log.info("Using default AWS credentials provider")
            DefaultCredentialsProvider.create()
        } else {
            log.info("Using SFC credential provider client ${mskTargetConfig.credentialProviderClient}")
            AwsIoTCredentialSessionProvider(credentialClientConfig, logger)
        }
    }

    // Flag is set to true when the SFC credentials provider has set the credentials
    private var credentialsInitialized = (credentialClientConfig == null)

    // Transformation for output data
    private val transformation by lazy {
        if (mskTargetConfig.template != null) {
            logger.getCtxInfoLog("Template \"${mskTargetConfig.template} specified, this will overwrite the serialization setting ${mskTargetConfig.serialization}")
            OutputTransformation(mskTargetConfig.template!!, logger)
        } else null
    }


    private val metricDimensions = mapOf(
        METRICS_DIMENSION_SOURCE to targetID,
        MetricsCollector.METRICS_DIMENSION_TYPE to className
    )

    // If using the SFC credentials provider this worker wil periodically resolve the temporary credentials
    private val credentialsWorker = if (credentialsProvider is AwsIoTCredentialSessionProvider) scope.launch {

        while (isActive) {
            // resolve credentials, note that only when the existing credentials are no longer valid new one will be requested from the credentials service
            val credentials = credentialsProvider.resolveCredentials()
            if (lastCredentials == null || lastCredentials != credentials) {
                credentialsLock.withLock {
                    credentialsInitialized = false
                    lastCredentials = credentials
                    System.setProperty("aws.accessKeyId", credentials.accessKeyId())
                    System.setProperty("aws.secretKey", credentials.secretAccessKey())
                    System.setProperty("aws.secretAccessKey", credentials.secretAccessKey())
                    if (credentials is AwsSessionCredentials) {
                        System.setProperty("aws.sessionToken", credentials.sessionToken())
                    }
                    credentialsInitialized = true
                }
            }

            delay(60.toDuration(DurationUnit.SECONDS))
        }
    }
    else null


    private val providerProperties by lazy {

        val properties = mutableMapOf<String, Any>(
            CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to mskTargetConfig.bootstrapServers.joinToString(separator = ","),
            CommonClientConfigs.CLIENT_ID_CONFIG to "sfc-msk-target-${getHostName()}",
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SASL_SSL",
            ProducerConfig.ACKS_CONFIG to mskTargetConfig.acknowledgements.toString(),
            ProducerConfig.COMPRESSION_TYPE_CONFIG to mskTargetConfig.compressionType.toString(),
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java.name,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to ByteArraySerializer::class.java.name,
            SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS to IAMClientCallbackHandler::class.java.name,
            SaslConfigs.SASL_JAAS_CONFIG to "${IAMLoginModule::class.java.name} required;",
            SaslConfigs.SASL_MECHANISM to "AWS_MSK_IAM"
        )

        if (mskTargetConfig.batchSize != null) properties[ProducerConfig.BATCH_SIZE_CONFIG] = mskTargetConfig.batchSize!!

        // merge with additional provider properties
        mskTargetConfig.providerProperties.forEach { (propertyName, propertyValue) ->
            properties[propertyName] = propertyValue
        }
        properties
    }

    private val producer: KafkaProducer<String, ByteArray> by lazy {
        KafkaProducer(providerProperties)
    }

    // Periodically flushes the producer to enforce date to be submitted even when the producer buffer is not full
    private val periodicalFlush: Job =

        scope.launch("Kafka producer flush") {
            val log = logger.getCtxLoggers(className, "periodicalFlush")
            if (mskTargetConfig.interval == null || mskTargetConfig.interval!!.inWholeMilliseconds == 0L) return@launch

            try {
                while (isActive) {
                    delay(mskTargetConfig.interval!!)
                    log.trace("Flushing Kafka producer triggered")
                    if (wasAnyDataSent)
                        flush()

                }
            } catch (e: Exception) {
                // no action needed
            }
        }


    private val metricsCollector: MetricsCollector? by lazy {
        val metricsConfiguration = mskTargetConfig.metrics
        if (mskWriterConfig.isCollectingMetrics) {
            logger.metricsCollectorMethod = collectMetricsFromLogger
            MetricsCollector(
                metricsConfig = mskWriterConfig.metrics,
                metricsSourceName = targetID,
                metricsSourceType = MetricsSourceType.TARGET_WRITER,
                metricsSourceConfiguration = metricsConfiguration,
                staticDimensions = TARGET_METRIC_DIMENSIONS,
                logger = logger
            )
        } else null
    }

    private val collectMetricsFromLogger: MetricsCollectorMethod? =
        if (mskWriterConfig.isCollectingMetrics) {
            { metricsList ->
                try {
                    val dataPoints = metricsList.map { MetricsDataPoint(it.metricsName, metricDimensions, it.metricUnit, it.metricsValue) }
                    runBlocking {
                        metricsCollector?.put(targetID, dataPoints)
                    }
                } catch (e: java.lang.Exception) {
                    logger.getCtxErrorLog(this::class.java.simpleName, "collectMetricsFromLogger")("Error collecting metrics from logger, $e")
                }
            }
        } else null

    override val metricsProvider: MetricsProvider?
        get() = if (metricsCollector != null) InProcessMetricsProvider(metricsCollector!!, logger) else null


    override suspend fun writeTargetData(targetData: TargetData) {
        targetDataChannel.send(targetData)
    }

    private val topicName by lazy { mskTargetConfig.topicName }


    override suspend fun close() {
        flush()

        writer.cancel()
        credentialsWorker?.cancel()
        periodicalFlush.cancel()
        producer.close()
    }


    // coroutine writing messages to topic
    private val writer = scope.launch("Writer") {
        val log = logger.getCtxLoggers(className, "writer")
        log.info("AWS MKS writer for target \"$targetID\" writing to topic \"$topicName\"  on target \"$targetID\"")

        while (isActive) {
            if (credentialsInitialized) {
                val targetData = targetDataChannel.receive()
                handleTargetData(targetData)
            } else {
                waitForCredentialsInitializationFinished()
            }
        }
    }

    private suspend fun waitForCredentialsInitializationFinished() {

        try {
            withTimeout(60.toDuration(DurationUnit.SECONDS)) {
                while (!credentialsInitialized) {
                    delay(1.toDuration(DurationUnit.SECONDS))
                }
            }
        } catch (e: TimeoutCancellationException) {
            logger.getCtxErrorLog(className, "waitForCredentialsInitializationFinished")("Timeout obtaining credentials")
        }
    }


    private fun handleTargetData(targetData: TargetData) {

        val log = logger.getCtxLoggers(className, "handleTargetData")

        val payload = buildRecordPayload(targetData)

        if (payload == null) {
            log.error("Data transformation or serialization returned null")
            return
        }

        val record = buildProducerRecord(targetData, payload)
        sendRecord(record, targetData)

    }

    private fun sendRecord(
        rec: ProducerRecord<String, ByteArray>,
        targetData: TargetData,
    ) {

        val log = logger.getCtxLoggers(className, "sendRecord")

        val start = DateTime.systemDateTime().toEpochMilli()

        producer.send(rec) { metadata: RecordMetadata, exception ->
            if (exception == null) {
                targetResults?.ack(targetData)
                val writeDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()
                createMetrics(targetID, metricDimensions, metadata, writeDurationInMillis)
                wasAnyDataSent = true
                log.trace("Message ${targetData.serial} sent successfully to topic \"$topicName\", partition ${metadata.partition()} at offset ${metadata.offset()}")
                createMetrics(targetID, metricDimensions, metadata, writeDurationInMillis)

                if (mskTargetConfig.interval!!.inWholeMilliseconds == 0L) {
                    flush()
                    wasAnyDataSent = false
                }
            } else {

                when (exception) {
                    is TimeoutException -> {
                        log.warning("Timeout writing  message ${targetData.serial} to topic \"$topicName\" for target \"$targetID\", ${exception.message}")
                        targetResults?.nack(targetData)
                    }

                    else -> {
                        log.error("Error writing message ${targetData.serial} to topic \"$topicName\" for target \"$targetID\", ${exception.message}")
                        targetResults?.error(targetData)
                        runBlocking { metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions) }
                    }
                }
                runBlocking { metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions) }
            }
        }
    }

    private fun buildProducerRecord(targetData: TargetData, payload: ByteArray?): ProducerRecord<String, ByteArray> {

        val headers = mutableListOf<Header>(RecordHeader(mskWriterConfig.elementNames.serial, targetData.serial.toByteArray()))
        mskTargetConfig.headers.forEach { (k, v) ->
            headers.add(RecordHeader(k, v.toByteArray()))
        }
        return ProducerRecord(topicName, mskTargetConfig.partition, null, mskTargetConfig.key, payload, headers)
    }

    private fun buildRecordPayload(targetData: TargetData) = when {

        // Transformed output will overwrite serialization type
        transformation != null ->
            transformation!!.transform(targetData, mskWriterConfig.elementNames)?.toByteArray(Charsets.UTF_8)

        mskTargetConfig.serialization == Serialization.JSON -> targetData.toJson(mskWriterConfig.elementNames).toByteArray(Charsets.UTF_8)
        mskTargetConfig.serialization == Serialization.PROTOBUF -> GrpcTargetValueFromNativeExt.newWriteValuesRequest(targetData, false).toByteArray()

        else -> throw NotImplementedError("Data serialization ${mskTargetConfig.serialization} implemented")
    }


    private fun flush() {
        if (wasAnyDataSent) {
            runBlocking {
                credentialsLock.withLock {
                    try {
                        producer.flush()
                    } catch (e: Exception) {
                        logger.getCtxErrorLog(className, "flush")("Error flushing kafka producer, $e")
                    }
                }
            }
        }
    }


    private fun createMetrics(
        adapterID: String,
        metricDimensions: MetricDimensions,
        metadata: RecordMetadata,
        writeDurationInMillis: Double
    ) {

        runBlocking {
            val ts = if (metadata.hasTimestamp()) Instant.ofEpochMilli(metadata.timestamp()) else DateTime.systemDateTimeUTC()
            metricsCollector?.put(
                adapterID,
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITES, 1.0, MetricUnits.COUNT, metricDimensions, ts),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_MESSAGES, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_DURATION, writeDurationInMillis, MetricUnits.MILLISECONDS, metricDimensions),
                metricsCollector?.buildValueDataPoint(
                    adapterID,
                    METRICS_WRITE_SIZE,
                    maxOf(metadata.serializedValueSize().toDouble(), 0.0),
                    MetricUnits.BYTES,
                    metricDimensions
                ),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions)

            )
        }
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

        /**
         * Creates MSK target writer instance from configuration.
         * @param configReader ConfigReader Reader for target configuration
         * @param targetID String ID of the target
         * @param logger Logger Logger for output
         * @param resultHandler Callback for target result
         * @return TargetWriter Created MSK target writer
         */
        @JvmStatic
        fun newInstance(configReader: ConfigReader, targetID: String, logger: Logger, resultHandler: TargetResultHandler?): TargetWriter {
            return try {
                AwsMskTargetWriter(targetID, configReader, logger, resultHandler)
            } catch (e: Throwable) {
                throw TargetException("Error creating AWS MSK target writer, ${e.message}")
            }
        }

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
        )

    }


}
