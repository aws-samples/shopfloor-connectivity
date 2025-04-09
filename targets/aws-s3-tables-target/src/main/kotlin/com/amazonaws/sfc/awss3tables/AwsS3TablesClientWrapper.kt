
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awss3tables


import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3tables.S3TablesClient
import software.amazon.awssdk.services.s3tables.model.GetTableRequest
import software.amazon.awssdk.services.s3tables.model.GetTableResponse
//
//// Wrapper class to allow testing with mocked s3 tables client
//class AwsS3TablesClientWrapper(private val client: S3TablesClient) : AwsS3TablesClient {
//    override fun getTable(request: GetTableRequest, body: RequestBody): GetTableResponse = client.getTable(request)
//    override fun close() = client.close()
//}