// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.natstarget.config

import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_PASSWORD
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_TOKEN
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_USERNAME
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.TargetConfiguration
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.crypto.TlsConfiguration
import com.amazonaws.sfc.data.Compress.CONFIG_COMPRESS
import com.amazonaws.sfc.data.CompressionType
import com.amazonaws.sfc.crypto.TlsConfiguration.Companion.CONFIG_TLS_SSL
import com.google.gson.annotations.SerializedName
import java.io.File
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@ConfigurationClass
class NatsTargetConfiguration : TargetConfiguration(), Validate {


    @SerializedName(CONFIG_SUBJECT_NAME)
    private var _subjectName: String? = null
    val subjectName: String
        get() = _subjectName ?: ""

    @SerializedName(CONFIG_ALTERNATE_SUBJECT_NAME)
    private var _alternateSubjectName: String? = null
    val alternateSubjectName: String?
        get() = _alternateSubjectName

    @SerializedName(CONFIG_WARN_ALTERNATE_SUBJECT_NAME)
    private var _warnAlternateSubjectName: Boolean = true
    val warnAlternateSubjectName: Boolean
        get() = _warnAlternateSubjectName

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

    @SerializedName(CONFIG_PUBLISH_TIMEOUT)
    private var _publishTimeout = DEFAULT_PUBLISH_TIMEOUT

    val publishTimeout: Duration = _publishTimeout.toDuration(DurationUnit.SECONDS)

    @SerializedName(CONFIG_WAIT_AFTER_CONNECT_ERROR)
    private var _waitAfterConnectError = DEFAULT_WAIT_AFTER_CONNECT_ERROR
    val waitAfterConnectError: Duration
        get() = _waitAfterConnectError.toDuration(DurationUnit.SECONDS)

    @SerializedName(CONFIG_CONNECT_RETRIES)
    private var _connectRetries = CONNECT_RETRIES_DEFAULT
    val connectRetries
        get() = _connectRetries

    @SerializedName(CONFIG_URL)
    private var _url: String? = null
    val url: String
        get() = this._url ?: ""

    //https://docs.nats.io/using-nats/developer/connecting/tls
    @SerializedName(CONFIG_TLS_SSL)
    private var _tslSslConfig: TlsConfiguration? = null
    val tlsSslConfiguration: TlsConfiguration?
        get() = _tslSslConfig

    @SerializedName(CONFIG_BATCH_COUNT)
    private var _batchCount: Int? = null
    val batchCount
        get() = _batchCount ?: 0

    @SerializedName(CONFIG_BATCH_SIZE)
    private var _batchSize: Int? = null
    val batchSize
        get() = if (_batchSize != null) _batchSize!! * 1024 else 0

    @SerializedName(CONFIG_BATCH_INTERVAL)
    private var _batchInterval: Int? = null
    val batchInterval: Duration
        get() = _batchInterval?.toDuration(DurationUnit.MILLISECONDS) ?: Duration.INFINITE

    @SerializedName(CONFIG_MAX_PAYLOAD_SIZE)
    private var _maxPayloadSize: Int? = null
    val maxPayloadSize
        get() = if (_maxPayloadSize != null) _maxPayloadSize!! * 1024 else null


    @SerializedName(CONFIG_COMPRESS)
    private var _compressionType: CompressionType? = null

    val compressionType: CompressionType
        get() = _compressionType ?: CompressionType.NONE


    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return

        validateBufferSize()
        checkRequiredSettings()

        validateFiles()
        super.validate()

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

    private fun validateBufferSize() {
        ConfigurationException.check(
            (_batchSize == null || _maxPayloadSize == null || _compressionType == CompressionType.NONE || (_batchSize!! <= _maxPayloadSize!!)),
            "$CONFIG_BATCH_SIZE must be smaller or equal to value of $CONFIG_BATCH_SIZE",
            CONFIG_BATCH_SIZE,
            this)
    }

    private fun checkRequiredSettings() {
        ConfigurationException.check(
            !_url.isNullOrEmpty(),
            "$CONFIG_URL for NATS target must be set",
            CONFIG_URL,
            this
        )
    }

