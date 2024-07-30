// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awssitewise

import com.amazonaws.sfc.services.AwsServicePermissions
import software.amazon.awssdk.services.iotsitewise.model.*
import software.amazon.awssdk.services.iotsitewise.paginators.ListAssetModelsIterable
import software.amazon.awssdk.services.iotsitewise.paginators.ListAssetsIterable

/**
 * Interface for SiteWise client, abstracted to allow testing with mocked client.
 * @see <a href="https://https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/iotsitewise/IoTSiteWiseClient.html">IotSiteWiseClient</a>
 */
@AwsServicePermissions(
    "iotsitewise", [
        "BatchPutAssetPropertyValue",
        "CreateAsset",
        "CreateAssetModel",
        "DescribeAsset",
        "DescribeAssetModel",
        "DescribeEndpoint",
        "ListAssetModelProperties",
        "ListAssets",
        "UpdateAssetModel",
        "UpdateAssetProperty",
        "TagResource"
    ]
)
interface AwsSiteWiseClient {
    fun batchPutAssetPropertyValue(batchPutAssetPropertyValueRequest: BatchPutAssetPropertyValueRequest): BatchPutAssetPropertyValueResponse
    fun listAssetModelsPaginator(listAssetModelsPagRequest: ListAssetModelsRequest): ListAssetModelsIterable
    fun describeAssetModel(describeAssetModelRequest: DescribeAssetModelRequest): DescribeAssetModelResponse
    fun listAssetsPaginator(listAssetRequest: ListAssetsRequest): ListAssetsIterable
    fun describeAsset(describeAssetRequest: DescribeAssetRequest): DescribeAssetResponse
    fun createAsset(createAssetRequest: CreateAssetRequest): CreateAssetResponse
    fun updateAssetModel(updateAssetModelRequest: UpdateAssetModelRequest): UpdateAssetModelResponse
    fun createAssetModel(createAssetModelRequest: CreateAssetModelRequest): CreateAssetModelResponse
    fun updateAssetProperty(updateAssetPropertyRequest: UpdateAssetPropertyRequest): UpdateAssetPropertyResponse
    fun close()
}