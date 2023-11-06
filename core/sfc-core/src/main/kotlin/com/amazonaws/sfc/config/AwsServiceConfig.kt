
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.config

import software.amazon.awssdk.regions.Region

// Common properties for configuration types for AWS target
interface AwsServiceConfig {
    val region: Region?
    val credentialProviderClient: String?
}