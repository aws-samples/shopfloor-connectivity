
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.service

import com.amazonaws.sfc.config.BaseConfiguration
import com.amazonaws.sfc.config.ConfigReader
import com.amazonaws.sfc.config.ServiceConfiguration
import com.amazonaws.sfc.data.JsonHelper.Companion.gsonExtended
import com.amazonaws.sfc.secrets.CloudSecretConfiguration
import com.amazonaws.sfc.secrets.SecretsManager

fun addExternalSecretsConfig(config: ServiceConfiguration,
                             configReader: ConfigReader,
                             outputConfigMap: MutableMap<String, Any?>) {

    // secrets as configured
    val secretsConfiguration = config.secretsManagerConfiguration ?: return

    // get all placeholders secrets that need to be resolved by an external IPC service
    val externalResolvedSecrets = getExternalSecretsConfigurations(configReader.secretsManager, gsonExtended().toJson(outputConfigMap))
    if (externalResolvedSecrets.isEmpty()) return

    // add configured secrets for placeholders
    secretsConfiguration.cloudSecrets = externalResolvedSecrets
    outputConfigMap[BaseConfiguration.CONFIG_SECRETS_MANGER] = secretsConfiguration.asConfigurationMap()

    // test if a client is configured for access to secrets manager, if so add it
    val credentialsClientId = secretsConfiguration.credentialProviderClient ?: return
    val credentialsClient = config.awsCredentialServiceClients[credentialsClientId] ?: return
    outputConfigMap[BaseConfiguration.CONFIG_AWS_IOT_CREDENTIAL_PROVIDER_CLIENTS] =
        mapOf(credentialsClientId to credentialsClient.asConfigurationMap())
}


private fun getExternalSecretsConfigurations(secretsManager: SecretsManager?, s: String): List<CloudSecretConfiguration> {

    if (secretsManager == null) {
        return emptyList()
    }

    return ConfigReader.getExternalPlaceHolders(s).mapNotNull { placeHolderName ->
        // find configured secret by name or alias from configured secrets
        val secret = secretsManager.configuredSecrets.find { secret -> secret.id == placeHolderName || secret.alias == placeHolderName }

        if (secret != null) secret else {
            // if the arn was configured and the names was used for the placeholder then lookup the arn by that name
            val resolvedSecret = secretsManager.secrets[placeHolderName]
            if (resolvedSecret != null) CloudSecretConfiguration.create(secretId = resolvedSecret) else null
        }

    }.distinctBy { it.id }
}
