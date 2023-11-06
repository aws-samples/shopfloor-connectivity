
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsiot.http

import com.amazonaws.sfc.services.AwsServicePermissions
import software.amazon.awssdk.services.iotdataplane.model.PublishRequest
import software.amazon.awssdk.services.iotdataplane.model.PublishResponse

/**
 * Interface for AWS IoT data plane for testing with mocked client.
 * @see <a href="https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/iotdataplane/IotDataPlaneClient.html"/>IotDataPlaneClient</a>
 **/
@AwsServicePermissions("iot", ["Connect", "Publish", "DescribeEndpoint"])
interface AwsIoTDataPlaneClient {
    fun publish(publishRequest: PublishRequest): PublishResponse
    fun close()
}