
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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