
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsfirehose

import com.amazonaws.sfc.services.AwsServicePermissions
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest
import software.amazon.awssdk.services.firehose.model.PutRecordBatchResponse

/**
 * AWS Firehose client interface
 * @see <a href="https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/firehose/FirehoseClient.html">FirehoseClient</a>
 */
// Abstraction for testing with mock client
@AwsServicePermissions("firehose", ["PutRecordBatch"])
interface AwsFirehoseClient {
    fun putRecordBatch(putRecordBatchRequest: PutRecordBatchRequest): PutRecordBatchResponse
    fun close()
}