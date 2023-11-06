
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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