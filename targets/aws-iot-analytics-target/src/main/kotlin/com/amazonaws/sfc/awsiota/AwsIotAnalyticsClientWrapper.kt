/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.awsiota

import software.amazon.awssdk.services.iotanalytics.IoTAnalyticsClient
import software.amazon.awssdk.services.iotanalytics.model.BatchPutMessageRequest
import software.amazon.awssdk.services.iotanalytics.model.BatchPutMessageResponse

/**
 * Wrapper for AWS IotAnalytics client to allow testing with mocked client
 */
class AwsIotAnalyticsClientWrapper(private val client: IoTAnalyticsClient) : AwsIotAnalyticsClient {
    override fun batchPutMessage(batchPutMessageRequest: BatchPutMessageRequest): BatchPutMessageResponse = client.batchPutMessage(batchPutMessageRequest)
    override fun close() = client.close()
}