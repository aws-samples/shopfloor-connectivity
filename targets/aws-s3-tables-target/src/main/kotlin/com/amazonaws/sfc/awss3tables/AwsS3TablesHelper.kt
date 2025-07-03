// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.awss3tables


import com.amazonaws.sfc.awss3tables.config.AwsS3TablesTargetConfiguration
import com.amazonaws.sfc.awss3tables.config.AwsS3TablesWriterConfiguration.Companion.AWS_S3_TABLES
import com.amazonaws.sfc.awss3tables.config.ColumnConfiguration
import com.amazonaws.sfc.awss3tables.config.PartitionTransform
import com.amazonaws.sfc.awss3tables.config.TablePartitionConfiguration
import com.amazonaws.sfc.client.AwsServiceClientHelper.Companion.AWS_SERVICE_BACKOFF_MS
import com.amazonaws.sfc.client.AwsServiceClientHelper.Companion.AWS_SERVICE_RETRIES
import com.amazonaws.sfc.client.AwsServiceClientHelper.Companion.SFC_USER_AGENT_PREFIX
import com.amazonaws.sfc.client.AwsServiceRetryableException
import com.amazonaws.sfc.config.AwsServiceConfig
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.targets.TargetException
import com.amazonaws.sfc.util.BaseRetryableAccessor
import com.amazonaws.sfc.util.CrashableSupplier
import org.apache.iceberg.CatalogProperties
import org.apache.iceberg.PartitionSpec
import org.apache.iceberg.Schema
import org.apache.iceberg.catalog.TableIdentifier
import org.apache.iceberg.rest.RESTCatalog
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.awscore.internal.AwsErrorCode
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption
import software.amazon.awssdk.services.s3tables.S3TablesClient
import software.amazon.awssdk.services.s3tables.model.*
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

class AwsS3TablesHelper(private val targetConfig: AwsS3TablesTargetConfiguration, private val logger: Logger) {

    val className = AwsS3TablesHelper::class.java.name.toString()

    private val s3TablesClient: AwsS3TablesClientWrapper = buildS3TableClient()

    private fun buildS3TableClient(): AwsS3TablesClientWrapper {
        val log = logger.getCtxLoggers(className, "s3TablesClient")

        val builder = S3TablesClient.builder()
        val awsServiceConfig = targetConfig as AwsServiceConfig
        val region = awsServiceConfig.region
        if (region != null) {
            log.trace("Using region $region")
            builder.region(region)
        }

        if (targetConfig.endpoint != null) {
            log.trace("Using endpoint ${awsServiceConfig.endpoint}")
            builder.endpointOverride(URI(awsServiceConfig.endpoint ?: ""))
        }

        builder.overrideConfiguration(
            ClientOverrideConfiguration.builder().advancedOptions(mutableMapOf(SdkAdvancedClientOption.USER_AGENT_PREFIX to SFC_USER_AGENT_PREFIX)).build())

        val s3TablesClient = (builder.build() as S3TablesClient)
        return AwsS3TablesClientWrapper(s3TablesClient)
    }

    fun <T> executeServiceCallWithRetries(retries: Int = AWS_SERVICE_RETRIES, backoffMs: Int = AWS_SERVICE_BACKOFF_MS, block: () -> T) = BaseRetryableAccessor().retry(
        tries = retries,
        initialBackoffMillis = backoffMs,
        func = CrashableSupplier<T, Exception> { block() },
        retryableExceptions = HashSet(listOf(AwsServiceRetryableException::class.java)))

    fun processServiceException(e: AwsServiceException) {

        // Session credentials expired, clear credentials to enforce fetching of new credentials
        if (e.awsErrorDetails().errorCode() == "ExpiredToken") {
            throw AwsServiceRetryableException(e.message)
        }

        // Other recoverable service errors
        if (AwsErrorCode.isRetryableErrorCode(e.awsErrorDetails().errorCode())) {
            throw AwsServiceRetryableException(e.message)
        }
    }

