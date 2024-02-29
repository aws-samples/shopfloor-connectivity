
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsiotcore

import com.amazonaws.sfc.services.AwsServicePermissions
import software.amazon.awssdk.services.iotdataplane.model.PublishRequest
import software.amazon.awssdk.services.iotdataplane.model.PublishResponse


@AwsServicePermissions("iot", ["Connect", "Publish", "DescribeEndpoint"])
interface AwsIoTCoreDataPlaneClient {
    fun publish(publishRequest: PublishRequest): PublishResponse
    fun close()
}