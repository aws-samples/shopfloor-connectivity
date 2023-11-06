
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.cloudwatch

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse


/**
 * Wrapper for AWS CloudWatch client to allow testing with mocked client
 */
class AwsCloudWatchClientWrapper(private val client: CloudWatchClient) : AwsCloudWatchClient {
    override fun putMetricData(putMetricDataRequest: PutMetricDataRequest): PutMetricDataResponse = client.putMetricData(putMetricDataRequest)
    override fun close() = client.close()
}