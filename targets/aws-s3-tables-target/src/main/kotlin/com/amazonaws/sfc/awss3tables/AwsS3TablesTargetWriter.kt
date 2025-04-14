// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awss3tables


import com.amazonaws.sfc.awsiot.AwsIoTCredentialSessionProvider
import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.awss3tables.config.AwsS3TablesTargetConfiguration
import com.amazonaws.sfc.awss3tables.config.AwsS3TablesWriterConfiguration

import com.amazonaws.sfc.awss3tables.config.AwsS3TablesWriterConfiguration.Companion.AWS_S3_TABLES
import com.amazonaws.sfc.config.BaseConfiguration
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.data.*
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
import com.amazonaws.sfc.targets.AwsServiceTargetClientHelper
import com.amazonaws.sfc.targets.TargetDataChannel
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.*
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import org.apache.iceberg.CatalogProperties
import software.amazon.awssdk.services.s3tables.S3TablesClient

import org.apache.iceberg.catalog.Namespace
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import java.util.concurrent.locks.ReentrantLock
import kotlin.collections.get
import kotlin.concurrent.withLock
import kotlin.time.DurationUnit
import kotlin.time.toDuration


// AWS S3 target
class AwsS3TablesTargetWriter(
    private val targetID: String,
    private val configReader: ConfigReader,
    private val logger: Logger,
    resultHandler: TargetResultHandler?
) : TargetWriter {

    private val className = this::class.java.simpleName

    init {
        logger.getCtxInfoLog(className, "")(BuildConfig.toString())
    }

    private val scope = buildScope(AWS_S3_TABLES)


    // Mutex for r/w consistency credential elements
    private val credentialsLock = ReentrantLock()
    private var lastCredentials: AwsCredentials? = null

    // Configuration loaded for the MSK target writer
    private val s3TablesWriterConfig: AwsS3TablesWriterConfiguration by lazy {
        try {
            configReader.getConfig()
        } catch (e: Exception) {
            throw ConfigurationException("Could not load $AWS_S3_TABLES target configuration: ${e.message}", BaseConfiguration.CONFIG_TARGETS)
        }
    }


    private val s3TablesTargetConfig: AwsS3TablesTargetConfiguration by lazy{
             s3TablesWriterConfig.targets[targetID]
                    ?: throw ConfigurationException(
                        "Configuration for type $AWS_S3_TABLES for target with ID \"$targetID\" does not exist, existing targets are ${s3TablesWriterConfig.targets.keys}",
                        BaseConfiguration.CONFIG_TARGETS
                    )
        }

    private val metricDimensions = mapOf(
        METRICS_DIMENSION_SOURCE to targetID,
        MetricsCollector.METRICS_DIMENSION_TYPE to className
    )

    private val clientHelper =
        AwsServiceTargetClientHelper(
            configReader.getConfig<AwsS3TablesWriterConfiguration>(),
            targetID,
            S3TablesClient.builder(),
            logger
        )


    private val targetConfig: AwsS3TablesTargetConfiguration by lazy {
        clientHelper.targetConfig(config, targetID, AWS_S3_TABLES)
    }

    private val credentialClientConfig: AwsIotCredentialProviderClientConfiguration? by lazy {
        if (!s3TablesTargetConfig.credentialProviderClient.isNullOrEmpty()) {
            val cc = s3TablesWriterConfig
            cc.awsCredentialServiceClients[s3TablesTargetConfig.credentialProviderClient]
                    ?: throw ConfigurationException(
                        "Configuration for \"${s3TablesTargetConfig.credentialProviderClient}\" does not exist, configured clients are ${s3TablesWriterConfig.awsCredentialServiceClients.keys}",
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
            log.info("Using SFC credential provider client ${s3TablesTargetConfig.credentialProviderClient}")
            AwsIoTCredentialSessionProvider(credentialClientConfig, logger)
        }
    }

    // Flag is set to true when the SFC credentials provider has set the credentials
    private var credentialsInitialized = (credentialClientConfig == null)

    // If using the SFC credentials provider this worker wil periodically resolve the temporary credentials
    private val credentialsWorker = if (credentialsProvider is AwsIoTCredentialSessionProvider) scope.launch {
        val log = logger.getCtxLoggers(className, "credentialsWorker")
        while (isActive) {
            try {
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
            } catch (e: Exception) {
                if (e.isJobCancellationException)
                    log.info("Credentials worker stopped")
                else
                    log.errorEx("Credentials worker error", e)
            }
        }
    }
    else null

    private val targetDataChannel = TargetDataChannel.create(targetConfig, "$className::targetDatChannel")


    private val targetResults = if (resultHandler != null) TargetResultBufferedHelper(targetID, resultHandler, logger) else null
    private val buffer = TargetDataBuffer(storeFullMessage = true)

    private val config: AwsS3TablesWriterConfiguration
        get() {
            return clientHelper.writerConfig(configReader, AWS_S3_TABLES)
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



    private val writer = scope.launch("Writer") {

        var timer = timerJob()

        val loggers = logger.getCtxLoggers(AwsS3TablesTargetWriter::class.java.simpleName, "writer")
        loggers.info("AWS S3 writer for target \"$targetID\" writing to S3 bucket \"${targetConfig.tableBucketName}\"  in region ${targetConfig.region}")

        while (isActive) {
            try {
                select {
                    targetDataChannel.onReceive { targetData ->
               //         val payload = buildPayload(targetData)

                        targetResults?.add(targetData)
                        buffer.add(targetData, "")

                        loggers.trace("Received message, buffered items is ${buffer.size} with a total size of ${buffer.payloadSize.byteCountString}")

                        // flush if reached buffer size
                        if (targetData.noBuffering || buffer.payloadSize >= targetConfig.bufferSize) {
                            loggers.trace("${targetConfig.bufferSize.byteCountString}  buffer size reached, flushing buffer")
                            timer.cancel()
                            flush()
                            timer = timerJob()

                        }
                    }
                    timer.onJoin {
                        loggers.trace("${targetConfig.interval / 1000} seconds buffer interval reached, flushing buffer")
                        flush()
                        timer = timerJob()
                    }
                }
            } catch (e: Exception) {
                if (!e.isJobCancellationException)
                    loggers.errorEx("Error in writer", e)
            }
        }

    }

    private fun flush() {


        val log = logger.getCtxLoggers(className, "flush")
        if (buffer.size == 0) {
            return
        }

        val properties: MutableMap<String?, String?> = HashMap<String?, String?>()
        properties.put(CatalogProperties.CATALOG_IMPL, "org.apache.iceberg.rest.RESTCatalog")
        properties.put(CatalogProperties.URI, "https://s3tables.eu-west-1.amazonaws.com/iceberg")
        properties.put(CatalogProperties.WAREHOUSE_LOCATION, "arn:aws:s3tables:eu-west-1:816487731748:bucket/sfc-table-bucket")
        properties.put(CatalogProperties.FILE_IO_IMPL, "org.apache.iceberg.aws.s3.S3FileIO")
        properties.put("rest.signing-name", "s3tables")
        properties.put("rest.signing-region", "eu-west-1")
        properties.put("rest.sigv4-enabled", "true")

        val nameSpace = Namespace.of(targetConfig.namespace)

        val tableBucketName = targetConfig.tableBucketName
        log.trace("Writing data to bucket \"$tableBucketName\"")

        val start = DateTime.systemDateTime().toEpochMilli()

        try {

//            val request = buildPutObjectRequest()
//            val content = buildContent(request.key())
//            val resp = clientHelper.executeServiceCallWithRetries {
//                try {
//                    log.info("Creating S3 object ${request.key()} containing ${content.optionalContentLength().get().byteCountString}")
//                    val resp = s3Client.putObject(request, content)
//                    targetResults?.ackBuffered()
//
//                    val writeDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()
//                    createMetrics(targetID, metricDimensions, writeDurationInMillis)
//
//                    resp
//                } catch (e: AwsServiceException) {
//                    log.trace("S3 putObject error ${e.message}")
//                    // Check the exception, it will throw an AwsServiceRetryableException if the error is recoverable
//                    clientHelper.processServiceException(e)
//                    // Non recoverable service exceptions
//                    throw e
//                }
//            }
//
//            log.trace("S3 putObject result is ${resp.sdkHttpResponse()?.statusCode()}")

        } catch (e: Exception) {
            log.errorEx("Error writing to bucket \"$tableBucketName\" for target \"$targetID\"", e)
            runBlocking { metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions) }

            if (canNotReachAwsService(e)) {
                targetResults?.nackBuffered()
            } else {
                targetResults?.errorBuffered()
            }
        } finally {
            buffer.clear()
        }
    }

    private fun createMetrics(
        adapterID: String,
        metricDimensions: MetricDimensions,
        writeDurationInMillis: Double
    ) {

        runBlocking {
            metricsCollector?.put(
                adapterID,
                metricsCollector?.buildValueDataPoint(adapterID, MetricsCollector.METRICS_MEMORY, MemoryMonitor.getUsedMemoryMB().toDouble(), MetricUnits.MEGABYTES),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITES, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_MESSAGES, buffer.size.toDouble(), MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_DURATION, writeDurationInMillis, MetricUnits.MILLISECONDS, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions),
                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SIZE, buffer.payloadSize.toDouble(), MetricUnits.BYTES, metricDimensions)
            )
        }
    }

    private fun CoroutineScope.timerJob() = launch("Timeout timer") {
        try {
            delay(targetConfig.interval.toLong())
        } catch (e: Exception) {
            // no harm done, timer is just used to guard for timeouts
        }
    }






    override suspend fun writeTargetData(targetData: TargetData) {
        targetDataChannel.submit(targetData, logger.getCtxLoggers("$className:writeTargetData"))
    }

    override suspend fun close() {
        flush()
        writer.cancel()

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
         * Creates an instance of an AWS S3 writer from the passed configuration
         * @param configReader ConfigReader Reads the configuration for the writer
         * @see AwsS3TablesWriterConfiguration
         * @param targetID String ID of the target
         * @param logger Logger Logger for output
         * @return TargetWriter
         * @throws Exception
         */
        @JvmStatic
        fun newInstance(configReader: ConfigReader, targetID: String, logger: Logger, resultHandler: TargetResultHandler?): TargetWriter {
            return try {
                AwsS3TablesTargetWriter(targetID, configReader, logger, resultHandler)
            } catch (e: Throwable) {
                throw TargetException("Error creating AWS S3 target writer, ${e.message}")
            }
        }

        val TARGET_METRIC_DIMENSIONS = mapOf(
            MetricsCollector.METRICS_DIMENSION_SOURCE_CATEGORY to METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
        )


    }
}