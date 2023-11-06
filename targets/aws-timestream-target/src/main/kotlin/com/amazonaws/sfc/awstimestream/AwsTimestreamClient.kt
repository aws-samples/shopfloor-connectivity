
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awstimestream

import com.amazonaws.sfc.services.AwsServicePermissions
import software.amazon.awssdk.services.timestreamwrite.model.WriteRecordsRequest
import software.amazon.awssdk.services.timestreamwrite.model.WriteRecordsResponse

/**
 * Abstraction of Timestream client.
 * Note that Timestream API requires permission to call timestream:DescribeEndpoints action
 */
@AwsServicePermissions("timestream", ["WriteRecords", "DescribeEndpoints"])
interface AwsTimestreamClient {
    fun writeRecords(writeRecordsRequest: WriteRecordsRequest): WriteRecordsResponse
    fun close()
}