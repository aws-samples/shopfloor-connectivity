// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.natstarget.config

import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_PASSWORD
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_TOKEN
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_USERNAME
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.crypto.TlsConfiguration
import com.amazonaws.sfc.crypto.TlsConfiguration.Companion.CONFIG_TLS_SSL
import com.google.gson.annotations.SerializedName
import java.io.File
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class NatsServerConfiguration : Validate {

    @SerializedName(CONFIG_URL)
    private var _url: String? = null
    val url: String
        get() = this._url ?: ""

    @SerializedName(CONFIG_CONNECT_RETRIES)
    private var _connectRetries = CONNECT_RETRIES_DEFAULT
    val connectRetries
        get() = _connectRetries


    @SerializedName(CONFIG_WAIT_AFTER_CONNECT_ERROR)
    private var _waitAfterConnectError = DEFAULT_WAIT_AFTER_CONNECT_ERROR
    val waitAfterConnectError: Duration
        get() = _waitAfterConnectError.toDuration(DurationUnit.SECONDS)


    // https://docs.nats.io/using-nats/developer/connecting/token
    @SerializedName(CONFIG_TOKEN)
    private var _token: String? = null
    val token: String?
        get() = _token

    // https://docs.nats.io/using-nats/developer/connecting/userpass
    @SerializedName(CONFIG_USERNAME)
    private var _username: String? = null
    val username: String?
        get() = _username

    // https://docs.nats.io/using-nats/developer/connecting/userpass
    @SerializedName(CONFIG_PASSWORD)
    private var _password: String? = null
    val password: String?
        get() = _password

    // https://docs.nats.io/using-nats/developer/connecting/nkey
    @SerializedName(CONFIG_NKEY_FILE)
    private var _nkeyFile: String? = null
    val nkeyFile: String?
        get() = _nkeyFile

    // https://docs.nats.io/using-nats/developer/connecting/creds
    @SerializedName(CONFIG_CREDENTIALS_FILE)
    private var _credentialsFile: String? = null
    val credentialsFile: String?
        get() = _credentialsFile


    // https://docs.nats.io/using-nats/developer/connecting/tls
    @SerializedName(CONFIG_TLS_SSL)
    private var _tslConfig: TlsConfiguration? = null
    val tlsConfiguration: TlsConfiguration?
        get() = _tslConfig


    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }


    override fun validate() {

        if (validated) return

        validateUrl()
        checkRequiredSettings()
        validateWaitAfterConnectError()
        validateFiles()

        validated = true
    }

    private fun validateFiles() {
        if (_nkeyFile != null) {
            if (File(_nkeyFile!!).exists() == false) {
                throw ConfigurationException("$CONFIG_NKEY_FILE \"$_nkeyFile\" file does not exist", CONFIG_NKEY_FILE, this)
            }
        }

        if (_credentialsFile != null) {
            if (File(_credentialsFile!!).exists() == false) {
                throw ConfigurationException("$CONFIG_CREDENTIALS_FILE \"$_credentialsFile\" file does not exist", CONFIG_CREDENTIALS_FILE, this)
            }
        }
    }

    private fun atLeastOneSecond(t: Duration) = t >= (1.toDuration(DurationUnit.SECONDS))

    private fun checkRequiredSettings() {

        ConfigurationException.check(
            (_username == null && _password == null) || (username != null && password != null),
            "When using $CONFIG_USERNAME and $CONFIG_PASSWORD, both must be set",
            "$CONFIG_USERNAME and $CONFIG_PASSWORD",
            this
        )


    }

    fun validateUrl() {

        ConfigurationException.check(
            !_url.isNullOrEmpty(),
            "$CONFIG_URL for NATS server must be set",
            CONFIG_URL,
            this
        )

        _url!!.split(",").map { it.trim() }.forEach { u ->
            val scheme = SCHEME_REGEX.find(u)?.groups?.get(1)?.value
            if (NATS_VALID_PROTOCOLS.contains(scheme) == false) {
                throw ConfigurationException("$CONFIG_URL \"$_url\" is not a valid NATS server URL, valid protocols are $NATS_VALID_PROTOCOLS", CONFIG_URL, this)
            }
        }

    }

    private fun validateWaitAfterConnectError() =
        ConfigurationException.check(
            atLeastOneSecond(waitAfterConnectError),
            "Wait after connect error $CONFIG_WAIT_AFTER_CONNECT_ERROR must be 1 second or longer",
            "WaitAfterConnectError",
            this
        )


    companion object {

        private const val CONFIG_URL = "Url"
        const val CONFIG_NKEY_FILE = "NKeyFile"
        const val CONFIG_CREDENTIALS_FILE = "CredentialsFile"
        const val DEFAULT_WAIT_AFTER_CONNECT_ERROR = 10
        private const val CONFIG_WAIT_AFTER_CONNECT_ERROR = "WaitAfterConnectError"
        private const val CONFIG_CONNECT_RETRIES = "ConnectRetries"
        private const val CONNECT_RETRIES_DEFAULT = 3

        private const val NATS_TLS = "tls"
        private const val NATS_WEB_SOCKET = "ws"
        private const val NATS_NATS = "nats"
        private val NATS_VALID_PROTOCOLS = listOf(NATS_TLS, NATS_WEB_SOCKET, NATS_NATS)
        private val SCHEME_REGEX = "^([a-z]+)://".toRegex()

        val default = NatsServerConfiguration()


        fun create(
            url: String = default._url ?: "",
            username: String? = default._username,
            password: String? = default._password,
            token: String? = default._token,
            nkeyFile: String? = default._nkeyFile,
            credentialsFile: String? = default._credentialsFile,
            tlsSslConfiguration: TlsConfiguration? = default._tslConfig,
            waitAfterConnectError: Int = default._waitAfterConnectError,
            connectRetries: Int = default._connectRetries,
        ): NatsServerConfiguration {


            val instance = NatsServerConfiguration()
            with(instance) {
                _url = url
                _username = username
                _password = password
                _token = token
                _nkeyFile = nkeyFile
                _credentialsFile = credentialsFile
                _tslConfig = tlsSslConfiguration
                _waitAfterConnectError = waitAfterConnectError
                _connectRetries = connectRetries
            }
            return instance
        }


    }
}