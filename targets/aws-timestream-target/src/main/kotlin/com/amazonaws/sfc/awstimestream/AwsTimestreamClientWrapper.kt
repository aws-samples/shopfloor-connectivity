
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awstimestream

import software.amazon.awssdk.services.timestreamwrite.TimestreamWriteClient
import software.amazon.awssdk.services.timestreamwrite.model.WriteRecordsRequest
import software.amazon.awssdk.services.timestreamwrite.model.WriteRecordsResponse

/**
 * Abstraction of Timestream client for simulation and testing.
 * Note that Timestream API requires permission to call timestream:DescribeEndpoints action
 */
class AwsTimestreamClientWrapper(private val client: TimestreamWriteClient) : AwsTimestreamClient {
    override fun writeRecords(writeRecordsRequest: WriteRecordsRequest): WriteRecordsResponse =
        client.writeRecords(writeRecordsRequest)

    override fun close() = client.close()
}