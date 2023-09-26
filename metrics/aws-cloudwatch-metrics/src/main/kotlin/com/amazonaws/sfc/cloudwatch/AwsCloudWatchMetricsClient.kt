/*
 Copyright (c) 2022. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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