// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.awss3tables


import com.amazonaws.sfc.awss3tables.config.AwsS3TablesTargetConfiguration
import com.amazonaws.sfc.awss3tables.config.AwsS3TablesWriterConfiguration
import com.amazonaws.sfc.awss3tables.config.AwsS3TablesWriterConfiguration.Companion.AWS_S3_TABLES
import com.amazonaws.sfc.client.AwsServiceClientHelper.Companion.AWS_SERVICE_BACKOFF_MS
import com.amazonaws.sfc.client.AwsServiceClientHelper.Companion.AWS_SERVICE_RETRIES
import com.amazonaws.sfc.client.AwsServiceRetryableException
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.targets.AwsServiceTargetClientHelper
import com.amazonaws.sfc.util.BaseRetryableAccessor
import com.amazonaws.sfc.util.CrashableSupplier
import org.apache.iceberg.CatalogProperties
import org.apache.iceberg.rest.RESTCatalog

import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.awscore.internal.AwsErrorCode
import software.amazon.awssdk.services.s3tables.model.*

class AwsS3TablesHelper(private val s3TablesClient: AwsS3TablesClient, private val targetConfig : AwsS3TablesTargetConfiguration, private val logger: Logger) {

    val className = AwsS3TablesHelper::class.java.name.toString()

    fun <T> executeServiceCallWithRetries(retries: Int = AWS_SERVICE_RETRIES, backoffMs: Int = AWS_SERVICE_BACKOFF_MS, block: () -> T) =
        BaseRetryableAccessor().retry(
            tries = retries,
            initialBackoffMillis = backoffMs,
            func = CrashableSupplier<T, Exception> { block() },
            retryableExceptions = HashSet(listOf(AwsServiceRetryableException::class.java))
        )

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

    private val catalog by lazy{
        val region = targetConfig.region!!.toString()
        val tableBucketName = targetConfig.tableBucketName
        val tableBucketArn = getTableBucketArn(tableBucketName)

        val properties: MutableMap<String?, String?> = HashMap<String?, String?>()
        properties.put(CatalogProperties.CATALOG_IMPL, "org.apache.iceberg.rest.RESTCatalog")
        properties.put(CatalogProperties.URI, "https://s3tables.$region.amazonaws.com/iceberg")
        properties.put(CatalogProperties.WAREHOUSE_LOCATION, "arn:aws:s3tables:eu-west-1:816487731748:bucket/$tableBucketArn")
        properties.put(CatalogProperties.FILE_IO_IMPL, "org.apache.iceberg.aws.s3.S3FileIO")
        properties.put("rest.signing-name", "s3tables")
        properties.put("rest.signing-region", region)
        properties.put("rest.sigv4-enabled", "true")

        val catalog : RESTCatalog = RESTCatalog()
        try {
            catalog.initialize("s3tables", properties)

            val nss = catalog.listNamespaces()
            println(nss)
        }catch (e:Exception){
            println(e)
        }

    }


    fun listNameSpacesForBucketArn(tableBucketArn: String?): Set<String> {
        val log = logger.getCtxLoggers(className, "listNameSpacesForBucketArn")

        return executeServiceCallWithRetries {
            try {
                val response: ListNamespacesResponse = s3TablesClient.listNamespaces(ListNamespacesRequest.builder().tableBucketARN(tableBucketArn).build())
                response.namespaces().flatMap { it.namespace().map { it.toString() } }.toSet()
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

    fun listTablesForBucketArn(tableBucketArn: String?): Set<Pair<String, String>> {
        val log = logger.getCtxLoggers(className, "listTablesForBucketArn")
        return executeServiceCallWithRetries {
            try {
                val response: ListTablesResponse = s3TablesClient.listTables(ListTablesRequest.builder().tableBucketARN(tableBucketArn).build())
                response.tables().map { it.namespace().toSet().first() to it.name() }.toSet()
            } catch (e: AwsServiceException) {
                log.error("S3Tables:listTables error ${e.message}")
                processServiceException(e)
                emptySet()
            }
        }
    }

    fun listTablesForBucketName(tableBucketName: String): Set<Pair<String, String>> {
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

    fun listTableBucketNames(): Set<String> = listTableBuckets().map { it.name() }.toSet()

    fun listTableBucketARNS(): Set<String> = listTableBuckets().map { it.arn() }.toSet()

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

    fun createTableBucket(tableBucketName: String): String? {
        val log = logger.getCtxLoggers(className, "createTableBucket")
        return executeServiceCallWithRetries {
            try {
                val response: CreateTableBucketResponse = s3TablesClient.createTableBucket(CreateTableBucketRequest.builder().name(tableBucketName).build())
                val arn = response.arn()
                if (arn != null) tableArnBuffer[tableBucketName] = arn
                arn
            } catch (e: AwsServiceException) {
                log.error("S3Tables:createTableBucket error ${e.message}")
                processServiceException(e)
                null
            }
        }
    }

    fun createTableBucketIfNotExists(tableBucketName: String): String? {
        val arn = getTableBucketArn(tableBucketName)
        return arn ?: createTableBucket(tableBucketName)
    }

    fun createNamespace(bucketName: String, namespace: String): String? {
        val log = logger.getCtxLoggers(className, "createNamespace")
        return executeServiceCallWithRetries {
            try {
                val bucketArn = getTableBucketArn(bucketName)
                if (bucketArn == null) {
                    log.error("Bucket with name \"$bucketName\" does not exist")
                    null
                } else {
                    val response: CreateNamespaceResponse = s3TablesClient.createNamespace(CreateNamespaceRequest.builder().tableBucketARN(bucketArn).namespace(namespace).build())
                    response.namespace().first()
                }
            } catch (e: AwsServiceException) {
                log.error("S3Tables:createNamespace error ${e.message}")
                processServiceException(e)
                null
            }
        }
    }

    fun createNamespaceIfNotExists(bucketName: String, namespace: String): String? {
        val ns = listNameSpacesForBucketName(bucketName)
        return if (ns.isEmpty()) createNamespace(bucketName, namespace) else ns.first()
    }

    fun createTable(bucketName: String, namespace: String, tableName: String): String? {
        val properties: MutableMap<String?, String?> = HashMap<String?, String?>()
        properties.put(CatalogProperties.CATALOG_IMPL, "org.apache.iceberg.rest.RESTCatalog")
        properties.put(CatalogProperties.URI, "https://s3tables.eu-west-1.amazonaws.com/iceberg")
        properties.put(CatalogProperties.WAREHOUSE_LOCATION, "arn:aws:s3tables:eu-west-1:816487731748:bucket/$bucketName")
        properties.put(CatalogProperties.FILE_IO_IMPL, "org.apache.iceberg.aws.s3.S3FileIO")
        properties.put("rest.signing-name", "s3tables")
        properties.put("rest.signing-region", "eu-west-1")
        properties.put("rest.sigv4-enabled", "true")

        val catalog : RESTCatalog = RESTCatalog()
        try {
            catalog.initialize(AWS_S3_TABLES, properties)

            val nss = catalog.listNamespaces()
            println(nss)
        }catch (e:Exception){
            println(e)
        }



        return ""
    }

    companion object {

        private val S3_TABLES_BUCKET_ARN_REGEX = Regex("""^arn:aws:s3tables:[a-z]+-[a-z]+-\d:\d{12}:bucket/(.+)$""")
    }

    private val tableArnBuffer = mutableMapOf<String, String>()

    fun bucketNameFromArn(arn: String): String {
        val match = S3_TABLES_BUCKET_ARN_REGEX.matchEntire(arn)
        return match?.groupValues?.get(1) ?: ""
    }


}