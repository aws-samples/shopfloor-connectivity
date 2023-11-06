
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.util

import com.amazonaws.sfc.config.ClientProxyConfiguration
import software.amazon.awssdk.crt.http.HttpProxyOptions
import software.amazon.awssdk.http.SdkHttpClient
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.http.apache.ProxyConfiguration
import java.net.URI

object ProxyUtils {

    private var proxyUrl: String? = null
    private var proxyUsername: String? = null
    private var proxyPassword: String? = null
    private var proxyNoProxyAddresses: String? = null

    /**
     * Returns `scheme` from the user provided proxy url of the format `scheme://user:pass@host:port`.
     *
     * `scheme` is required and must be one of `http`, `https`, or `socks5`
     *
     * @param url User provided URL value from config
     * @return `scheme` in `scheme://user:pass@host:port`
     */
    fun getSchemeFromProxyUrl(url: String): String {
        return URI.create(url).scheme
    }

    /**

     * Returns `user:pass` from the user provided proxy url of the format `scheme://user:pass@host:port`.
     *
     * `user:pass` are optional
     *
     * @param url User provided URL value from config
     * @return `user:pass` in `scheme://user:pass@host:port` or `null` if absent
     */
    fun getAuthFromProxyUrl(url: String): String? {
        return URI.create(url).userInfo
    }

    /**

     * Returns `host` from the user provided proxy url of the format `scheme://user:pass@host:port`.
     *
     * `host` is required
     *
     * @param url User provided URL value from config
     * @return `host` in `scheme://user:pass@host:port`
     */
    fun getHostFromProxyUrl(url: String): String {
        return URI.create(url).host
    }

    private fun getDefaultPortForSchemeFromProxyUrl(url: String): Int {
        return when (getSchemeFromProxyUrl(url)) {
            "http" -> 80
            "https" -> 443
            "socks5" -> 1080
            else -> -1
        }
    }

    /**
     * Returns `port` from the user provided proxy url of the format
     * `scheme://user:pass@host:port`.
     *
     * `port` is optional. If not provided, returns 80 for http, 443 for https, 1080 for socks5, or -1 for any other scheme.
     *
     * @param url User provided URL value from config
     * @return `port` in `scheme://user:pass@host:port` or the default for the
     * `scheme`, -1 if `scheme` isn't recognized
     */
    fun getPortFromProxyUrl(url: String): Int {
        val userProvidedPort = URI.create(url).port
        return if (userProvidedPort != -1) {
            userProvidedPort
        } else getDefaultPortForSchemeFromProxyUrl(url)
    }

    /**
     * If the username is provided in the proxy url (i.e. `user` in `scheme://user:pass@host:port`), it is always returned.
     *
     * If the username is not provided in the proxy url, then the username config property is returned.
     *
     * @param proxyUrl User specified proxy url
     * @param proxyUsername User specified proxy username
     * @return Username field for proxy basic authentication or null if not found in url or username config topics
     */
    fun getProxyUsername(proxyUrl: String, proxyUsername: String?): String? {
        val auth = getAuthFromProxyUrl(proxyUrl)
        if (!auth.isNullOrEmpty()) {
            val tokens = auth.split(":").toTypedArray()
            return tokens[0]
        }
        return if (proxyUsername.isNullOrEmpty()) {
            null
        } else {
            proxyUsername
        }
    }

    /**
     * If the password is provided in the proxy url (i.e. `pass` in `scheme://user:pass@host:port`), it is always returned.
     *
     * If the password is not provided in the proxy url, then the password config property is returned.
     *
     * @param proxyUrl User specified proxy url
     * @param proxyPassword User specified proxy password
     * @return Password field for proxy basic authentication or null if not found in url or password config topics
     */
    fun getProxyPassword(proxyUrl: String, proxyPassword: String?): String? {
        val auth = getAuthFromProxyUrl(proxyUrl)
        if (!auth.isNullOrEmpty()) {
            val tokens = auth.split(":").toTypedArray()
            if (tokens.size > 1) {
                return tokens[1]
            }
        }
        return if (!proxyPassword.isNullOrEmpty()) {
            proxyPassword
        } else null
    }

    /**
     * Provides a software.amazon.awssdk.crt.http.HttpProxyOptions object that can be used when building various
     * CRT library clients (like mqtt and http)
     *
     * @return httpProxyOptions containing user proxy settings, if specified. If not, httpProxyOptions is null.
     */
    fun getHttpProxyOptions(proxyConfiguration: ClientProxyConfiguration?): HttpProxyOptions? {

        if (proxyConfiguration == null) {
            return null
        }

        val proxyUrl = proxyConfiguration.proxyUrl
        if (proxyUrl.isNullOrEmpty()) {
            return null
        }

        val httpProxyOptions = HttpProxyOptions()

        httpProxyOptions.host = getHostFromProxyUrl(proxyUrl)
        httpProxyOptions.port = getPortFromProxyUrl(proxyUrl)
        val proxyUsername = getProxyUsername(proxyUrl, proxyConfiguration.proxyUsername)
        if (!proxyUsername.isNullOrEmpty()) {
            httpProxyOptions.authorizationType = HttpProxyOptions.HttpProxyAuthorizationType.Basic
            httpProxyOptions.authorizationUsername = proxyUsername
            httpProxyOptions.authorizationPassword = getProxyPassword(proxyUrl, proxyConfiguration.proxyPassword)
        }
        return httpProxyOptions
    }

