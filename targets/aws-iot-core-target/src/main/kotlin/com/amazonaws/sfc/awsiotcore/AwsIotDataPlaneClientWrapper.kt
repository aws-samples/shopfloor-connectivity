
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsiotcore

import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient
import software.amazon.awssdk.services.iotdataplane.model.PublishRequest
import software.amazon.awssdk.services.iotdataplane.model.PublishResponse


class AwsIotDataPlaneClientWrapper(private val client: IotDataPlaneClient) : AwsIoTCoreDataPlaneClient {
    override fun publish(publishRequest: PublishRequest): PublishResponse = client.publish(publishRequest)
    override fun close() = client.close()
}