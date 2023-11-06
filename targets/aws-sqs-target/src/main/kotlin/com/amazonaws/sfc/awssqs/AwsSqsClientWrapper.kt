
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awssqs

import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse

/**
 * Wrapper for AWS SQS client to allow testing with mocked client
 * @param client SqsClient Client to use to make calls to SQS service
 * @see <a href="https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/sqs/SqsClient.html">SqsClient</a>
 */
class AwsSqsClientWrapper(private val client: SqsClient) : AwsSqsClient {
    override fun sendMessageBatch(sendMessageBatchRequest: SendMessageBatchRequest): SendMessageBatchResponse = client.sendMessageBatch(sendMessageBatchRequest)
    override fun close() = client.close()
}