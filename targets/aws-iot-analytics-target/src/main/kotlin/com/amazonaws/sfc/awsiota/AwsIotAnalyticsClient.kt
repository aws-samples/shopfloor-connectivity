
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsiota

import com.amazonaws.sfc.services.AwsServicePermissions
import software.amazon.awssdk.services.iotanalytics.model.BatchPutMessageRequest
import software.amazon.awssdk.services.iotanalytics.model.BatchPutMessageResponse


/**
 * Interface for IotAnalytics client, abstracted to allow testing with mocked client.
 */
@AwsServicePermissions("iotanalytics", ["BatchPutMessage"])
interface AwsIotAnalyticsClient {
    fun batchPutMessage(batchPutMessageRequest: BatchPutMessageRequest): BatchPutMessageResponse
    fun close()
}