// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.awss3tables

import software.amazon.awssdk.services.s3tables.S3TablesClient
import software.amazon.awssdk.services.s3tables.model.ListNamespacesRequest
import software.amazon.awssdk.services.s3tables.model.ListNamespacesResponse

class S3TableHelper(private val s3TableClient : AwsS3TablesClient) {
    fun listNameSpaces () : List<String> {
        val response : ListNamespacesResponse = s3TableClient.listNamespaces(ListNamespacesRequest.builder().build())
        return response.namespaces().map { it.toString() }
    }
}