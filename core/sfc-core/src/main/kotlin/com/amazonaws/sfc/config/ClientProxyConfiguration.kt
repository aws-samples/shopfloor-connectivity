/*
 Copyright (c) 2022. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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