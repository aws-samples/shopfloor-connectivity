
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awssitewise

import software.amazon.awssdk.services.iotsitewise.IoTSiteWiseClient
import software.amazon.awssdk.services.iotsitewise.model.*
import software.amazon.awssdk.services.iotsitewise.paginators.ListAssetModelsIterable
import software.amazon.awssdk.services.iotsitewise.paginators.ListAssetsIterable


/**
 * Wrapper for AWS Sitewise client to allow testing with mocked client
 * @param client IoTSieWiseClient Client to use to make calls to SiteWise service
 * @see <a href="https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/iotsitewise/IoTSiteWiseClient.html">IoTSiteWiseClient</a>
 */
class AwsSiteWiseClientWrapper(private val client: IoTSiteWiseClient) : AwsSiteWiseClient {
    override fun batchPutAssetPropertyValue(batchPutAssetPropertyValueRequest: BatchPutAssetPropertyValueRequest): BatchPutAssetPropertyValueResponse =
        client.batchPutAssetPropertyValue(batchPutAssetPropertyValueRequest)

    override fun listAssetModelsPaginator(listAssetModelsPagRequest: ListAssetModelsRequest): ListAssetModelsIterable =
        client.listAssetModelsPaginator(listAssetModelsPagRequest)

    override fun describeAssetModel(describeAssetModelRequest: DescribeAssetModelRequest): DescribeAssetModelResponse =
        client.describeAssetModel(describeAssetModelRequest)

    override fun listAssetsPaginator(listAssetRequest: ListAssetsRequest): ListAssetsIterable =
        client.listAssetsPaginator(listAssetRequest)

    override fun describeAsset(describeAssetRequest: DescribeAssetRequest): DescribeAssetResponse =
        client.describeAsset(describeAssetRequest)

    override fun createAsset(createAssetRequest: CreateAssetRequest): CreateAssetResponse =
        client.createAsset(createAssetRequest)

    override fun updateAssetModel(updateAssetModelRequest: UpdateAssetModelRequest): UpdateAssetModelResponse =
        client.updateAssetModel(updateAssetModelRequest)

    override fun createAssetModel(createAssetModelRequest: CreateAssetModelRequest): CreateAssetModelResponse =
        client.createAssetModel(createAssetModelRequest)

    override fun updateAssetProperty(updateAssetPropertyRequest: UpdateAssetPropertyRequest): UpdateAssetPropertyResponse =
        client.updateAssetProperty(updateAssetPropertyRequest)


    override fun close() = client.close()
}