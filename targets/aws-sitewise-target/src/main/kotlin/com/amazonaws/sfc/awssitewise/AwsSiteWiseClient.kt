/*
 Copyright (c) 2021. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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