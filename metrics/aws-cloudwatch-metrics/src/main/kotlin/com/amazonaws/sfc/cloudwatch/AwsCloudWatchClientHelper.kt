/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.cloudwatch

import com.amazonaws.sfc.client.AwsServiceClientHelper
import com.amazonaws.sfc.cloudwatch.config.AwsCloudWatchConfiguration
import com.amazonaws.sfc.config.AwsServiceConfig
import com.amazonaws.sfc.config.BaseConfiguration
import com.amazonaws.sfc.log.Logger
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient

// Helper class for interacting with AWS CloudWatch service
class AwsCloudWatchClientHelper(config: BaseConfiguration,
                                private val awsCloudWatchConfig: AwsCloudWatchConfiguration,
                                logger: Logger) : AwsServiceClientHelper(config, CloudWatchClient.builder(), logger) {

    override val awsService: AwsServiceConfig
        get() {
            return awsCloudWatchConfig
        }
}