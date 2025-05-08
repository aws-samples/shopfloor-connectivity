
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awss3tables




import software.amazon.awssdk.services.s3tables.S3TablesClient
import software.amazon.awssdk.services.s3tables.model.CreateNamespaceRequest
import software.amazon.awssdk.services.s3tables.model.CreateNamespaceResponse
import software.amazon.awssdk.services.s3tables.model.CreateTableBucketRequest
import software.amazon.awssdk.services.s3tables.model.CreateTableBucketResponse
import software.amazon.awssdk.services.s3tables.model.CreateTableRequest
import software.amazon.awssdk.services.s3tables.model.CreateTableResponse
import software.amazon.awssdk.services.s3tables.model.ListNamespacesRequest
import software.amazon.awssdk.services.s3tables.model.ListNamespacesResponse
import software.amazon.awssdk.services.s3tables.model.ListTableBucketsRequest
import software.amazon.awssdk.services.s3tables.model.ListTableBucketsResponse
import software.amazon.awssdk.services.s3tables.model.ListTablesRequest
import software.amazon.awssdk.services.s3tables.model.ListTablesResponse

// Wrapper class to allow testing with mocked s3 tables client
class AwsS3TablesClientWrapper(private val client: S3TablesClient) : AwsS3TablesClient {

    fun close() = client.close()
    override fun listNamespaces(listNamespaceRequest: ListNamespacesRequest): ListNamespacesResponse = client.listNamespaces(listNamespaceRequest)

    override fun listTables(request: ListTablesRequest): ListTablesResponse = client.listTables(request)
    override fun listTableBuckets(request: ListTableBucketsRequest): ListTableBucketsResponse  = client.listTableBuckets(request)
    override fun createTableBucket(request: CreateTableBucketRequest): CreateTableBucketResponse = client.createTableBucket(request)
    override fun createNamespace(request: CreateNamespaceRequest): CreateNamespaceResponse = client.createNamespace(request)
    override fun createTable(request: CreateTableRequest): CreateTableResponse = client.createTable(request)

}