    /**
     * Sets static proxy values to support easy client construction.
     *
     * @param clientProxyConfiguration contains user specified system proxy values
     */
    fun setProxyProperties(clientProxyConfiguration: ClientProxyConfiguration?) {

        proxyUrl = clientProxyConfiguration?.proxyUrl
        proxyUsername = clientProxyConfiguration?.proxyUsername
        proxyPassword = clientProxyConfiguration?.proxyPassword
        proxyNoProxyAddresses = clientProxyConfiguration?.noProxyAddresses
    }

    /**
     *
     * Boilerplate for providing a proxy configured ApacheHttpClient to AWS SDK v2 client builders.
     *
     * If you need to customize the HttpClient, but still need proxy support, use `ProxyUtils.getProxyConfiguration()`
     *
     * @return httpClient built with a ProxyConfiguration or null if no proxy is configured (null is ignored in AWS
     * SDK clients)
     */
    val sdkHttpClient: SdkHttpClient
        get() = sdkHttpClientBuilder.build()

    /**
     * Boilerplate for providing a proxy configured ApacheHttpClient builder to AWS SDK v2 client builders.
     *
     * If you need to customize the HttpClient, but still need proxy support, use `ProxyUtils.getProxyConfiguration()`
     *
     * @return httpClient built with a ProxyConfiguration or null if no proxy is configured (null is ignored in AWS
     * SDK clients)
     */
    val sdkHttpClientBuilder: ApacheHttpClient.Builder
        get() {
            val proxyConfiguration = proxyConfiguration
            return if (proxyConfiguration != null) {
                ApacheHttpClient.builder().proxyConfiguration(proxyConfiguration)
            } else ApacheHttpClient.builder()
        }

    private fun removeAuthFromProxyUrl(proxyUrl: String): String {
        val uri = URI.create(proxyUrl)
        val sb = StringBuilder()
        sb.append(uri.scheme).append("://").append(uri.host)
        if (uri.port != -1) {
            sb.append(':').append(uri.port)
        }
        return sb.toString()
    }

    private fun addAuthToProxyUrl(proxyUrl: String, username: String?, password: String?): String {
        val uri = URI.create(proxyUrl)
        val sb = StringBuilder()
        sb.append(uri.scheme).append("://").append(username).append(':').append(password).append('@')
            .append(uri.host)
        if (uri.port != -1) {
            sb.append(':').append(uri.port)
        }
        return sb.toString()
    } // ProxyConfiguration throws an error if auth data is included in the url

    /**
     *
     * Boilerplate for providing a `ProxyConfiguration` to AWS SDK v2 `ApacheHttpClient`s.
     *
     * @return ProxyConfiguration built with user proxy values or null if no proxy is configured (null is ignored in
     * the SDK)
     */
    val proxyConfiguration: ProxyConfiguration?
        get() {
            if (proxyUrl.isNullOrEmpty()) {
                return null
            }

            // ProxyConfiguration throws an error if auth data is included in the url
            val urlWithoutAuth = removeAuthFromProxyUrl(proxyUrl!!)
            val username = getProxyUsername(proxyUrl!!, proxyUsername)
            val password = getProxyPassword(proxyUrl!!, proxyPassword)
            var nonProxyHosts: Set<String>? = emptySet()
            if (!proxyNoProxyAddresses.isNullOrEmpty()) {
                nonProxyHosts = proxyNoProxyAddresses!!.split(",").map { it.trim() }.filter { it != "" }.toSet()
            }
            return ProxyConfiguration.builder()
                .endpoint(URI.create(urlWithoutAuth))
                .username(username)
                .password(password)
                .nonProxyHosts(nonProxyHosts)
                .build()
        }

    /**
     * Provides an url for use in the ALL_PROXY, HTTP_PROXY, and HTTPS_PROXY environment variables.
     *
     * If auth info is provided in both the url and username/password fields, then the url value is used.
     *
     * @param clientProxyConfiguration contains user specified system proxy values
     * @return the proxy url value or an empty string if no proxy is configured
     */
    fun getProxyEnvVarValue(clientProxyConfiguration: ClientProxyConfiguration?): String {

        val proxyUrl = clientProxyConfiguration?.proxyUrl
        if (proxyUrl.isNullOrEmpty()) {
            return ""
        }

        val proxyUsername = clientProxyConfiguration.proxyUsername
        return if (getAuthFromProxyUrl(proxyUrl) == null && !proxyUsername.isNullOrEmpty()) {
            addAuthToProxyUrl(proxyUrl, proxyUsername, clientProxyConfiguration.proxyPassword)
        } else proxyUrl
    }

    /**
     * Provides a value for use in the NO_PROXY environment variable.
     *
     * @param clientProxyConfiguration contains user specified system proxy values
     * @return localhost plus user provided values or an empty string if no proxy is configured
     */
    fun getNoProxyEnvVarValue(clientProxyConfiguration: ClientProxyConfiguration?): String {
        if (clientProxyConfiguration?.proxyUrl.isNullOrEmpty()) {
            return ""
        }
        return if (!clientProxyConfiguration?.noProxyAddresses.isNullOrEmpty()) {
            "localhost," + clientProxyConfiguration?.noProxyAddresses
        } else "localhost"
    }
}