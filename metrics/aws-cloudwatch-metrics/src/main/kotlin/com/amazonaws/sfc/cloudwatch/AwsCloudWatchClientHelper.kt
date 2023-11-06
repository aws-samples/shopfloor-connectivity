
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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