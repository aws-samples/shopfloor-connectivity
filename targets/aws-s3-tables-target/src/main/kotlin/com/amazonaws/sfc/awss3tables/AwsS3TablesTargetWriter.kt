// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awss3tables


import com.amazonaws.sfc.awsiot.AwsIoTCredentialSessionProvider
import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.awss3tables.AwsS3TablesTypesHelper.from
import com.amazonaws.sfc.awss3tables.config.*
import com.amazonaws.sfc.awss3tables.config.AwsS3TablesTargetConfiguration.Companion.CONFIG_AUTO_CREATE
import com.amazonaws.sfc.awss3tables.config.AwsS3TablesWriterConfiguration.Companion.AWS_S3_TABLES
import com.amazonaws.sfc.config.BaseConfiguration
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_TARGETS
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.data.TargetData
import com.amazonaws.sfc.data.TargetResultBufferedHelper
import com.amazonaws.sfc.data.TargetResultHandler
import com.amazonaws.sfc.data.TargetWriter
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_ERRORS
import com.amazonaws.sfc.targets.TargetDataChannel
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.transformations.invoke
import com.amazonaws.sfc.util.buildScope
import com.amazonaws.sfc.util.isJobCancellationException
import com.amazonaws.sfc.util.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import org.apache.iceberg.DataFile
import org.apache.iceberg.PartitionData
import org.apache.iceberg.PartitionSpec
import org.apache.iceberg.Table
import org.apache.iceberg.catalog.TableIdentifier
import org.apache.iceberg.data.GenericRecord
import org.apache.iceberg.data.parquet.GenericParquetWriter
import org.apache.iceberg.parquet.Parquet
import org.apache.iceberg.types.Type
import org.apache.iceberg.types.Types
import org.apache.iceberg.util.StructLikeMap
import org.checkerframework.checker.units.qual.t
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.system.exitProcess
import kotlin.time.DurationUnit
import kotlin.time.measureTime
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


    private val credentialsLock = ReentrantLock()

    // current credentials
    private var credentials: AwsCredentials? = null

    // last obtained credentials
    private var lastCredentials: AwsCredentials? = null

    private val credentialsAvailableChannel = Channel<Any>(Channel.CONFLATED)

    private var awsTablesHelperPrivate: AwsS3TablesHelper? = null

    private val catalogTables = mutableMapOf<TableIdentifier, Table>()


    private val writerConfiguration: AwsS3TablesWriterConfiguration by lazy {
        try {
            AwsS3TablesTargetConfigReader(configReader).getConfig()
        } catch (e: Exception) {
            throw ConfigurationException("Could not load $AWS_S3_TABLES Target configuration: ${e.message}", CONFIG_TARGETS, "")
        }
    }

    private val targetConfiguration: AwsS3TablesTargetConfiguration by lazy {
        writerConfiguration.targets[targetID]
                ?: throw ConfigurationException(
                    "Configuration for type $AWS_S3_TABLES for target with ID \"$targetID\" does not exist, existing targets are ${writerConfiguration.targets.keys}",
                    CONFIG_TARGETS, "")
        // clientHelper.targetConfig(config, targetID, AWS_S3_TABLES)
    }

    private val timers =
        (targetConfiguration.tables.map { it.tableName to createTimer(it.tableName) }).toMap().toMutableMap()



    val tablesHelper: AwsS3TablesHelper?
        get() {
            val log = logger.getCtxLoggers(className, "awsTablesHelper")

            if (awsTablesHelperPrivate == null) {
                awsTablesHelperPrivate = runBlocking {
                    select {
                        credentialsAvailableChannel.onReceive {
                            log.trace("Initial credentials received")
                            AwsS3TablesHelper(targetConfiguration, logger)
                        }
                        scope.launch { delay(60.toDuration(DurationUnit.SECONDS)) }.onJoin {
                            log.error("Initial S3 Tables client credentials timeout")
                            null
                        }
                    }
                }

            }
            return awsTablesHelperPrivate
        }




    private val credentialClientConfig: AwsIotCredentialProviderClientConfiguration? by lazy {
        if (!targetConfiguration.credentialProviderClient.isNullOrEmpty()) {
            val cc = writerConfiguration
            cc.awsCredentialServiceClients[targetConfiguration.credentialProviderClient]
                    ?: throw ConfigurationException(
                        "Configuration for \"${targetConfiguration.credentialProviderClient}\" does not exist, configured clients are ${writerConfiguration.awsCredentialServiceClients.keys}",
                        BaseConfiguration.CONFIG_CREDENTIAL_PROVIDER_CLIENT, ""
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
            log.info("Using SFC credential provider client ${targetConfiguration.credentialProviderClient}")
            AwsIoTCredentialSessionProvider(credentialClientConfig, logger)
        }
    }


    // If using the SFC credentials provider this worker wil periodically resolve the temporary credentials
    private val credentialsWorker = if (credentialsProvider is AwsIoTCredentialSessionProvider) scope.launch {
        val log = logger.getCtxLoggers(className, "credentialsWorker")
        while (isActive) {
            try {
                // resolve credentials, note that only when the existing credentials are no longer valid new one will be requested from the credentials service
                credentials = credentialsProvider.resolveCredentials()
                if (credentials != null && (lastCredentials == null || lastCredentials != credentials)) {
                    credentialsLock.withLock {
                        lastCredentials = credentials
                        System.setProperty("aws.accessKeyId", credentials!!.accessKeyId())
                        System.setProperty("aws.secretKey", credentials!!.secretAccessKey())
                        System.setProperty("aws.secretAccessKey", credentials!!.secretAccessKey())
                        if (credentials is AwsSessionCredentials) {
                            System.setProperty("aws.sessionToken", (credentials as AwsSessionCredentials?)!!.sessionToken())
                        }
                        credentialsAvailableChannel.trySend(true)
                    }
                }
            } catch (e: Exception) {
                if (e.isJobCancellationException)
                    log.info("Credentials worker stopped")
                else
                    log.errorEx("Credentials worker error", e)

            }
            delay(60.toDuration(DurationUnit.SECONDS))
        }
    }
    else null


    private val metricDimensions = mapOf(
        METRICS_DIMENSION_SOURCE to targetID,
        MetricsCollector.METRICS_DIMENSION_TYPE to className
    )

    private val targetDataChannel = TargetDataChannel.create(targetConfiguration, "$className::targetDatChannel")


    private val targetResults = if (resultHandler != null) TargetResultBufferedHelper(targetID, resultHandler, logger) else null

    private val buffers = ConcurrentHashMap<String, RecordBuffer>()// TargetDataBuffer(storeFullMessage = false)

    private val timerChannel = Channel<String>(capacity = targetConfiguration.tables.count())


    private val metricsCollector: MetricsCollector? by lazy {
        val metricsConfiguration = writerConfiguration.targets[targetID]?.metrics ?: MetricsSourceConfiguration()
        if (writerConfiguration.isCollectingMetrics) {
            logger.metricsCollectorMethod = collectMetricsFromLogger
            MetricsCollector(
                metricsConfig = writerConfiguration.metrics,
                metricsSourceName = targetID,
                metricsSourceType = MetricsSourceType.TARGET_WRITER,
                metricsSourceConfiguration = metricsConfiguration,
                staticDimensions = TARGET_METRIC_DIMENSIONS,
                logger = logger
            )
        } else null
    }

    private val collectMetricsFromLogger: MetricsCollectorMethod? =
        if (writerConfiguration.isCollectingMetrics) {
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

    fun setupAwsS3TablesResources() {
        setupTableBucket()
        setupNamespace()
        setupTables()
    }

    private fun setupTables() {
        val log = logger.getCtxLoggers(className, "setupTables")
        val tables = tablesHelper!!.listTablesForBucketName(targetConfiguration.tableBucketName)

        targetConfiguration.tables.forEach { tableConfiguration ->
            val tableIdentifier = TableIdentifier.of(targetConfiguration.namespace,tableConfiguration.tableName)
            val t = if (tables.contains(tableIdentifier)) {
                log.info("Table \"$tableIdentifier\" does exist")
                tableIdentifier
            } else {
                if (targetConfiguration.autoCreate) {
                    log.info("Table \"$tableIdentifier\" does not exist, creating")
                    try {
                        val table = tablesHelper!!.createTable(targetConfiguration.namespace, tableConfiguration.tableName, tableConfiguration.schema, tableConfiguration.partition)
                        log.info("Created table \"$tableIdentifier\" for bucket \"${targetConfiguration.tableBucketName}\"")
                    } catch (e: Exception) {
                        throw TargetException("Error creating table \"$tableIdentifier\" for bucket \"${targetConfiguration.tableBucketName}\", $e")
                    }
                } else {
                    throw TargetException("Table \"tableName\" does not exist, and $CONFIG_AUTO_CREATE is false")
                }
            }

            log.info("Loading table \"$tableIdentifier\" from catalog")
            val catalogTable = tablesHelper?.catalog?.loadTable(tableIdentifier)
            if (catalogTable == null) {
                throw TargetException("Could not load table \"$tableIdentifier\"")
            } else{
                log.info("Schema for table \"$tableIdentifier\" is \n${catalogTable.schema()}")
                if (catalogTable.spec().isPartitioned) {
                    log.info("Partition specification for \"$tableIdentifier\" is ${catalogTable.spec()}")
                }
            }
            catalogTables[tableIdentifier] = catalogTable

        }
    }


    private fun setupNamespace() {
        val log = logger.getCtxLoggers(className, "setupNamespace")
        val nameSpaces = tablesHelper!!.listNameSpacesForBucketName(targetConfiguration.tableBucketName)
        if (nameSpaces.contains(targetConfiguration.namespace)) {
            log.info("Namespace \"${targetConfiguration.tableBucketName}\" does exist")
        } else {
            if (targetConfiguration.autoCreate) {
                log.info("Namespace \"${targetConfiguration.tableBucketName}\" does not exist, creating")
                try {
                    tablesHelper!!.createNamespace(targetConfiguration.tableBucketName, targetConfiguration.namespace)
                    log.info("Created namespace \"${targetConfiguration.namespace}\" for bucket \"${targetConfiguration.tableBucketName}\"")
                } catch (e: Exception) {
                    throw TargetException("Error creating namespace \"${targetConfiguration.namespace}\" for bucket \"${targetConfiguration.tableBucketName}, $e")
                }
            } else {
                throw TargetException("Namespace \"${targetConfiguration.namespace}\" does not exist for bucket \"${targetConfiguration.tableBucketName}, and $CONFIG_AUTO_CREATE is false")
            }
        }
    }

    private fun setupTableBucket() {
        val log = logger.getCtxLoggers(className, "setupTableBucket")
        val tableBuckets = tablesHelper!!.listTableBuckets().map { it.name() }

        if (tableBuckets.contains(targetConfiguration.tableBucketName)) {
            log.info("Table bucket \"${targetConfiguration.tableBucketName}\" does exist")

        } else {
            if (targetConfiguration.autoCreate) {
                log.info("Table bucket \"${targetConfiguration.tableBucketName}\" does not exist, creating")
                try {
                    val bucket = tablesHelper!!.createTableBucket(targetConfiguration.tableBucketName)
                    log.info("Created bucket \"$bucket\"")
                } catch (e: Exception) {
                    throw TargetException("Error creating table bucket \"${targetConfiguration.tableBucketName}\", $e")
                }
            } else {
                throw TargetException("Table bucket \"${targetConfiguration.tableBucketName}\" does not exist, and $CONFIG_AUTO_CREATE is false")
            }
        }
    }

    private val writer = scope.launch("Writer") {

        val log = logger.getCtxLoggers(AwsS3TablesTargetWriter::class.java.simpleName, "writer")
        log.info("AWS S3 writer for target \"$targetID\" writing to S3 bucket \"${targetConfiguration.tableBucketName}\"  in region ${targetConfiguration.region}")

        try {
            log.info("Setting up AWS S3 tables resources")
            setupAwsS3TablesResources()
        } catch (e: Exception) {
            log.error("Error setting up AWS  S3 Tables, $e")
            exitProcess(1)
        }

        while (isActive) {

            try {
                select {

                    timerChannel.onReceive { tableName ->
                        timers[tableName]?.cancel()

                        val tableBuffer = buffers[tableName]

                        if (tableBuffer != null) {
                            if (tableBuffer.size > 0) {
                                log.info("Table buffer interval of ${targetConfiguration.interval} reached buffer for table \"$tableName\"")
                                writeBufferedMessages(tableBuffer, tableName)
                            }
                        }
                        timers[tableName] = createTimer(tableName)
                    }

                    targetDataChannel.onReceive { targetData ->
                        processTargetData(targetData)
                    }

                }
            } catch (e: Exception) {
                if (!e.isJobCancellationException)
                    log.errorEx("Error in writer", e)
            }
        }

    }

    private suspend fun processTargetData(targetData: TargetData) {

        val log = logger.getCtxLoggers(className, "processTargetData")
        targetResults?.add(targetData)

        targetConfiguration.tables.forEach { table ->

            val recordBuffer = buildRecords(table, targetData)

            if (targetData.noBuffering || recordBuffer.size >= targetConfiguration.bufferCount) {
                timers[table.tableName]?.cancel()
                log.info("Buffer count ${targetConfiguration.bufferCount} reached for table \"${table.tableName}\"")
                writeBufferedMessages(recordBuffer, table.tableName)
                timers[table.tableName]= createTimer(table.tableName)
            }

        }
    }

    private fun buildRecords(table: TableConfiguration, targetData: TargetData): RecordBuffer {

        val log = logger.getCtxLoggers(className, "build")

        val missingValues = mutableListOf<String>()
        val targetDataMap = targetData.toMap(writerConfiguration.elementNames, true)

        val recordsData = sequence {

            // a table can have multiple values to generate multiple records for a targetData value
            table.mappings.forEachIndexed { index, fieldMappings ->

                // get thet data for a record for each mapping
                val mappedRecordData = sequence {

                    // get value for every field in the schema of the table
                    table.schema.forEach { field ->

                        // get the mapping for the field
                        val fieldMapping = fieldMappings[field.name]
                        if (fieldMapping != null) {

                            // get the value for the field
                            if (fieldMapping.subMappings.isEmpty()) {
                                // native, list or map values
                                val fieldValue = if (field.type != null) getFieldValue(targetDataMap, fieldMapping, table.tableName, field.name, field.type!!, index) else null
                                if (fieldValue != null) yield(field.name to fieldValue)
                                else if (!field.optional) missingValues.add(field.name)
                            } else {
                                // field is a struct value with sub-fields
                                val nestedValue = map(fieldMappings, field, targetDataMap, table, index, missingValues) // nested field values sequence

                                if (nestedValue.isNotEmpty()) {
                                    yield(field.name to nestedValue)

                                    // no sub-fields for a non-optional struct fields
                                } else if (!field.optional) missingValues.add(field.name)
                            }
                        } else if (!field.optional) missingValues.add(field.name)

                    } // table schema fields
                }.toMap()  // mapped records sequence

                if (missingValues.isEmpty()) yield(mappedRecordData)
                else {
                    val s = if (missingValues.size > 1) "s" else ""
                    log.warning("Missing required value$s for table \"${table.tableName}\" mapping $index, field$s ${missingValues.joinToString { "\"$it\"" }} in target ${targetID}")
                }
            } // mappings
        }.toList()


        val tableBuffer = buffers.computeIfAbsent(table.tableName) { RecordBuffer() }
        recordsData.forEach { data ->
            val record = GenericRecord.create(table.catalogSchema).copy(data)
            // record = record.copy(data)
            log.trace("Created record $record")
            tableBuffer.addRecord(targetData.serial, record)
        }
        return tableBuffer
    }

    private fun map(fieldMappingsConfiguration: Map<String, FieldMappingConfiguration>,
                    field: FieldConfiguration,
                    targetDataMap: Map<String, Any>,
                    table: TableConfiguration,
                    index: Int,
                    missingValues: MutableList<String>): Map<String, Any> = sequence {
        val fieldMapping = fieldMappingsConfiguration[field.name]
        // for all sub-fields from the schema for the structured fields
        field.subFields.forEach { subField ->

            // get mapping for the subfield and get the value
            val subFieldMapping = fieldMapping?.subMappings?.get(subField.name)
            val subFieldValue = if (subFieldMapping != null && subField.type != null)
                getFieldValue(targetDataMap, subFieldMapping, table.tableName, "${field.name}.${subField.name}", subField.type!!, index)
            else null

            if (subFieldValue != null) yield(subField.name to subFieldValue)
            else if (!field.optional) missingValues.add("${field.name}.${subField.name}")

        }  // struct sub-fields
    }.toMap()

    private fun getFieldValue(targetDataMap: Map<String, Any>,
                              fieldMappingConfiguration: FieldMappingConfiguration,
                              tableName: String,
                              fieldName: String,
                              fieldType: Type,
                              index: Int): Any? {

        val queryValue = getValue(targetDataMap, fieldMappingConfiguration, tableName, fieldName)

        // test if a value wat retrieved, keep list of null values for non-optional fields
        val fieldValue = if (queryValue != null) {

            val value = fieldType.from(queryValue)
            if (value != null) {
                if (fieldMappingConfiguration.transformationID == null) {
                    value
                } else {
                    applyTransformation(value, fieldName, fieldMappingConfiguration.transformationID!!)
                }
            } else {
                val log = logger.getCtxLoggers(className, "getFieldValue")
                log.warning("Value $queryValue:${typeStr(queryValue)} is not compatible with field type $fieldType for table \"$tableName\" mapping $index, field \"$fieldName\" in target $targetID")
                null
            }
        } else null
        return fieldValue
    }

    private fun getValue(targetDataMap: Map<String, Any>,
                         mapping: FieldMappingConfiguration,
                         tableName: String,
                         fieldName: String): Any? {

        val value = searchValue(mapping, targetDataMap)
        if (logger.level == LogLevel.TRACE) {
            val log = logger.getCtxLoggers(className, "getValue")
            if (value != null) {
                log.trace("Found value \"$value\":${typeStr(value)} for field \"$fieldName\" in table \"$tableName\" using mapping \"${mapping.valueQueryStr}\"")
            } else {
                log.trace("No value found for field \"$fieldName\" in table \"$tableName\" using mapping \"${mapping.valueQueryStr}\"")
            }
        }
        return value
    }

    // type name as a string for single values
    private fun typeStrSingle(a: Any?): String = "${if (a != null) a::class.simpleName else "null"}"

    // type name as a string for array values
    fun typeStr(a: Any?): String =
        if (a is List<*>)
            "[${if ((a as Iterable<*>).toList().isNotEmpty()) typeStrSingle(a.first()) else ""}]"
        else
            typeStrSingle(a)


    private fun createTimer(tableName: String): Job {
        return scope.launch {
            try {
                delay(targetConfiguration.interval)
                timerChannel.send(tableName)
            } catch (_: Exception) {
                // no harm done, timer is just used to guard for timeouts
            }
        }
    }

    private suspend fun writeBufferedMessages(buffer: RecordBuffer, tableName: String) {

        val log = logger.getCtxLoggers(className, "writeBufferedMessages")


        try {


            when {

//                (mqttMessage.payload.size == 0) -> {
//                    log.trace("No payload to publish to topic $tableName")
//                    targetResults?.ackBuffered()
//                    buffer.clear()
//                    createTimer(tableName)
//                }

//                (targetConfig.maxPayloadSize != null && mqttMessage.payload.size > targetConfig.maxPayloadSize!!) -> {
//                    log.error("Size of MQTT message ${mqttMessage.payload.size} bytes is beyond max payload size of  ${targetConfig.maxPayloadSize!!.byteCountString} for target, reduce or set $CONFIG_BATCH_SIZE, $CONFIG_BATCH_COUNT or $CONFIG_BATCH_INTERVAL for this target")
//                    targetResults?.errorBuffered()
//                    metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions)
//                    createTimer(tableName)
//                }

                else -> {
                    val duration = measureTime {

                        val table = catalogTables[TableIdentifier.of(targetConfiguration.namespace, tableName)]
                        if (table != null){
                            val filePath = "${table.location()}/${UUID.randomUUID()}"
                            val file = table.io().newOutputFile(filePath)

                            val partitionData = PartitionData(table.spec().partitionType())
                            val firstRecord = buffer.items.first()
                            table.spec().fields().forEachIndexed { i, f->
                                val sourceValue = firstRecord.second.get(f.sourceId())
                                val type =table.schema().columns().first{it.fieldId() == f.sourceId()}.type()
                              val transformationValue = f.transform().bind(type ).apply { sourceValue }
                                partitionData.set(i, transformationValue)
                            }



                            try {
                                val writer = Parquet.writeData(file)
                                    .schema(table.schema())
                                    .createWriterFunc(GenericParquetWriter::buildWriter)
                                    .overwrite()
                                    .withSpec(if (table.spec().isPartitioned) table.spec() else PartitionSpec.unpartitioned())
                                    .withPartition((partitionData))
                                    .build<GenericRecord>()

                                try{
                                buffer.items.forEach { it ->
                                    writer.write(it.second)
                                }}catch (ee : Exception){
                                    println("error writing $ee")
                                }finally {
                                    writer.close()
                                }
                                val dataFile: DataFile? = writer.toDataFile()
                                table.newAppend().appendFile(dataFile).commit()

                            }catch (e : Exception){
                                log.errorEx("Error writing to file $filePath", e)
                            }


                        }







//                        val client = runBlocking {
//                            getClient(coroutineContext)
//                        }
//
//                        withTimeout(targetConfig.publishTimeout) {
//                            client.publish(tableName, mqttMessage)
//                        }

                        targetResults?.ackBuffered()
                    }
//                    val compressedStr = if (targetConfig.compressionType != CompressionType.NONE) " compressed " else " "
//                    val itemStr = if (doesBatching) " containing ${buffer.size} items " else " "
//                    log.trace("Published MQTT${compressedStr}message to topic\"$tableName\" with size of ${mqttMessage.payload.size.byteCountString} ${itemStr}in $duration")

                    //          createMetrics(targetID, metricDimensions, buffer, mqttMessage.payload.size, duration)

                }
            }

        } catch (e: Exception) {
            if (!e.isJobCancellationException) {
                metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions)
                log.errorEx("Error publishing to topic \"topic\" for target \"$targetID\", ${e.message}", e)
                if (e is TimeoutCancellationException) {
                    targetResults?.nackBuffered()
                } else {
                    targetResults?.errorBuffered()
                }
            }

        } finally {
            buffer.clear()
        }

    }

    private fun searchValue(mapping: FieldMappingConfiguration, data: Any): Any? = try {
        @Suppress("UNCHECKED_CAST")
        mapping.valueQuery?.search(data as Map<String, Any>)
    } catch (_: NullPointerException) {
        null
    } catch (e: Exception) {
        val log = logger.getCtxErrorLogEx(className, "searchData")
        log("Error querying data with expression \"${mapping.valueQueryStr}\"", e)
        null
    }


    private fun applyTransformation(value: Any, name: String, transformationID: String): Any? {
        val log = logger.getCtxLoggers(className, "applyTransformation")

        val transformation = writerConfiguration.transformations[transformationID] ?: return null
        return try {
            log.trace("Applying transformation \"$transformationID\" on value ${value}:${value::class.java.simpleName} to \"$name\"")
            val transformedValue = transformation.invoke(value, name, true, logger)
            log.trace("Result of transformation \"$transformationID\" is ${transformedValue}${if (transformedValue != null) ":${transformedValue::class.java.simpleName}" else ""}")
            transformedValue
        } catch (e: Exception) {
            logger.getCtxErrorLog(className, "applyTransformation")("Error applying transformation $transformationID to name \"$name\", $e")
            null
        }
    }

