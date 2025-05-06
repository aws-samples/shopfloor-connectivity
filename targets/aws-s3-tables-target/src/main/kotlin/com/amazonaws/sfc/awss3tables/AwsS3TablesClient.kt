
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awss3tables

import com.amazonaws.sfc.services.AwsServicePermissions
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3tables.model.GetTableRequest
import software.amazon.awssdk.services.s3tables.model.GetTableResponse
import software.amazon.awssdk.services.s3tables.model.ListNamespacesRequest
import software.amazon.awssdk.services.s3tables.model.ListNamespacesResponse


// Abstraction for testing with mock client
@AwsServicePermissions("s3tables", ["ListNamespaces"])
interface AwsS3TablesClient {
    fun listNamespaces(listNamespaceRequest : ListNamespacesRequest): ListNamespacesResponse
 //   fun getTable(request: GetTableRequest): GetTableResponse
//    fun getTable(request: GetTableRequest, body: RequestBody): GetTableResponse
//    fun close()
}