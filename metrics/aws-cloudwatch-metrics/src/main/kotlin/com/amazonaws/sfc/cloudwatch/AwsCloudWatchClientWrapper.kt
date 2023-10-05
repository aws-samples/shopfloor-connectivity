/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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