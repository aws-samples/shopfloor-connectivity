
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.client

import com.amazonaws.sfc.awsiot.AwsIoTCredentialSessionProvider
import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.config.AwsServiceConfig
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CREDENTIAL_PROVIDER_CLIENT
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.HasCredentialClients
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.util.BaseRetryableAccessor
import com.amazonaws.sfc.util.CrashableSupplier
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder
import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.awscore.internal.AwsErrorCode
import software.amazon.awssdk.core.SdkClient
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption


// Internal exception class used to indicate that failed execution of a code block making the AWS
// service calls should be retried.
internal class AwsServiceRetryableException(message: String?) : Exception(message)

abstract class AwsServiceClientHelper(
    private val config: HasCredentialClients,
    private val builder: AwsClientBuilder<*, *>,
    private val logger: Logger) {

    // Gets the configuration for the target from the configuration
    protected abstract val awsService: AwsServiceConfig?

    // Gets the configuration for the client that accesses the IoT Credentials service to obtain session credentials. Returns null is no such
    // client is configured for the target.
    private val credentialClientConfig: AwsIotCredentialProviderClientConfiguration?
        get() {
            if (!awsService?.credentialProviderClient.isNullOrEmpty()) {
                val cc = config
                return cc.awsCredentialServiceClients[awsService?.credentialProviderClient]
                       ?: throw ConfigurationException("Configuration for client \"${awsService?.credentialProviderClient}\" does not exist, configured clients are ${config.awsCredentialServiceClients.keys}", CONFIG_CREDENTIAL_PROVIDER_CLIENT)
            }
            return null
        }


    // Credentials provider using IoT Credentials Service
    private var _credentialsProvider: AwsIoTCredentialSessionProvider? = null

    private fun getCredentialsProvider(): AwsCredentialsProvider? {
        // Only if client is configured for the target
        if ((credentialClientConfig != null) && (_credentialsProvider == null)) {
            _credentialsProvider = AwsIoTCredentialSessionProvider(credentialClientConfig, logger)
            return _credentialsProvider
        }
        return null
    }


    /**
     * Tests if an AwsServiceException is caused by an error that might be recoverable. If this is the case then the AwsServiceRetryableException
     * is thrown as an indication a retry can be executed. This method is intended to be used by code blocks executed by the executeServiceCallWithRetries
     * method.
     * If the error is a result of expired credentials, the cached credentials in the helpers credentials provider instance, which acts as a credentials-provider
     * for the service client, will be cleared and new credentials will be fetched from the IoT Credentials Service if a client was configured as
     * part of the target configuration.
     * @see executeServiceCallWithRetries
     * @param e AwsServiceException Exception to be tested.
     */
    fun processServiceException(e: AwsServiceException) {
        // Session credentials expired, clear credentials to enforce fetching of new credentials
        if (e.awsErrorDetails().errorCode() == "ExpiredToken") {
            _credentialsProvider?.clearCredentials()
            throw AwsServiceRetryableException(e.message)
        }

        // Other recoverable service errors
        if (AwsErrorCode.isRetryableErrorCode(e.awsErrorDetails().errorCode())) {
            throw AwsServiceRetryableException(e.message)
        }
    }


    /**
     * Builds a configured service client.
     */
    val serviceClient: SdkClient by lazy {

        val region = awsService?.region
        if (region != null) {
            builder.region(region)
        }

        val clientCredentialsProvider = getCredentialsProvider()
        if (clientCredentialsProvider != null) {
            builder.credentialsProvider(clientCredentialsProvider)
        }


        builder.overrideConfiguration(ClientOverrideConfiguration.builder()
            .advancedOptions(mutableMapOf(SdkAdvancedClientOption.USER_AGENT_PREFIX to SFC_USER_AGENT_PREFIX))
            .build())


        builder.build() as SdkClient

    }


    /**
     * Executes a block of code that calls AWS Service API calls. If an exception is thrown from the code block of type AwsServiceRetryableException if a recoverable
     * error has occurred, execution of the code block will be retried. See processServiceException which implements the logic to determine if an error could be recoverable,
     * and in that case throws the AwsServiceRetryableException exception.
     * @see processServiceException
     * @param block Function0<T> Code to be executed
     * @param retries Int Number of retries
     * @param backoffMs Int Backoff period in ms. The time in between the retries is backoffMs * retry attempt
     * @return T? Type of the result returned by the code block
     */
    fun <T> executeServiceCallWithRetries(retries: Int = AWS_SERVICE_RETRIES, backoffMs: Int = AWS_SERVICE_BACKOFF_MS, block: () -> T) =
        BaseRetryableAccessor().retry(
            tries = retries,
            initialBackoffMillis = backoffMs,
            func = CrashableSupplier<T, Exception> { block() },
            retryableExceptions = HashSet(listOf(AwsServiceRetryableException::class.java))
        )


    companion object {
        private const val AWS_SERVICE_RETRIES = 5
        private const val AWS_SERVICE_BACKOFF_MS = 500
        private const val SFC_USER_AGENT_PREFIX = "AWS-SFC/"

    }
}