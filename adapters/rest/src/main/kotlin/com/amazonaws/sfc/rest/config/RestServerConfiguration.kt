// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.rest.config

import com.amazonaws.sfc.config.ClientProxyConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class RestServerConfiguration : Validate {

    @SerializedName(CONFIG_SERVER)
    private var _server = ""
    val server: String
        get() = _server

    @SerializedName(CONFIG_SERVER_PORT)
    private var _port: Int? = null
    val port: Int?
        get() = _port

    @SerializedName(CONFIG_WAIT_AFTER_READ_ERROR)
    private var _waitAfterReadError: Long = DEFAULT_WAIT_AFTER_READ_ERROR
    val waitAfterReadError: Duration
        get() = _waitAfterReadError.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_BEFORE_RETRY)
    private var _waitBeforeRetry: Long = DEFAULT_WAIT_BEFORE_RETRY
    val waitBeforeRetry: Duration
        get() = _waitBeforeRetry.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_REQUEST_TIMEOUT)
    private var _requestTimeout: Long = DEFAULT_REQUEST_TIMEOUT

    val requestTimeout: Duration
        get() = _requestTimeout.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_MAX_RETRIES)
    private var _maxRetries: Int = DEFAULT_MAX_RETRIES
    val maxRetries: Int
        get() = _maxRetries

    @SerializedName(CONFIG_HEADERS)
    private var _headers: Map<String, String> = mapOf()
    val headers: Map<String, String>
        get() = _headers


    @SerializedName(CONFIG_PROXY)
    private var _proxy: ClientProxyConfiguration? = null
    val proxy: ClientProxyConfiguration?
        get() = _proxy

    val serverString: String by lazy {

        var serverStr = server.trimEnd('/')
        serverStr = if (serverStr.lowercase().startsWith("http://") || serverStr.lowercase().startsWith("https://"))
            server
        else
            "http://$server"

        if (port != null) {
            serverStr = "$serverStr:$port"
        }

        serverStr

    }

    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return
        validateHost()
        validated = true

    }


    private fun validateHost() =
        ConfigurationException.check(
            (server.isNotEmpty()),
            "Server can not be empty",
            CONFIG_SERVER,
            this
        )


    companion object {
        const val DEFAULT_WAIT_AFTER_READ_ERROR = 10000L
        const val DEFAULT_WAIT_BEFORE_RETRY = 1000L
        const val DEFAULT_REQUEST_TIMEOUT = 5000L
        const val DEFAULT_MAX_RETRIES = 3

        const val CONFIG_SERVER = "Server"
        private const val CONFIG_SERVER_PORT = "Port"
        private const val CONFIG_WAIT_AFTER_READ_ERROR = "WaitAfterReadError"
        private const val CONFIG_REQUEST_TIMEOUT = "RequestTimeout"
        private const val CONFIG_BEFORE_RETRY = "WaitBeforeRetry"
        private const val CONFIG_MAX_RETRIES = "MaxRetries"
        private const val CONFIG_HEADERS = "Headers"
        private const val CONFIG_PROXY = "Proxy"


        private val default = RestServerConfiguration()

        fun create(server: String = default._server,
                   port: Int? = default._port,
                   waitAfterReadError: Long = default._waitAfterReadError,
                   proxy: ClientProxyConfiguration? = default._proxy): RestServerConfiguration {

            val instance = RestServerConfiguration()
            with(instance) {
                _server = server
                _port = port
                _waitAfterReadError = waitAfterReadError
                _proxy = proxy
            }
            return instance
        }

    }
}