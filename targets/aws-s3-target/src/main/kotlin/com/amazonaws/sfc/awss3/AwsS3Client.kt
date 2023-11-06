
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awss3

import com.amazonaws.sfc.services.AwsServicePermissions
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse


// Abstraction for testing with mock client
@AwsServicePermissions("s3", ["PutObject"])
interface AwsS3Client {
    fun putObject(request: PutObjectRequest, body: RequestBody): PutObjectResponse
    fun close()
}