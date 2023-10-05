/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.awsfirehose

import software.amazon.awssdk.services.firehose.FirehoseClient
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest
import software.amazon.awssdk.services.firehose.model.PutRecordBatchResponse

/**
 * Wrapper for firehose client class to allow testing with mocked service client
 * @property client FirehoseClient
 * @see <a href="https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/firehose/FirehoseClient.html">FirehoseClient</a>

 */
class AwsFirehoseClientWrapper(private val client: FirehoseClient) : AwsFirehoseClient {
    override fun putRecordBatch(putRecordBatchRequest: PutRecordBatchRequest): PutRecordBatchResponse =
        client.putRecordBatch(putRecordBatchRequest)

    override fun close() = client.close()
}