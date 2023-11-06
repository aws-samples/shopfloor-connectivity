
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awssns

import com.amazonaws.sfc.services.AwsServicePermissions
import software.amazon.awssdk.services.sns.model.PublishBatchRequest
import software.amazon.awssdk.services.sns.model.PublishBatchResponse

/**
 * Interface for SNS client, abstracted to allow testing with mocked client.
 * @see <a href="https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/sns/SnsClient.html">SnsClient</a>
 */
@AwsServicePermissions("sns", ["Publish"])
interface AwsSnsClient {
    fun publishBatch(publishBatchRequest: PublishBatchRequest): PublishBatchResponse
    fun close()
}