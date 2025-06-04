
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awss3tables

import com.amazonaws.sfc.services.AwsServicePermissions
import software.amazon.awssdk.services.s3tables.model.*


// Abstraction for testing with mock client
@AwsServicePermissions("s3tables", ["ListNamespaces", "ListTables", "ListTableBuckets", "CreateTableBucket", "CreateNamespace", "CreateTable", "GetTableBucket", "GetTableData", "GetTable", "GetTableMetadataLocation", "PutTableData","UpdateTableMetadataLocation"])
interface AwsS3TablesClient {
    fun listNamespaces(listNamespaceRequest : ListNamespacesRequest): ListNamespacesResponse
    fun listTables(request: ListTablesRequest): ListTablesResponse
    fun listTableBuckets(request : ListTableBucketsRequest) : ListTableBucketsResponse
    fun getTable(request: GetTableRequest):  GetTableResponse
    fun createTableBucket( request : CreateTableBucketRequest) : CreateTableBucketResponse
    fun createNamespace(request : CreateNamespaceRequest) : CreateNamespaceResponse
    fun createTable(request : software.amazon.awssdk.services.s3tables.model.CreateTableRequest) : software.amazon.awssdk.services.s3tables.model.CreateTableResponse
}