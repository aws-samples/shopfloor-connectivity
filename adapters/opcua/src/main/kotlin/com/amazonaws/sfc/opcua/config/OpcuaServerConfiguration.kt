
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.opcua.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.crypto.CertificateConfiguration
import com.google.gson.annotations.SerializedName
import org.eclipse.milo.opcua.stack.core.channel.MessageLimits
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Suppress("MemberVisibilityCanBePrivate", "MemberVisibilityCanBePrivate")
@ConfigurationClass
class OpcuaServerConfiguration : Validate {

    @SerializedName(CONFIG_ADDRESS)
    private var _address = ""
    val address: String
        get() = _address

    @SerializedName(CONFIG_PORT)
    private var _port = DEFAULT_PORT
    val port: Int
        get() = _port

    @SerializedName(CONFIG_PATH)
    private var _path = ""
    val path: String
        get() = _path

    @SerializedName(CONFIG_SERVER_PROFILE)
    private var _serverProfile: String? = null
    val serverProfile: String?
        get() = _serverProfile

    @SerializedName(CONFIG_SECURITY_POLICY)
    private var _securityPolicy: OpcuaSecurityPolicy = OpcuaSecurityPolicy.None
    val securityPolicy: OpcuaSecurityPolicy
        get() = _securityPolicy

