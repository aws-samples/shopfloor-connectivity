
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsiot

import com.amazonaws.sfc.data.JsonHelper.Companion.fromJsonExtended
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.system.DateTime.systemDateTimeUTC
import com.amazonaws.sfc.util.ItemCacheHandler
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import java.time.Instant
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class AwsIoTCredentialSessionProvider(config: AwsIotCredentialProviderClientConfiguration?, private val logger: Logger) :
        AwsCredentialsProvider, ItemCacheHandler<AwsCachedCredentials, AwsIotCredentialProviderClientConfiguration>(

    initializer = { _, _ ->

        try {
            if (config == null) {
                null
            } else {
                logger.getCtxTraceLog(this::class.java.simpleName, "")("Getting new session credentials for using client configuration $config")
                getCredentials(config, logger)
            }
        } catch (e: Throwable) {
            throw AWSIotException("Error getting credentials , ${e.message}:${e.cause}")
        }
    },

    isValid = { credentials ->
        if (config?.skipCredentialsExpiryCheck == true) {
            true
        } else {
            val (checkedTime, credentialsStillValid) = areCredentialsValid(config?.expiryClockSkewSeconds, credentials)
            if (!credentialsStillValid) {
                logger.getCtxInfoLog(this::class.java.simpleName, "")("Session with expiry time ${credentials?.expiry} is older than $checkedTime, requesting new session credentials")
            }
            credentialsStillValid
        }
    }) {


    companion object {

        private const val IOT_CREDENTIALS_PATH_ROLE_ALIASES = "role-aliases"
        private const val IOT_CREDENTIALS_PATH_ROLE_CREDENTIALS = "credentials"
        private const val IOT_CREDENTIALS_HTTP_VERB = "GET"
        private const val IOT_CREDENTIALS = "credentials"
        private const val IOT_ACCESS_KEY_ID = "accessKeyId"
        private const val IOT_SECRET_ACCESS_KEY = "secretAccessKey"
        private const val IOT_CREDENTIALS_EXPIRATION = "expiration"
        private const val IOT_SESSION_TOKEN = "sessionToken"

        private val className = this::class.java.simpleName

        private fun getCredentials(config: AwsIotCredentialProviderClientConfiguration, logger: Logger): AwsCachedCredentials {

            val item = AwsCachedCredentials()

            val log = logger.getCtxLoggers(className, "getCredentials")

            val connectionManager = IotConnectionManager(config)
            val iotCloudHelper = IotCloudHelper()

            val credentialPath =
                "/$IOT_CREDENTIALS_PATH_ROLE_ALIASES/${config.roleAlias}/$IOT_CREDENTIALS_PATH_ROLE_CREDENTIALS"

            log.info("Request session credentials from IoT credentials Service for thing \"${config.thingName}\"")
            val cloudResponse = iotCloudHelper.sendHttpRequest(
                connectionManager,
                config.thingName,
                credentialPath,
                IOT_CREDENTIALS_HTTP_VERB,
                null
            )

            item.responseCode = cloudResponse.statusCode
            item.expiry = systemDateTimeUTC()

            if (item.responseCode == 200) {
                val awsCredentials = translateToAwsSdkFormat(cloudResponse.toString())
                item.credentials = awsCredentials.first
                item.expiry = awsCredentials.second
                log.info(
                    "Credentials received, session token is \"${
                        item.credentials?.sessionToken()?.substring(0, 16)
                    }....\" (truncated), expiration time is ${item.expiry}"
                )
            } else {
                log.error("No credentials received, response code is ${item.responseCode}")
                item.expiry = Instant.now()
            }

            return item
        }

        private fun areCredentialsValid(
            expiryClockSkewSeconds: Int?, credentials: AwsCachedCredentials?
        ): Pair<Instant, Boolean> {
            val checkedTime = systemDateTimeUTC().plusSeconds(expiryClockSkewSeconds?.toLong() ?: 0)
            val valid = credentials?.credentials != null && checkedTime.isBefore(credentials.expiry)
            return Pair(checkedTime, valid)
        }

        private fun translateToAwsSdkFormat(credentialStr: String): Pair<AwsSessionCredentials?, Instant?> {

            return try {
                val map = fromJsonExtended(credentialStr, Map::class.java)

                val credentials = map[IOT_CREDENTIALS] as Map<*, *>?
                                  ?: throw (AWSIotException("No $IOT_CREDENTIALS element found in response"))

                val accessKey = credentials[IOT_ACCESS_KEY_ID] as String?
                                ?: throw AWSIotException("No expiry $IOT_ACCESS_KEY_ID in $IOT_CREDENTIALS element")

                val secretAccessKey = credentials[IOT_SECRET_ACCESS_KEY] as String?
                                      ?: throw AWSIotException("No expiry $IOT_SECRET_ACCESS_KEY in $IOT_CREDENTIALS element")

                val sessionToken = credentials[IOT_SESSION_TOKEN] as String?
                                   ?: throw AWSIotException("No expiry $IOT_SESSION_TOKEN in $IOT_CREDENTIALS element")

                val awsSessionCredentials = AwsSessionCredentials.create(accessKey, secretAccessKey, sessionToken)

                val expiration = Instant.parse(credentials[IOT_CREDENTIALS_EXPIRATION] as String?)
                                 ?: throw AWSIotException("No expiry $IOT_CREDENTIALS_EXPIRATION in $IOT_CREDENTIALS element")

                Pair(awsSessionCredentials, expiration)
            } catch (e: Exception) {
                throw AWSIotException(e)
            }
        }

    }

    fun clearCredentials() {
        runBlocking {
            clear()
        }
    }

    override fun resolveCredentials(): AwsCredentials {
        try {
            val c: AwsCredentials? =
                runBlocking<AwsCredentials?> {
                    return@runBlocking withTimeout(10000.toDuration(DurationUnit.SECONDS)) {
                        getAsync().await()?.credentials
                    }
                }
            return c ?: throw AWSIotException("Error resolving credentials")
        } catch (e: TimeoutCancellationException) {
            throw AWSIotException("Timeout resolving IoT Credential Service credentials")
        } catch (e: Exception) {
            throw e
        }
    }

}