    fun validateUrl(){

        ConfigurationException.check(
            !_url.isNullOrEmpty(),
            "$CONFIG_URL for NATS server must be set",
            CONFIG_URL,
            this
        )

        _url!!.split(",").map { it.trim() }.forEach { u ->
            val scheme = SCHEME_REGEX.find(u)?.groups?.get(1)?.value
            if (NATS_VALID_PROTOCOLS.contains(scheme)== false) {
                throw ConfigurationException("$CONFIG_URL} \"$_url\" is not a valid NATS server URL, valid protocols are $NATS_VALID_PROTOCOLS}", CONFIG_URL, this)
            }

            if (scheme == NATS_TLS &&  _tslSslConfig == null){
                throw ConfigurationException("When using TLS, $CONFIG_TLS_SSL must be set", CONFIG_TLS_SSL, this)
            }
        }
    }

    companion object {

        private const val CONFIG_URL = "Url"
        const val CONFIG_SUBJECT_NAME = "SubjectName"
        const val CONFIG_ALTERNATE_SUBJECT_NAME = "AlternateSubjectName"
        const val CONFIG_WARN_ALTERNATE_SUBJECT_NAME = "WarnAlternateSubjectName"
        const val CONFIG_NKEY_FILE = "NKeyFile" // https://docs.nats.io/using-nats/developer/connecting/nkey
        const val CONFIG_CREDENTIALS_FILE = "CredentialsFile" //https://docs.nats.io/using-nats/developer/connecting/creds
        private const val CONFIG_PUBLISH_TIMEOUT = "PublishTimeout"
        private const val DEFAULT_PUBLISH_TIMEOUT = 10
        const val DEFAULT_WAIT_AFTER_CONNECT_ERROR = 10
        private const val CONFIG_WAIT_AFTER_CONNECT_ERROR = "WaitAfterConnectError"
        private const val CONFIG_CONNECT_RETRIES = "ConnectRetries"
        private const val CONNECT_RETRIES_DEFAULT = 10
        const val CONFIG_BATCH_SIZE = "BatchSize"
        const val CONFIG_BATCH_COUNT = "BatchCount"
        const val CONFIG_BATCH_INTERVAL = "BatchInterval"
        private const val CONFIG_MAX_PAYLOAD_SIZE = "MaxPayloadSize"

        private const val NATS_TLS = "tls"
        private const val NATS_WEB_SOCKET = "ws"
        private const val NATS_NATS = "nats"
        private val NATS_VALID_PROTOCOLS = listOf(NATS_TLS, NATS_WEB_SOCKET, NATS_NATS)
        private val SCHEME_REGEX = "^([a-z]+)://".toRegex()

        val default = NatsTargetConfiguration()


        fun create(
            url: String = default._url ?: "",
            subjectName: String? = default._subjectName,
            alternateSubjectName : String? = default.alternateSubjectName,
            warnAlternateSubjectName: Boolean = default._warnAlternateSubjectName,
            username: String? = default._username,
            password: String? = default._password,
            token: String? = default._token,
            nkeyFile: String? = default._nkeyFile,
            credentialsFile: String? = default._credentialsFile,
            tlsSslConfiguration: TlsConfiguration? = default._tslSslConfig,
            publishTimeout: Int = default._publishTimeout,
            waitAfterConnectError : Int = default._waitAfterConnectError,
            connectRetries: Int = default._connectRetries,
            batchCount: Int? = default.batchCount,
            batchSize: Int? = default._batchSize,
            batchInterval: Int? = default._batchInterval,
            maxPayloadSize: Int? = default._maxPayloadSize,
            compression: CompressionType? = default._compressionType
        ): NatsTargetConfiguration {


            val instance = NatsTargetConfiguration()
            with(instance) {
                _url = url
                _subjectName = subjectName
                _alternateSubjectName = alternateSubjectName
                _warnAlternateSubjectName = warnAlternateSubjectName
                _username = username
                _password = password
                _token = token
                _nkeyFile = nkeyFile
                _credentialsFile = credentialsFile
                _tslSslConfig = tlsSslConfiguration
                _publishTimeout = publishTimeout
                _waitAfterConnectError = waitAfterConnectError
                _connectRetries = connectRetries
                _batchCount = batchCount
                _batchSize = batchSize
                _batchInterval = batchInterval
                _maxPayloadSize = maxPayloadSize
                _compressionType = compression
            }
            return instance
        }


    }
}