    @SerializedName(CONFIG_CONNECT_TIMEOUT)
    private var _connectTimeout: Long = DEFAULT_CONNECT_TIMEOUT_MS
    val connectTimeout: Duration
        get() = _connectTimeout.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_READ_TIMEOUT)
    private var _readTimeout: Long = DEFAULT_READ_TIMEOUT_MS
    val readTimeout: Duration
        get() = _readTimeout.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_WAIT_AFTER_CONNECT_ERROR)
    private var _waitAfterConnectError: Long = DEFAULT_WAIT_AFTER_CONNECT_ERROR
    val waitAfterConnectError: Duration
        get() = _waitAfterConnectError.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_WAIT_AFTER_READ_ERROR)
    private var _waitAfterReadError: Long = DEFAULT_WAIT_AFTER_READ_ERROR
    val waitAfterReadError: Duration
        get() = _waitAfterReadError.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_CONNECTION_WATCHDOG_INTERVAL)
    private var _connectionWatchdogInterval: Long = DEFAULT_CONNECTION_WATCHDOG_INTERVAL
    val connectionWatchdogInterval: Duration
        get() = _connectionWatchdogInterval.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_READ_BATCH_SIZE)
    private var _readBatchSize: Int = DEFAULT_READ_BATCH_SIZE
    val readBatchSize: Int
        get() = _readBatchSize

    @SerializedName(CONFIG_MAX_MESSAGE_SIZE)
    private var _maxMessageSize: Int = DEFAULT_MAX_MESSAGE_SIZE
    val maxMessageSize: Int
        get() = _maxMessageSize

    @SerializedName(CONFIG_MAX_CHUNK_SIZE)
    private var _maxChunkSize: Int = DEFAULT_MAX_CHUNK_SIZE
    val maxChunkSize: Int
        get() = _maxChunkSize

    @SerializedName(CONFIG_MAX_CHUNK_COUNT)
    private var _maxChunkCount: Int? = null
    val maxChunkCount: Int
        get() = _maxChunkCount ?: ((maxMessageSize / maxChunkSize) * 2)

    @SerializedName(CONFIG_CERTIFICATE)
    private var _certificateConfiguration: CertificateConfiguration? = null
    val certificateConfiguration: CertificateConfiguration?
        get() = _certificateConfiguration

    @SerializedName(CONFIG_CERTIFICATE_VALIDATION)
    private var _certificateValidationConfiguration: OpcuaCertificateValidationConfiguration? = null
    val certificateValidationConfiguration: OpcuaCertificateValidationConfiguration?
        get() = _certificateValidationConfiguration

    val endPoint
        get() = listOf("${address}:${port}", path).joinToString(separator = "/")

    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    @Throws(ConfigurationException::class)
    override fun validate() {

        if (validated) return
        validateAddress()
        validatePort()
        validateConnectionTimeout()
        validateWaitAfterConnectError()
        validateWaitAfterReadError()
        validateMessageLimits()
        validateCertificateForPolicy()
        validated = true
    }

    private fun validateCertificateForPolicy() {
        if (_securityPolicy != OpcuaSecurityPolicy.None) {
            ConfigurationException.check(
                _certificateConfiguration?.certificatePath != null,
                "$CONFIG_CERTIFICATE must be set to a certificate when security policy $_securityPolicy is used",
                CONFIG_CERTIFICATE,
                this
            )
        }
    }

    private fun atLeastOneSecond(t: Duration) = t >= (1000.toDuration(DurationUnit.MILLISECONDS))

    private fun validateWaitAfterReadError() =
        ConfigurationException.check(
            atLeastOneSecond(waitAfterReadError),
            "Wait after read error $CONFIG_WAIT_AFTER_READ_ERROR must be 1 (second) or longer",
            CONFIG_WAIT_AFTER_READ_ERROR,
            this
        )

    private fun validateWaitAfterConnectError() =
        ConfigurationException.check(
            atLeastOneSecond(waitAfterConnectError),
            "Wait after connect error $CONFIG_WAIT_AFTER_CONNECT_ERROR must be 1 (second) or longer",
            CONFIG_WAIT_AFTER_CONNECT_ERROR,
            this
        )

    private fun validateConnectionTimeout() =
        ConfigurationException.check(
            atLeastOneSecond(connectTimeout),
            "Connect timeout $CONFIG_CONNECT_TIMEOUT must be 1 (millisecond) or longer",
            CONFIG_CONNECT_TIMEOUT,
            this
        )

    private fun validatePort() {
        ConfigurationException.check(
            (port > 0),
            "$CONFIG_PORT must be 1 or higher",
            CONFIG_PORT,
            this
        )
    }

    private fun validateAddress() =
        ConfigurationException.check(
            (address.isNotEmpty()),
            "$CONFIG_ADDRESS of OPCUA source server can not be empty",
            CONFIG_ADDRESS,
            this
        )

    private fun validateMessageLimits() {
        ConfigurationException.check(
            (maxChunkSize > 8196 && maxChunkSize <= Int.MAX_VALUE - 8),
            "$CONFIG_MAX_CHUNK_SIZE must be > 8196 and <= ${Int.MAX_VALUE - 8}",
            CONFIG_MAX_CHUNK_SIZE,
            this
        )

        ConfigurationException.check(
            (maxMessageSize > 8196 && maxMessageSize <= Int.MAX_VALUE - 8),
            "$CONFIG_MAX_MESSAGE_SIZE must be > 8196 and <= ${Int.MAX_VALUE}",
            CONFIG_MAX_MESSAGE_SIZE,
            this
        )

        ConfigurationException.check(
            (maxChunkSize < maxMessageSize),
            "$CONFIG_MAX_CHUNK_SIZE must be < $CONFIG_MAX_MESSAGE_SIZE",
            CONFIG_MAX_CHUNK_SIZE,
            this
        )

        ConfigurationException.check(
            (maxChunkCount > 1),
            "$CONFIG_MAX_CHUNK_COUNT must be > 1 ",
            CONFIG_MAX_CHUNK_COUNT,
            this
        )
    }


    companion object {
        const val DEFAULT_PORT = 53530
        const val DEFAULT_CONNECT_TIMEOUT_MS = 10000L
        const val DEFAULT_READ_TIMEOUT_MS = 10000L
        const val DEFAULT_WAIT_AFTER_READ_ERROR = 1000L
        const val DEFAULT_WAIT_AFTER_CONNECT_ERROR = 10000L
        const val DEFAULT_READ_BATCH_SIZE = 500
        const val DEFAULT_CONNECTION_WATCHDOG_INTERVAL = 1000L
        const val DEFAULT_MAX_MESSAGE_SIZE = MessageLimits.DEFAULT_MAX_MESSAGE_SIZE
        const val DEFAULT_MAX_CHUNK_SIZE = MessageLimits.DEFAULT_MAX_CHUNK_SIZE

        private const val CONFIG_ADDRESS = "Address"
        private const val CONFIG_PORT = "Port"
        private const val CONFIG_PATH = "Path"
        private const val CONFIG_CONNECT_TIMEOUT = "ConnectTimeout"
        private const val CONFIG_READ_TIMEOUT = "ReadTimeout"
        private const val CONFIG_WAIT_AFTER_CONNECT_ERROR = "WaitAfterConnectError"
        private const val CONFIG_WAIT_AFTER_READ_ERROR = "WaitAfterReadError"
        private const val CONFIG_CONNECTION_WATCHDOG_INTERVAL = "ConnectionWatchdogInterval"
        private const val CONFIG_READ_BATCH_SIZE = "ReadBatchSize"
        private const val CONFIG_MAX_MESSAGE_SIZE = "MaxMessageSize"
        private const val CONFIG_MAX_CHUNK_SIZE = "MaxChunkSize"
        private const val CONFIG_MAX_CHUNK_COUNT = "MaxChunkCount"
        private const val CONFIG_CERTIFICATE = "Certificate"
        private const val CONFIG_SECURITY_POLICY = "SecurityPolicy"
        private const val CONFIG_CERTIFICATE_VALIDATION = "CertificateValidation"
        const val CONFIG_SERVER_PROFILE = "ServerProfile"


        private val default = OpcuaServerConfiguration()

        fun create(address: String = default._address,
                   port: Int = default._port,
                   path: String = default._path,
                   connectTimeout: Long = default._connectTimeout,
                   readTimeout: Long = default._readTimeout,
                   serverProfile: String? = default._serverProfile,
                   waitAfterConnectError: Long = default._waitAfterConnectError,
                   waitAfterReadError: Long = default._waitAfterReadError,
                   readBatchSize: Int = default._readBatchSize,
                   maxMessageSize: Int = default._maxMessageSize,
                   maxChunkSize: Int = default._maxChunkSize,
                   maxChunkCount: Int = default.maxChunkCount): OpcuaServerConfiguration {

            val instance = OpcuaServerConfiguration()

            @Suppress("DuplicatedCode")
            with(instance) {
                _address = address
                _port = port
                _path = path
                _connectTimeout = connectTimeout
                _readTimeout = readTimeout
                _waitAfterConnectError = waitAfterConnectError
                _waitAfterReadError = waitAfterReadError
                _readBatchSize = readBatchSize
                _serverProfile = serverProfile
                _maxMessageSize = maxMessageSize
                _maxChunkSize = maxChunkSize
                _maxChunkCount = maxChunkCount
            }
            return instance
        }


    }
}