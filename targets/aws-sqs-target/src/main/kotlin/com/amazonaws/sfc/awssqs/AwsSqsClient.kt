/*
 Copyright (c) 2021. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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