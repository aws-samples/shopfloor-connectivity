/*

Copyright (c) 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved. 
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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