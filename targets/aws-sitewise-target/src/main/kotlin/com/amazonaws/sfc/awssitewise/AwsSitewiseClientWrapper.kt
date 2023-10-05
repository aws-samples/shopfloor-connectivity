/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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