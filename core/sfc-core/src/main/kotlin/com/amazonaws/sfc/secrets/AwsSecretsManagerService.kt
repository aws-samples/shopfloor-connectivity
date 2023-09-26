/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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