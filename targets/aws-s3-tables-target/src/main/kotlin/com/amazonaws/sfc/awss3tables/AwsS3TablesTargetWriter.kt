// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awss3tables


import com.amazonaws.sfc.awsiot.AwsIoTCredentialSessionProvider
import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.awss3tables.AwsS3TablesHelper.Companion.buildSchema
import com.amazonaws.sfc.awss3tables.AwsS3TablesTypesHelper.from
import com.amazonaws.sfc.awss3tables.config.*
import com.amazonaws.sfc.awss3tables.config.AwsS3TablesTargetConfiguration.Companion.CONFIG_AUTO_CREATE
import com.amazonaws.sfc.awss3tables.config.AwsS3TablesWriterConfiguration.Companion.AWS_S3_TABLES
import com.amazonaws.sfc.config.BaseConfiguration
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_TARGETS
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_VALUE_FILTER
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.data.TargetData
import com.amazonaws.sfc.data.TargetResultBufferedHelper
import com.amazonaws.sfc.data.TargetResultHandler
import com.amazonaws.sfc.data.TargetWriter
import com.amazonaws.sfc.filters.ValueFiltersCache
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.metrics.*
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_DIMENSION_SOURCE_CATEGORY_TARGET
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_MESSAGES
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_RECORDS_WRITTEN
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITES
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_DURATION
import com.amazonaws.sfc.metrics.MetricsCollector.Companion.METRICS_WRITE_SUCCESS
import com.amazonaws.sfc.targets.TargetDataChannel
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.transformations.invoke
import com.amazonaws.sfc.util.MemoryMonitor
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
import org.apache.iceberg.data.Record
import org.apache.iceberg.data.parquet.GenericParquetWriter
import org.apache.iceberg.io.DataWriter
import org.apache.iceberg.parquet.Parquet
import org.apache.iceberg.types.Type
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import kotlin.time.toDuration


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

    var initialized = false


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

    private var timer = createTimer()

    private val valueFilters = ValueFiltersCache(writerConfiguration.valueFilters)

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
            val tableIdentifier = TableIdentifier.of(targetConfiguration.namespace, tableConfiguration.tableName)
            if (tables.contains(tableIdentifier)) {
                log.info("Table \"$tableIdentifier\" does exist")
            } else {
                if (targetConfiguration.autoCreate) {
                    log.info("Table \"$tableIdentifier\" does not exist, creating")
                    try {
                        tablesHelper!!.createTable(targetConfiguration.namespace, tableConfiguration.tableName, tableConfiguration.schema, tableConfiguration.partition)
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
            } else {
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
            initialized = true
        } catch (e: Exception) {
            log.error("Error setting up AWS  S3 Tables, $e")
        }

        while (isActive) {

            try {
                select {
                    timer.onJoin {
                        timer.cancel()
                        log.info("Buffer interval of ${targetConfiguration.interval} reached")
                        writeBufferedMessages()
                        timer = createTimer()
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

    private fun processTargetData(targetData: TargetData) {

        val log = logger.getCtxLoggers(className, "processTargetData")
        targetResults?.add(targetData)

        targetConfiguration.tables.forEach { table ->

            val recordCount = buildRecords(table, targetData)
            val s = if (recordCount > 1) "s" else ""
            log.trace("$recordCount record$s created from target data for table \"${table.tableName}\"")

            val totalBufferedRecords = buffers.values.sumOf { it.size }
            if (targetData.noBuffering || totalBufferedRecords >= targetConfiguration.bufferCount) {
                timer.cancel()
                log.trace("Total buffer count $totalBufferedRecords")
                writeBufferedMessages()
                timer = createTimer()
            }
        }
    }

    private fun buildRecords(table: TableConfiguration, targetData: TargetData): Int {

        val log = logger.getCtxLoggers(className, "build")

        val missingValues = mutableListOf<String>()
        val targetDataMap = targetData.toMap(writerConfiguration.elementNames, true)

        val recordsData = sequence {

            // a table can have multiple values to generate multiple records for a targetData value
            table.mappings.forEachIndexed { tableMappingIndex, tableMapping: Map<String, ColumnMappingConfiguration> ->

                val filteredByValueFilter = mutableListOf<Pair<String, Any>>()
                // get thet data for a record for each tableMapping
                val mappedRecordData = sequence {

                    // get value for every field in the schema of the table
                    table.schema.forEach { field ->

                        // get the tableMapping for the field
                        val fieldMapping = tableMapping[field.name]
                        if (fieldMapping != null) {

                            // get the value for the field
                            if (fieldMapping.subMappings.isEmpty()) {
                                // native, list or map values

                                val fieldValue = if (field.type != null) getFieldValue(targetDataMap, fieldMapping, table.tableName, field.name, field.type!!, tableMappingIndex) else null
                                if (fieldValue != null && fieldMapping.valueFilterID != null) {
                                    val valueFilter = valueFilters[fieldMapping.valueFilterID!!]
                                    if (valueFilter != null && (!valueFilter.apply(fieldValue))) {
                                        filteredByValueFilter.add(field.name to fieldValue)
                                    }
                                }

                                if (fieldValue != null) yield(field.name to fieldValue)
                                else if (!field.optional) missingValues.add(field.name)

                            } else {
                                // field is a struct value with sub-fields
                                val nestedValue = mapNestedValue(tableMapping, field, targetDataMap, table, tableMappingIndex, missingValues, filteredByValueFilter) // nested field values sequence

                                if (nestedValue.isNotEmpty()) {
                                    val rec = GenericRecord.create(buildSchema(field.subFields)).copy(nestedValue)
                                    yield(field.name to rec)

                                    // no sub-fields for a non-optional struct fields
                                } else if (!field.optional) missingValues.add(field.name)
                            }
                        } else if (!field.optional) missingValues.add(field.name)

                    } // table schema fields
                }.toMap()  // mapped records sequence

                when {
                    (filteredByValueFilter.isNotEmpty()) -> {
                        val s = if (filteredByValueFilter.size > 1) "s" else ""
                        log.trace("Table \"${table.tableName}\", tableMapping $tableMappingIndex, filtered out by $CONFIG_VALUE_FILTER$s for field$s [${filteredByValueFilter.joinToString(separator = ", ") { "\"${it.first}\" => \"${it.second}\"" }}] in target $targetID")
                    }

                    (missingValues.isNotEmpty()) -> {
                        val s = if (missingValues.size > 1) "s" else ""
                        val message =
                            "Missing required value$s for table \"${table.tableName}\" tableMapping $tableMappingIndex, field$s ${missingValues.joinToString { "\"$it\"" }} in target $targetID"
                        if (targetConfiguration.warnIfValueMissing)
                            log.warning(message)
                        else
                            log.trace(message)
                    }

                    else -> yield(mappedRecordData)
                }

            } // mappings
        }.toList()


        val tableBuffer = buffers.computeIfAbsent(table.tableName) { RecordBuffer() }

        var recordCount = 0
        recordsData.forEach { data ->
            val record = GenericRecord.create(table.catalogSchema).copy(data)
            // record = record.copy(data)
            log.trace("Created record $record")
            tableBuffer.addRecord(targetData.serial, record)
            recordCount += 1
        }
        log.trace("${tableBuffer.size} records buffered for table \"${table.tableName}\"")
        return recordCount
    }

    private fun mapNestedValue(tableMappingConfiguration: Map<String, ColumnMappingConfiguration>,
                               field: ColumnConfiguration,
                               targetDataMap: Map<String, Any>,
                               table: TableConfiguration,
                               index: Int,
                               missingValues: MutableList<String>,
                               filteredOutByFields: MutableList<Pair<String, Any>>): Map<String, Any> = sequence {
        val fieldMapping = tableMappingConfiguration[field.name]
        // for all sub-fields from the schema for the structured fields
        field.subFields.forEach { subField ->

            // get mapping for the subfield and get the value
            val subFieldMapping = fieldMapping?.subMappings?.get(subField.name)
            val subFieldValue = if (subFieldMapping != null && subField.type != null)
                getFieldValue(targetDataMap, subFieldMapping, table.tableName, "${field.name}.${subField.name}", subField.type!!, index)
            else null

            if (subFieldValue != null && subFieldMapping?.valueFilterID != null) {
                val valueFilter = valueFilters[subFieldMapping.valueFilterID!!]
                if (valueFilter != null && (!valueFilter.apply(subFieldValue))) filteredOutByFields.add("${field.name}.${subField.name}" to subFieldValue)
            }
            if (subFieldValue != null) yield(subField.name to subFieldValue)
            else if (!field.optional) missingValues.add("${field.name}.${subField.name}")

        }  // struct sub-fields
    }.toMap()

    private fun getFieldValue(targetDataMap: Map<String, Any>,
                              columnMappingConfiguration: ColumnMappingConfiguration,
                              tableName: String,
                              fieldName: String,
                              fieldType: Type,
                              index: Int): Any? {

        val queryValue = getValue(targetDataMap, columnMappingConfiguration, tableName, fieldName)

        // test if a value wat retrieved, keep list of null values for non-optional fields
        val fieldValue = if (queryValue != null) {
            val value = if (columnMappingConfiguration.transformationID == null) {
                queryValue
            } else {
                applyTransformation(queryValue, fieldName, columnMappingConfiguration.transformationID!!)
            }
            if (value == null) return null

            val fieldTypedValue = fieldType.from(value)
            if (fieldTypedValue != null) {
                fieldTypedValue
            } else {
                val log = logger.getCtxLoggers(className, "getFieldValue")
                log.warning("Value $value:${typeStr(value)} is not compatible with field type $fieldType for table \"$tableName\" mapping $index, field \"$fieldName\" in target $targetID")
                null
            }
        } else null
        return fieldValue
    }

    private fun getValue(targetDataMap: Map<String, Any>,
                         mapping: ColumnMappingConfiguration,
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


    private fun createTimer(): Job {
        return scope.launch {
            try {
                delay(targetConfiguration.interval)
            } catch (_: Exception) {
                // no harm done, timer is just used to guard for timeouts
            }
        }
    }

    private  fun writeBufferedMessages() {

        val log = logger.getCtxLoggers(className, "writeBufferedMessages")


        var recordCount: Long = 0
        var totalDuration: Duration = Duration.ZERO

        if (!initialized) {
            targetResults?.errorBuffered()
            log.error("AWS S3 Tables target \"$targetID\" not initialized")
        } else {

            buffers.forEach { tableName, tableBuffer ->
                try {

                    if (tableBuffer.size == 0) {
                        log.trace("Nu buffered records for table \"$tableName\" to write")
                    } else {

                        log.trace("Writing ${tableBuffer.records.size} buffered records for table \"$tableName\"")

                        val catalogTable = catalogTables[TableIdentifier.of(targetConfiguration.namespace, tableName)]

                        var partitionedWrites = 0

                        if (catalogTable != null) {
                            val duration = measureTime {

                                if (catalogTable.spec().isPartitioned) {
                                    val partitionedRecords = buildPartitionRecordSets(tableName, tableBuffer.records, catalogTable)
                                    partitionedWrites = partitionedRecords?.keys?.size ?: 0
                                    partitionedRecords?.forEach { (partitionData, records) ->
                                        recordCount += writeRecords(catalogTable, records, partitionData)
                                    }
                                } else {
                                    recordCount += writeRecords(catalogTable, tableBuffer.records)
                                }
                            }

                            val s = if (partitionedWrites > 1) " in $partitionedWrites partition optimized writes" else ""
                            log.info("Written ${tableBuffer.size} buffered records for table \"$tableName\"$s in $duration")
                            totalDuration += duration


                        } else throw TargetException("Table \"$tableName\" not found")

                    }

                    tableBuffer.clear()
                } catch (e: Exception) {
                    targetResults?.errorBuffered()
                    if (writer.isActive) {
                        log.error("Error writing buffered messages for table \"$tableName\", $e")
                        runBlocking { metricsCollector?.put(targetID, MetricsCollector.METRICS_WRITE_ERRORS, 1.0, MetricUnits.COUNT, metricDimensions) }
                    }
                }
            }
            targetResults?.ackBuffered()
            val messages = buffers.values.flatMap { it.serials }.toSet().size
            createMetrics(targetID, metricDimensions, recordCount, messages, totalDuration.toDouble(DurationUnit.MILLISECONDS))
        }

        buffers.values.forEach { it.clear() }
    }

    private fun writeRecords(
        catalogTable: Table,
        records: List<GenericRecord>,
        partitionData: PartitionData? = null): Int {

        if (records.isEmpty()) return 0

        val log = logger.getCtxLoggers(className, "writeRecords")

        val s = if (records.size == 1) "" else "s"
        if (partitionData != null)
            log.trace("Writing ${records.size} record$s for table \"${catalogTable.name()}\" with partition data $partitionData")
        else
            log.trace("Writing ${records.size} record$s for table \"${catalogTable.name()}\"")

        val tableWriter = buildTableWriter(catalogTable, partitionData)
        try {
            records.forEach { record -> tableWriter.write(record) }
        } finally {
            tableWriter.close()
        }

        val dataFile: DataFile? = tableWriter.toDataFile()
        catalogTable.newAppend().appendFile(dataFile).commit()
        return records.size

    }

    fun buildPartitionData(record: Record, table: Table): PartitionData {
        val spec: PartitionSpec = table.spec()
        val schema = table.schema()
        val partitionData = PartitionData(spec.partitionType())

        spec.fields().forEachIndexed { index, field ->
            val sourceId = field.sourceId()
            val source = schema.findField(sourceId)
            val sourceValue = record.getField(source.name())

            val transform = PartitionTransform.of(field.transform().toString(), source.name())

            when (transform) {
                PartitionTransform.IDENTITY -> partitionData.set(index, sourceValue)

                PartitionTransform.YEAR -> {
                    when (sourceValue) {
                        is Int -> partitionData.set(index, sourceValue)
                        is Long -> partitionData.set(index, sourceValue.toInt())
                        is Float -> partitionData.set(index, sourceValue.toInt())
                        is Double -> partitionData.set(index, sourceValue.toInt())
                        is LocalDate -> partitionData.set(index, sourceValue.year)
                        is OffsetDateTime -> partitionData.set(index, sourceValue.year)
                        is LocalDateTime -> partitionData.set(index, sourceValue.year)
                    }
                }

                PartitionTransform.MONTH -> {
                    when (sourceValue) {
                        is Int -> partitionData.set(index, sourceValue)
                        is Long -> partitionData.set(index, sourceValue.toInt())
                        is Float -> partitionData.set(index, sourceValue.toInt())
                        is Double -> partitionData.set(index, sourceValue.toInt())
                        is LocalDate -> partitionData.set(index, sourceValue.monthValue)
                        is OffsetDateTime -> partitionData.set(index, sourceValue.monthValue)
                        is LocalDateTime -> partitionData.set(index, sourceValue.monthValue)

                    }
                }

                PartitionTransform.DAY -> {
                    when (sourceValue) {
                        is Int -> partitionData.set(index, sourceValue)
                        is Long -> partitionData.set(index, sourceValue.toInt())
                        is Float -> partitionData.set(index, sourceValue.toInt())
                        is Double -> partitionData.set(index, sourceValue.toInt())
                        is LocalDate -> partitionData.set(index, sourceValue.dayOfMonth)
                        is OffsetDateTime -> partitionData.set(index, sourceValue.dayOfMonth)
                        is LocalDateTime -> partitionData.set(index, sourceValue.dayOfMonth)
                    }
                }

                PartitionTransform.HOUR -> {
                    when (sourceValue) {
                        is Int -> partitionData.set(index, sourceValue)
                        is Long -> partitionData.set(index, sourceValue.toInt())
                        is Float -> partitionData.set(index, sourceValue.toInt())
                        is Double -> partitionData.set(index, sourceValue.toInt())
                        is OffsetDateTime -> partitionData.set(index, sourceValue.hour)
                        is LocalDateTime -> {
                            partitionData.set(index, sourceValue.hour)
                        }
                    }
                }

                PartitionTransform.BUCKET -> {
                    val numBuckets = transform.param as Int
                    val hashCode = sourceValue.hashCode()
                    val bucketValue = abs(hashCode % numBuckets)
                    partitionData.set(index, bucketValue)
                }

                PartitionTransform.TRUNCATE -> {
                    val stringValue = sourceValue.toString()
                    val width = transform.param as Int
                    partitionData.set(index, stringValue.take(width))
                }

            }

        }

        return partitionData
    }

    fun buildTableWriter(table: Table, partitionData: PartitionData? = null): DataWriter<GenericRecord?> {

        val log = logger.getCtxLoggers(className, "buildTableWriter")

        val filePath = "${table.location()}/${UUID.randomUUID()}"
        val file = table.io().newOutputFile(filePath)

        return try {
            val builder = Parquet.writeData(file)
                .schema(table.schema())
                .createWriterFunc(GenericParquetWriter::buildWriter)
                .overwrite()

            if (table.spec().isPartitioned && partitionData != null) {
                builder.withSpec(table.spec())
                builder.withPartition(partitionData)
            } else {
                builder.withSpec(PartitionSpec.unpartitioned())
            }

            builder.build<GenericRecord>()

        } catch (e: Exception) {
            log.error("Error building writer for table \"${table.name()}\" using filepath \"$filePath\" , e")
            throw e
        }
    }

    private fun buildPartitionRecordSets(tableName: String,
                                         recordsForTable: List<GenericRecord>,
                                         table: Table): Map<PartitionData, List<GenericRecord>>? {


        val optimizePartitioning = targetConfiguration.tables.find { it.tableName == tableName }?.partitionOptimized ?: false
        return if (optimizePartitioning) {
            recordsForTable.map { rec ->
                rec to buildPartitionData(rec, table)
            }.groupBy { it.second }.map { group -> group.key to group.value.map { it.first } }.toMap()
        } else {
            val firstRecord = recordsForTable.first()
            mapOf(buildPartitionData(firstRecord, table) to recordsForTable)
        }
    }

    private fun searchValue(mapping: ColumnMappingConfiguration, data: Any): Any? = try {
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

    private fun createMetrics(
        adapterID: String,
        metricDimensions: MetricDimensions,
        recordCount: Long,
        messages: Int,
        writeDurationInMillis: Double
    ) {
        metricsCollector?.put(
            adapterID,
            metricsCollector?.buildValueDataPoint(adapterID, MetricsCollector.METRICS_MEMORY, MemoryMonitor.getUsedMemoryMB().toDouble(), MetricUnits.MEGABYTES),
            metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITES, 1.0, MetricUnits.COUNT, metricDimensions),
            metricsCollector?.buildValueDataPoint(adapterID, METRICS_MESSAGES, messages.toDouble(), MetricUnits.COUNT, metricDimensions),
            metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_DURATION, writeDurationInMillis, MetricUnits.MILLISECONDS, metricDimensions),
            metricsCollector?.buildValueDataPoint(adapterID, METRICS_WRITE_SUCCESS, 1.0, MetricUnits.COUNT, metricDimensions),
            metricsCollector?.buildValueDataPoint(adapterID, METRICS_RECORDS_WRITTEN, recordCount.toDouble(), MetricUnits.BYTES, metricDimensions)
        )

    }


    override suspend fun writeTargetData(targetData: TargetData) {
        targetDataChannel.submit(targetData, logger.getCtxLoggers("$className:writeTargetData"))
    }

    override suspend fun close() {
        writer.cancel()
        timer.cancel()
        credentialsWorker?.cancel()
        writeBufferedMessages()

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
