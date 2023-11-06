
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awssitewise

import software.amazon.awssdk.services.iotsitewise.IoTSiteWiseClient
import software.amazon.awssdk.services.iotsitewise.model.BatchPutAssetPropertyValueRequest
import software.amazon.awssdk.services.iotsitewise.model.BatchPutAssetPropertyValueResponse


/**
 * Wrapper for AWS Sitewise client to allow testing with mocked client
 * @param client IoTSieWiseClient Client to use to make calls to SiteWise service
 * @see <a href="https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/iotsitewise/IoTSiteWiseClient.html">IoTSiteWiseClient</a>
 */
class AwsSiteWiseClientWrapper(private val client: IoTSiteWiseClient) : AwsSiteWiseClient {
    override fun batchPutAssetPropertyValue(batchPutAssetPropertyValueRequest: BatchPutAssetPropertyValueRequest): BatchPutAssetPropertyValueResponse =
        client.batchPutAssetPropertyValue(batchPutAssetPropertyValueRequest)

    override fun close() = client.close()
}