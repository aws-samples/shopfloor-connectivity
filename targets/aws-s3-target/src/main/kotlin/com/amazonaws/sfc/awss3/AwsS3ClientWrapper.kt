
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awss3


import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse

// Wrapper class to allow testing with mocked s3 client
class AwsS3ClientWrapper(private val client: S3Client) : AwsS3Client {
    override fun putObject(request: PutObjectRequest, body: RequestBody): PutObjectResponse = client.putObject(request, body)
    override fun close() = client.close()
}