    val catalog: RESTCatalog? by lazy {

        val log = logger.getCtxLoggers(className, "catalog")

        val region = targetConfig.region!!.toString()
        val tableBucketName = targetConfig.tableBucketName
        val tableBucketArn = getTableBucketArn(tableBucketName)

        log.trace("Creating iceberg catalog for bucket $tableBucketName in region $region")

        val properties: MutableMap<String?, String?> = HashMap()
        properties.put(CatalogProperties.CATALOG_IMPL, "org.apache.iceberg.rest.RESTCatalog")
        properties.put(CatalogProperties.URI, "https://s3tables.$region.amazonaws.com/iceberg")
        properties.put(CatalogProperties.WAREHOUSE_LOCATION, "$tableBucketArn")
        properties.put(CatalogProperties.FILE_IO_IMPL, "org.apache.iceberg.aws.s3.S3FileIO")
        properties.put("rest.signing-name", "s3tables")
        properties.put("rest.signing-region", region)
        properties.put("rest.sigv4-enabled", "true")

        val catalog = RESTCatalog()
        try {
            catalog.initialize(AWS_S3_TABLES, properties)
            log.info("Iceberg catalog \"${catalog.name()}\" for warehouse \"${catalog.properties()["warehouse"]}\" created and initialized")
            catalog
        } catch (e: Exception) {
            log.error("Error creating and initializing iceberg catalog, $e")
            null
        }
    }


    fun listNameSpacesForBucketArn(tableBucketArn: String?): Set<String> {
        val log = logger.getCtxLoggers(className, "listNameSpacesForBucketArn")

        return executeServiceCallWithRetries {
            try {
                val response: ListNamespacesResponse = s3TablesClient.listNamespaces(ListNamespacesRequest.builder().tableBucketARN(tableBucketArn).build())
                response.namespaces().flatMap { it.namespace().map { ns -> ns.toString() } }.toSet()
            } catch (e: AwsServiceException) {
                log.error("S3Tables:listNamespaces error ${e.message}")
                processServiceException(e)
                emptySet()
            }
        }
    }

    fun listNameSpacesForBucketName(tableBucketName: String): Set<String> {
        val arn = getTableBucketArn(tableBucketName)
        return listNameSpacesForBucketArn(arn)
    }

    fun listTablesForBucketArn(tableBucketArn: String?): Set<TableIdentifier> {
        val log = logger.getCtxLoggers(className, "listTablesForBucketArn")
        return executeServiceCallWithRetries {
            try {
                val response: ListTablesResponse = s3TablesClient.listTables(ListTablesRequest.builder().tableBucketARN(tableBucketArn).build())
                response.tables().map { TableIdentifier.of(it.namespace().toSet().first(), it.name()) }.toSet()
            } catch (e: AwsServiceException) {
                log.error("S3Tables:listTables error ${e.message}")
                processServiceException(e)
                emptySet()
            }
        }
    }

    fun listTablesForBucketName(tableBucketName: String): Set<TableIdentifier> {
        val arn = getTableBucketArn(tableBucketName)
        return listTablesForBucketArn(arn)
    }

    fun listTableBuckets(): List<TableBucketSummary> {
        val log = logger.getCtxLoggers(className, "listTableBuckets")
        return executeServiceCallWithRetries {
            try {
                val response: ListTableBucketsResponse = s3TablesClient.listTableBuckets(ListTableBucketsRequest.builder().build())
                response.tableBuckets().forEach { tableArnBuffer[it.name()] = it.arn() }
                response.tableBuckets()
            } catch (e: AwsServiceException) {
                log.error("S3Tables:listTableBuckets error ${e.message}")
                processServiceException(e)
                emptyList()
            }
        }
    }


    fun getTableBucketArn(tableBucketName: String): String? {

        if (tableArnBuffer.containsKey(tableBucketName)) return tableArnBuffer[tableBucketName]

        val log = logger.getCtxLoggers(className, "getTableBucketArn")

        return executeServiceCallWithRetries {
            try {
                val buckets = listTableBuckets()
                val arn = buckets.firstOrNull { it.name() == tableBucketName }?.arn()
                if (arn != null) tableArnBuffer[tableBucketName] = arn
                arn
            } catch (e: AwsServiceException) {
                log.error("S3Tables:listTableBuckets error ${e.message}")
                processServiceException(e)
                null
            }
        }
    }

