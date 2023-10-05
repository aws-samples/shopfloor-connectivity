/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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