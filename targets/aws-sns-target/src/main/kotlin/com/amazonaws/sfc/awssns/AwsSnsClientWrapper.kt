
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awssns

import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishBatchRequest
import software.amazon.awssdk.services.sns.model.PublishBatchResponse

/**
 * Wrapper for AWS SNS client to allow testing with mocked client
 * @param client SnsClient Client to use to make calls to SNS service
 * @see <a href="https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/sns/SnsClient.html">SnsClient</a>
 */
class AwsSnsClientWrapper(private val client: SnsClient) : AwsSnsClient {
    override fun publishBatch(publishBatchRequest: PublishBatchRequest): PublishBatchResponse = client.publishBatch(publishBatchRequest)
    override fun close() = client.close()
}