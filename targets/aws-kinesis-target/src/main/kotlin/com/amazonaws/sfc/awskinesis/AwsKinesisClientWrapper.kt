/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.awskinesis

import software.amazon.awssdk.services.kinesis.KinesisClient
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse

/**
 * Wrapper for Kinesis client to test with mocked Kinesis client
 * @param client Client to communicate with the Kinesis service
 * @see <a href="https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/kinesis/KinesisClient.html">KinesisClient</a>
 */
class AwsKinesisClientWrapper(private val client: KinesisClient) : AwsKinesisClient {

    override fun putRecords(putRecordsRequest: PutRecordsRequest): PutRecordsResponse = client.putRecords(putRecordsRequest)

    override fun close() = client.close()
}