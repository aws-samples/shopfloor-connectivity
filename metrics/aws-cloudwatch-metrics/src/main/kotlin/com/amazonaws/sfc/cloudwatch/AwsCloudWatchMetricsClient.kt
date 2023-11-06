
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.cloudwatch

import com.amazonaws.sfc.services.AwsServicePermissions
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse


/**
 * Interface for CloudWatch client, abstracted to allow testing with mocked client.
 */
@AwsServicePermissions("cloudwatch", ["PutMetricData"])
interface AwsCloudWatchClient {
    fun putMetricData(putMetricDataRequest: PutMetricDataRequest): PutMetricDataResponse
    fun close()
}