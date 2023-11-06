
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.config

import com.google.gson.annotations.SerializedName

@ConfigurationClass
class ClientProxyConfiguration private constructor(
    proxyUrl: String? = null,
    proxyUsername: String? = null,
    proxyPassword: String? = null,
    noProxyAddresses: String? = null) {

    @SerializedName(CONFIG_PROXY_URL)
    private var _proxyUrl: String? = proxyUrl

    val proxyUrl: String?
        get() = _proxyUrl

    @SerializedName(CONFIG_PROXY_USERNAME)
    private var _proxyUsername: String? = proxyUsername

    val proxyUsername: String?
        get() = _proxyUsername

    @SerializedName(CONFIG_PROXY_PASSWORD)
    private var _proxyPassword: String? = proxyPassword

    val proxyPassword: String?
        get() = _proxyPassword

    @SerializedName(CONFIG_NO_PROXY_ADDRESSES)
    private var _noProxyAddresses: String? = noProxyAddresses

    val noProxyAddresses: String?
        get() = _noProxyAddresses

    companion object {

        const val CONFIG_PROXY_USERNAME = "ProxyUsername"
        const val CONFIG_PROXY_PASSWORD = "ProxyPassword"
        const val CONFIG_PROXY_URL = "ProxyUrl"
        const val CONFIG_NO_PROXY_ADDRESSES = "NoProxyAddresses"

        fun create(proxyUrl: String? = null,
                   proxyUsername: String? = null,
                   proxyPassword: String? = null,
                   noProxyAddresses: String? = null): ClientProxyConfiguration {

            val instance = ClientProxyConfiguration()

            with(instance) {
                _proxyUrl = proxyUrl
                _proxyUsername = proxyUsername
                _proxyPassword = proxyPassword
                _noProxyAddresses = noProxyAddresses
            }

            return instance
        }

    }
}