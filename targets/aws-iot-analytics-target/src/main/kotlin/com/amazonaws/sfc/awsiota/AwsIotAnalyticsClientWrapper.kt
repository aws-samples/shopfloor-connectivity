
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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