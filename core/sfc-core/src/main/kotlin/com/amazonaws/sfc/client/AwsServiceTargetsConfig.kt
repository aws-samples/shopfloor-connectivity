
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.client

import com.amazonaws.sfc.config.AwsServiceConfig

// Common properties for configuration types for AWS targets
interface AwsServiceTargetsConfig<T : AwsServiceConfig> {
    val targets: Map<String, T>
}