
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.secrets

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse


/**
 * Wrapper for AWS service client, enables unit testing
 * @property client SecretsManagerClient
 * @constructor
 */
class AwsSecretsClientWrapper(private val client: SecretsManagerClient) : AwsSecretsClient {
    override fun getSecretValue(getSecretValueRequest: GetSecretValueRequest): GetSecretValueResponse = client.getSecretValue(getSecretValueRequest)
}