
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsiot.http

import software.amazon.awssdk.services.iotdataplane.IotDataPlaneClient
import software.amazon.awssdk.services.iotdataplane.model.PublishRequest
import software.amazon.awssdk.services.iotdataplane.model.PublishResponse

/**
 * Wrapper for AWS IoT data plane that abstracts the actual client to allow mocked instances for testing
 * @see <a href="https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/iotdataplane/IotDataPlaneClient.html">IotDataPlaneClient</a>
 **/
class AwsIotDataPlaneClientWrapper(private val client: IotDataPlaneClient) : AwsIoTDataPlaneClient {
    override fun publish(publishRequest: PublishRequest): PublishResponse = client.publish(publishRequest)
    override fun close() = client.close()
}