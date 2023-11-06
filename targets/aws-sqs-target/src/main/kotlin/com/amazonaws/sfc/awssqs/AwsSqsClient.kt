
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awssqs

import com.amazonaws.sfc.services.AwsServicePermissions
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse

/**
 * Interface for SQS client, abstracted to allow testing with mocked client.
 * @see <a href="https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/sqs/SqsClient.html">SqsClient</a>
 */
@AwsServicePermissions("sqs", ["SendMessage"])
interface AwsSqsClient {
    fun sendMessageBatch(sendMessageBatchRequest: SendMessageBatchRequest): SendMessageBatchResponse
    fun close()
}