    fun createTableBucket(tableBucketName: String): String {
        val response: CreateTableBucketResponse = s3TablesClient.createTableBucket(CreateTableBucketRequest.builder().name(tableBucketName).build())
        val arn = response.arn()
        if (arn != null) tableArnBuffer[tableBucketName] = arn
        return arn

    }


    fun createNamespace(bucketName: String, namespace: String): String {

        val bucketArn = getTableBucketArn(bucketName)
        if (bucketArn == null) {
            throw TargetException("Bucket with name \"$bucketName\" does not exist")
        } else {
            val response: CreateNamespaceResponse = s3TablesClient.createNamespace(CreateNamespaceRequest.builder().tableBucketARN(bucketArn).namespace(namespace).build())
            return response.namespace().first()
        }

    }


    fun createTable(namespace: String, tableName: String, schema: List<ColumnConfiguration>, partitionSpec: TablePartitionConfiguration?): org.apache.iceberg.catalog.TableIdentifier {

        val tableIdentifier = TableIdentifier.of(namespace, tableName)
        val s = buildSchema(schema)
        if (partitionSpec?.transforms?.isNotEmpty() == true)
            catalog?.createTable(tableIdentifier, s, buildPartitionSpecification(s, partitionSpec)) else
            catalog?.createTable(tableIdentifier, s)
        return tableIdentifier

    }

    companion object {

        fun validateName(name: String): Pair<Boolean, String> {
            // Check if namespace is reserved
            if (name.equals("aws_s3_metadata", ignoreCase = true)) {
                return Pair(false, "Name name 'aws_s3_metadata' is reserved and cannot be used")
            }

            // Check length (1-225 characters)
            if (name.isEmpty() || name.length > 225) {
                return Pair(false, "Name must be between 1 and 225 characters long")
            }

            // Check if starts with underscore
            if (name.startsWith('_')) {
                return Pair(false, "Name name cannot start with an underscore")
            }

            // Check if starts with letter or number
            if (!name[0].isLetterOrDigit()) {
                return Pair(false, "Name name must begin with a letter or number")
            }

            // Check if ends with letter or number
            if (!name.last().isLetterOrDigit()) {
                return Pair(false, "Name name must end with a letter or number")
            }

            // Check for valid characters and forbidden characters
            val containsInvalidChars = name.any { char ->
                !char.isLowerCase() && !char.isDigit() && char != '_'
            }

            if (containsInvalidChars) {
                return Pair(false, "Name name can only contain lowercase letters, numbers, and underscores")
            }

            // Check for hyphens and periods
            if (name.contains('-') || name.contains('.')) {
                return Pair(false, "Namespace name cannot contain hyphens or periods")
            }

            return Pair(true, "")
        }

        private val tableArnBuffer = mutableMapOf<String, String>()

        private var id: AtomicInteger = AtomicInteger(0)
        fun nextId(): Int = id.incrementAndGet()

        fun buildSchema(fields: List<ColumnConfiguration>): org.apache.iceberg.Schema {
            return Schema(fields.map { it.field })

        }

        fun buildPartitionSpecification(schema: Schema, partition: TablePartitionConfiguration?): PartitionSpec? {
            if (partition == null || partition.transforms.isEmpty()) return null
            val builder = PartitionSpec.builderFor(schema)
            partition.transforms.forEach {
                when (it) {
                    PartitionTransform.IDENTITY -> builder.identity(it.source)
                    PartitionTransform.YEAR -> builder.year(it.source)
                    PartitionTransform.MONTH -> builder.month(it.source)
                    PartitionTransform.DAY -> builder.day(it.source)
                    PartitionTransform.HOUR -> builder.hour(it.source)
                    PartitionTransform.BUCKET -> if (it.param != null) builder.bucket(it.source, it.param as Int)
                    PartitionTransform.TRUNCATE -> if (it.param != null) builder.truncate(it.source, it.param as Int)
                }
            }
            return builder.build()
        }

    }

}