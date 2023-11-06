
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awslambda

import com.amazonaws.sfc.services.AwsServicePermissions
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import software.amazon.awssdk.services.lambda.model.InvokeResponse


// Abstraction for testing with mock client
@AwsServicePermissions("lambda", ["InvokeFunction"])
interface AwsLambdaClient {
    fun invoke(invokeRequest: InvokeRequest): InvokeResponse
    fun close()
}