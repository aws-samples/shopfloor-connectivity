
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.secrets

import com.amazonaws.sfc.client.AwsServiceClientHelper
import com.amazonaws.sfc.config.AwsServiceConfig
import com.amazonaws.sfc.config.HasSecretsManager
import com.amazonaws.sfc.log.Logger
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse

/**
 * AWS Secrets Manager interface
 * @property config HasSecretsManager
 * @property logger Logger
 * @property clientHelper AwsSecretManagerClientHelper
 * @property secretsClient AwsSecretsClient
 */
class AwsSecretsManagerService(private val config: HasSecretsManager, private val logger: Logger) {

    private val className = this::class.java.simpleName

    /**
     * Internal helper to communicate with the actual service
     * @property awsService AwsServiceConfig?
     * @constructor
     */
    inner class AwsSecretManagerClientHelper(config: HasSecretsManager,
                                             logger: Logger) : AwsServiceClientHelper(config, SecretsManagerClient.builder(), logger) {

        override val awsService: AwsServiceConfig?
            get() {
                return if (config.secretsManagerConfiguration != null &&
                           (!config.secretsManagerConfiguration!!.cloudSecrets.isNullOrEmpty())) config.secretsManagerConfiguration else null
            }
    }

    //  client helper to communicate with service
    private val clientHelper = AwsSecretManagerClientHelper(config, logger)

    private val secretsClient: AwsSecretsClient
        get() = AwsSecretsClientWrapper(clientHelper.serviceClient as SecretsManagerClient)

    /**
     * Gets a secret from the service
     * @param request GetSecretValueRequest
     * @return GetSecretValueResponse
     */
    fun getSecret(request: GetSecretValueRequest): GetSecretValueResponse {

        val log = logger.getCtxLoggers(className, "getSecretValue")

        return try {
            clientHelper.executeServiceCallWithRetries {
                try {
                    secretsClient.getSecretValue(request)
                } catch (e: AwsServiceException) {
                    clientHelper.processServiceException(e)
                    throw e
                }
            }
        } catch (e: Throwable) {
            log.error("SecretsManager getSecretValue error for value \"$request.secretArn\",  ${e.message}")
            throw e
        }

    }

}