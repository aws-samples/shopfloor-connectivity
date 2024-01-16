/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.ads.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.TcpConfiguration
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class AdsDeviceConfiguration : TcpConfiguration, Validate {


    @SerializedName(CONFIG_WAIT_AFTER_CONNECT_ERROR)
    private var _waitAfterConnectError: Long = DEFAULT_WAIT_AFTER_CONNECT_ERROR
    override val waitAfterConnectError: Duration
        get() = _waitAfterConnectError.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_WAIT_AFTER_READ_ERROR)
    private var _waitAfterReadError: Long = DEFAULT_WAIT_AFTER_READ_ERROR
    override val waitAfterReadError: Duration
        get() = _waitAfterReadError.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_WAIT_AFTER_WRITE_ERROR)
    private var _waitAfterWriteError: Long = DEFAULT_WAIT_AFTER_WRITE_ERROR
    override val waitAfterWriteError: Duration
        get() = _waitAfterWriteError.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_CONNECT_TIMEOUT)
    private var _connectTimeout: Long = DEFAULT_CONNECT_TIMEOUT_MS
    override val connectTimeout: Duration
        get() = _connectTimeout.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_COMMAND_TIMEOUT)
    private var _commandTimeout: Long = DEFAULT_COMMAND_TIMEOUT_MS
    val commandTimeout: Duration
        get() = _commandTimeout.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_READ_TIMEOUT)
    private var _readTimeout: Long = DEFAULT_READ_TIMEOUT_MS
    val readTimeout: Duration
        get() = _readTimeout.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_ADDRESS)
    private var _address: String = ""
    override val address: String
        get() = _address

    @SerializedName(CONFIG_PORT)
    private var _port: Int = DEFAULT_ADS_PORT
    override val port: Int
        get() = _port

    companion object {
        private const val CONFIG_ADDRESS = "Address"
        private const val CONFIG_PORT = "Port"
        private const val CONFIG_CONNECT_TIMEOUT = "ConnectTimeout"
        private const val CONFIG_COMMAND_TIMEOUT = "CommandTimeout"
        private const val CONFIG_READ_TIMEOUT = "ReadTimeout"
        private const val CONFIG_WAIT_AFTER_CONNECT_ERROR = "WaitAfterConnectError"
        private const val CONFIG_WAIT_AFTER_READ_ERROR = "WaitAfterReadError"
        private const val CONFIG_WAIT_AFTER_WRITE_ERROR = "WaitAfterWriteError"

        const val DEFAULT_ADS_PORT = 48898

        const val DEFAULT_CONNECT_TIMEOUT_MS = 10000L
        const val DEFAULT_READ_TIMEOUT_MS = 1000L
        const val DEFAULT_COMMAND_TIMEOUT_MS = 10000L
        const val DEFAULT_WAIT_AFTER_READ_ERROR = 10000L
        const val DEFAULT_WAIT_AFTER_WRITE_ERROR = 10000L
        const val DEFAULT_WAIT_AFTER_CONNECT_ERROR = 10000L

        private val default = AdsDeviceConfiguration()

        fun create(
            waitAfterConnectError: Long = default._waitAfterConnectError,
            waitAfterReadError: Long = default._waitAfterReadError,
            waitAfterWriteError: Long = default._waitAfterWriteError,
            connectTimeout: Long = default._connectTimeout,
            commandTimeout: Long = default._commandTimeout,
            address: String = default._address,
            port: Int = default._port
        ): AdsDeviceConfiguration {

            val instance = AdsDeviceConfiguration()

            with(instance) {
                _address = address
                _port = port
                _commandTimeout= commandTimeout
                _connectTimeout = connectTimeout
                _waitAfterConnectError = waitAfterConnectError
                _waitAfterReadError = waitAfterReadError
                _waitAfterWriteError = waitAfterWriteError
            }
            return instance
        }
    }

    override fun validate() {
        ConfigurationException.check(
            _address.isNotEmpty(),
            "$CONFIG_ADDRESS for ADS device must be set",
            CONFIG_ADDRESS,
            this
        )
    }

    private var _validated = false

    override var validated: Boolean
        get() = _validated
        set(value) {
            _validated = value
        }
}