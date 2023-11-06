
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awssitewise

import com.amazonaws.sfc.services.AwsServicePermissions
import software.amazon.awssdk.services.iotsitewise.model.BatchPutAssetPropertyValueRequest
import software.amazon.awssdk.services.iotsitewise.model.BatchPutAssetPropertyValueResponse

/**
 * Interface for SiteWise client, abstracted to allow testing with mocked client.
 * @see <a href="https://https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/iotsitewise/IoTSiteWiseClient.html">IotSiteWiseClient</a>
 */
@AwsServicePermissions("iotsitewise", ["BatchPutAssetPropertyValue", "DescribeEndpoint"])
interface AwsSiteWiseClient {
    fun batchPutAssetPropertyValue(batchPutAssetPropertyValueRequest: BatchPutAssetPropertyValueRequest): BatchPutAssetPropertyValueResponse
    fun close()
}