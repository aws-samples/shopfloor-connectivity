
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsiot

import com.amazonaws.sfc.config.ClientProxyConfiguration
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File

class GreenGrass2Configuration {
    val system: Map<String, String>? = null
    val services: Map<String, Map<String, Any>>? = null

    val certificateFilePath: String by lazy {
        system?.get(DEVICE_PARAM_CERTIFICATE_FILE_PATH) ?: ""
    }

    val privateKeyPath: String by lazy {
        system?.get(DEVICE_PARAM_PRIVATE_KEY_PATH) ?: ""
    }

    val rootCaPath: String by lazy {
        system?.get(DEVICE_PARAM_ROOT_CA_PATH) ?: ""
    }

    val thingName: String by lazy {
        system?.get(DEVICE_PARAM_THING_NAME) ?: ""
    }

    private val nucleusServiceConfig: Map<*, *>? by lazy {
        services?.get(DEFAULT_NUCLEUS_COMPONENT_NAME)?.get("configuration") as Map<*, *>?

    }

    private val networkProxy: Map<*, *>? by lazy {
        nucleusServiceConfig?.get(DEVICE_NETWORK_PROXY_NAMESPACE) as Map<*, *>?
    }


    val iotCredEndpoint: String by lazy {
        nucleusServiceConfig?.get(DEVICE_PARAM_IOT_CRED_ENDPOINT) as String
    }

    val iotRoleAlias: String by lazy {
        nucleusServiceConfig?.get(IOT_ROLE_ALIAS_TOPIC) as String
    }

    val proxy: ClientProxyConfiguration?
        get() {
            val proxy = networkProxy?.get(DEVICE_PROXY_NAMESPACE) as Map<*, *>?
            if (proxy.isNullOrEmpty()) {
                return null
            }

            return ClientProxyConfiguration.create(
                proxyUrl = proxy[DEVICE_PARAM_PROXY_URL] as String?,
                proxyUsername = proxy[DEVICE_PARAM_PROXY_USERNAME] as String?,
                proxyPassword = proxy[DEVICE_PARAM_PROXY_PASSWORD] as String?,
                noProxyAddresses = networkProxy?.get(DEVICE_PARAM_NO_PROXY_ADDRESSES) as String?
            )
        }

    companion object {

        const val DEFAULT_NUCLEUS_COMPONENT_NAME = "aws.greengrass.Nucleus"
        const val DEVICE_PARAM_IOT_CRED_ENDPOINT = "iotCredEndpoint"
        const val DEVICE_PARAM_PRIVATE_KEY_PATH = "privateKeyPath"
        const val DEVICE_PARAM_CERTIFICATE_FILE_PATH = "certificateFilePath"
        const val DEVICE_PARAM_ROOT_CA_PATH = "rootCaPath"
        const val DEVICE_PARAM_THING_NAME = "thingName"
        const val DEVICE_NETWORK_PROXY_NAMESPACE = "networkProxy"
        const val IOT_ROLE_ALIAS_TOPIC = "iotRoleAlias"
        const val DEVICE_PROXY_NAMESPACE = "proxy"
        const val DEVICE_PARAM_NO_PROXY_ADDRESSES = "noProxyAddresses"
        const val DEVICE_PARAM_PROXY_URL = "url"
        const val DEVICE_PARAM_PROXY_USERNAME = "username"
        const val DEVICE_PARAM_PROXY_PASSWORD = "password"
        private const val DEFAULT_DEPLOYMENT_ROOT = "/greengrass/v2"
        private const val CONFIG_FILE = "config/effectiveConfig.yaml"

        fun load(greengrassRoot: String = DEFAULT_DEPLOYMENT_ROOT): GreenGrass2Configuration? {

            val configFile = File(listOf(greengrassRoot, CONFIG_FILE).joinToString(separator = File.separator))
            val bytes = configFile.readBytes()
            val mapper = ObjectMapper(YAMLFactory()).configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

            return mapper.readValue(bytes, GreenGrass2Configuration::class.java)

        }
    }
}