//    private fun flush() {
//
//
//        val log = logger.getCtxLoggers(className, "flush")
//        if (buffer.size == 0) {
//            return
//        }
//
//        val region = targetConfiguration.region.toString()
//
//        val properties: MutableMap<String?, String?> = HashMap<String?, String?>()
//        properties.put(CatalogProperties.CATALOG_IMPL, "org.apache.iceberg.rest.RESTCatalog")
//
//        properties.put(CatalogProperties.URI, "https://s3tables.$region.amazonaws.com/iceberg")
////        properties.put(CatalogProperties.WAREHOUSE_LOCATION, "arn:aws:s3tables:eu-west-1:816487731748:bucket/sfc-table-bucket")
//        properties.put(CatalogProperties.WAREHOUSE_LOCATION, targetConfiguration.tableBucketName)
//        properties.put(CatalogProperties.FILE_IO_IMPL, "org.apache.iceberg.aws.s3.S3FileIO")
//        properties.put("rest.signing-name", "s3tables")
//        properties.put("rest.signing-region", region)
//        properties.put("rest.sigv4-enabled", "true")
//
//        val nameSpace = Namespace.of(targetConfiguration.namespace)
//
//        val tableBucketName = targetConfiguration.tableBucketName
//        log.trace("Writing data to bucket \"$tableBucketName\"")
//
//        val start = DateTime.systemDateTime().toEpochMilli()
//
//        try {
//
////            val request = buildPutObjectRequest()
////            val content = buildContent(request.key())
////            val resp = clientHelper.executeServiceCallWithRetries {
////                try {
////                    log.info("Creating S3 object ${request.key()} containing ${content.optionalContentLength().get().byteCountString}")
////                    val resp = s3Client.putObject(request, content)
////                    targetResults?.ackBuffered()
////
////                    val writeDurationInMillis = (DateTime.systemDateTime().toEpochMilli() - start).toDouble()
////                    createMetrics(targetID, metricDimensions, writeDurationInMillis)
////
////                    resp
////                } catch (e: AwsServiceException) {
////                    log.trace("S3 putObject error ${e.message}")
////                    // Check the exception, it will throw an AwsServiceRetryableException if the error is recoverable
////                    clientHelper.processServiceException(e)
////                    // Non recoverable service exceptions
////                    throw e
////                }
////            }
////
////            log.trace("S3 putObject result is ${resp.sdkHttpResponse()?.statusCode()}")
//
//        } catch (e: Exception) {
//            log.errorEx("Error writing to bucket \"$tableBucketName\" for target \"$targetID\"", e)
//            runBlocking { metricsCollector?.put(targetID, METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions) }
//
//            if (canNotReachAwsService(e)) {
//                targetResults?.nackBuffered()
//            } else {
//                targetResults?.errorBuffered()
//            }
//        } finally {
//            buffer.clear()
//        }
//    }

    private fun createMetrics(
        adapterID: String,
        metricDimensions: MetricDimensions,
        writeDurationInMillis: Double
    ) {

        runBlocking {
//            metricsCollector?.put(
//                adapterID,
//                metricsCollector?.buildValueDataPoint(adapterID, MetricsCollector.METRICS_MEMORY, MemoryMonitor.getUsedMemoryMB().toDouble(), MetricUnits.MEGABYTES),
//                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITES, 1.0, MetricUnits.COUNT, metricDimensions),
//                metricsCollector?.buildValueDataPoint(adapterID, METRICS_MESSAGES, buffer.size.toDouble(), MetricUnits.COUNT, metricDimensions),
//                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_DURATION, writeDurationInMillis, MetricUnits.MILLISECONDS, metricDimensions),
//                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions),
//                metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SIZE, buffer.payloadSize.toDouble(), MetricUnits.BYTES, metricDimensions)
//            )
        }
    }

    private fun CoroutineScope.timerJob() = launch("Timeout timer") {
        try {
            delay(targetConfiguration.interval)
        } catch (e: Exception) {
            // no harm done, timer is just used to guard for timeouts
        }
    }


    override suspend fun writeTargetData(targetData: TargetData) {
        targetDataChannel.submit(targetData, logger.getCtxLoggers("$className:writeTargetData"))
    }

    override suspend fun close() {
        timers.values.forEach { timer ->
            timer.cancel()
        }
        buffers.forEach { (tableName, buffer) ->
            val tableBuffer = buffers[tableName]
            if (tableBuffer != null && tableBuffer.size > 0) {
                writeBufferedMessages(tableBuffer, tableName)
            }
        }
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
