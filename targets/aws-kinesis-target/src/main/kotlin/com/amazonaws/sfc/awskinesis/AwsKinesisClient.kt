
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awskinesis

import com.amazonaws.sfc.services.AwsServicePermissions
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse

/**
 * AWS Kinesis client interface, abstracted to allow testing using a mocked client
 * @see <a href="https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/kinesis/KinesisClient.html">KinesisClient</a>
 */
@AwsServicePermissions("kinesis", ["PutRecords"])
interface AwsKinesisClient {
    fun putRecords(putRecordsRequest: PutRecordsRequest): PutRecordsResponse
    fun close()
}