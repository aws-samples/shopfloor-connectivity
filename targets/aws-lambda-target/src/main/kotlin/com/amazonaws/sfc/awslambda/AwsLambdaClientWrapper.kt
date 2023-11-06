
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awslambda

import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.InvokeRequest
import software.amazon.awssdk.services.lambda.model.InvokeResponse

// Wrapper class to allow testing with mocked lambda client
class AwsLambdaClientWrapper(private val client: LambdaClient) : AwsLambdaClient {
    override fun invoke(invokeRequest: InvokeRequest): InvokeResponse = client.invoke(invokeRequest)
    override fun close() = client.close()
}