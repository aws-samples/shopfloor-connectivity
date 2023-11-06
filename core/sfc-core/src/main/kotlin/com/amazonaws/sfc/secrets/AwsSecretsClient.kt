
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.secrets

import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse

/**
 * Interface for abstracting AWS service client
 */
interface AwsSecretsClient {
    fun getSecretValue(getSecretValueRequest: GetSecretValueRequest): GetSecretValueResponse

}
