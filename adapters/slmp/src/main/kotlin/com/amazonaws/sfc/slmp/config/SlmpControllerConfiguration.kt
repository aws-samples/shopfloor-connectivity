/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: MIT-0
 *
 */

package com.amazonaws.sfc.slmp.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.TcpConfiguration
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class SlmpControllerConfiguration : TcpConfiguration, Validate {


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

    @SerializedName(CONFIG_READ_TIMEOUT)
    private var _readTimeout: Long = DEFAULT_READ_TIMEOUT_MS
    val readTimeout: Duration
        get() = _readTimeout.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_ADDRESS)
    private var _address: String = ""
    override val address: String
        get() = _address

    @SerializedName(CONFIG_PORT)
    private var _port: Int = DEFAULT_SLMP_PORT
    override val port: Int
        get() = _port

    @SerializedName(CONFIG_NETWORK_NUMBER)
    private var _networkNumber: Byte = DEFAULT_NETWORK_NUMBER
    val networkNumber: Byte
        get() =  _networkNumber

    @SerializedName(CONFIG_STATION_NUMBER)
    private var _stationNumber: Byte = DEFAULT_STATION_NUMBER
    val stationNumber: Byte
        get() = _stationNumber

    @SerializedName(CONFIG_MODULE_NUMBER)
    private var _moduleNumber: Short = DEFAULT_MODULE_NUMBER
    val moduleNumber: Short
        get() = _moduleNumber

    @SerializedName(CONFIG_MULTI_DROP_STATION_NUMBER)
    private var _multiDropStationNumber: Byte = DEFAULT_MULTI_DROP_STATION_NUMBER
    val multiDropStationNumber: Byte
        get() = _multiDropStationNumber

    @SerializedName(CONFIG_MONITORING_TIMER)
    private var _monitoringTimer: Short = DEFAULT_MONITORING_TIMER
    val monitoringTimer: Short
        get() = _monitoringTimer

    override fun validate() {
        ConfigurationException.check(
            _address.isNotEmpty(),
            "$CONFIG_ADDRESS for SLMP PLC must be set",
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


    companion object {
        private const val CONFIG_ADDRESS = "Address"
        private const val CONFIG_PORT = "Port"
        private const val CONFIG_CONNECT_TIMEOUT = "ConnectTimeout"
        const val CONFIG_READ_TIMEOUT = "ReadTimeout"
        private const val CONFIG_WAIT_AFTER_CONNECT_ERROR = "WaitAfterConnectError"
        private const val CONFIG_WAIT_AFTER_READ_ERROR = "WaitAfterReadError"
        private const val CONFIG_WAIT_AFTER_WRITE_ERROR = "WaitAfterWriteError"
        private const val CONFIG_NETWORK_NUMBER = "NetworkNumber"
        private const val CONFIG_STATION_NUMBER = "StationNumber"
        private const val CONFIG_MODULE_NUMBER = "ModuleNumber"
        private const val CONFIG_MULTI_DROP_STATION_NUMBER = "MultiDropStationNumber"
        private const val CONFIG_MONITORING_TIMER = "MonitoringTimer"

        const val DEFAULT_SLMP_PORT = 50000
        const val DEFAULT_CONNECT_TIMEOUT_MS = 10000L
        const val DEFAULT_READ_TIMEOUT_MS = 5000L
        const val DEFAULT_WAIT_AFTER_READ_ERROR = 10000L
        const val DEFAULT_WAIT_AFTER_WRITE_ERROR = 10000L
        const val DEFAULT_WAIT_AFTER_CONNECT_ERROR = 10000L
        const val DEFAULT_NETWORK_NUMBER: Byte = 0x00.toByte()
        const val DEFAULT_STATION_NUMBER: Byte = 0xFF.toByte()
        const val DEFAULT_MODULE_NUMBER: Short = 0x03FF.toShort()
        const val DEFAULT_MULTI_DROP_STATION_NUMBER = 0x00.toByte()
        const val DEFAULT_MONITORING_TIMER: Short = 0.toShort()



        private val default = SlmpControllerConfiguration()

        fun create(
            waitAfterConnectError: Long = default._waitAfterConnectError,
            waitAfterReadError: Long = default._waitAfterReadError,
            waitAfterWriteError: Long = default._waitAfterWriteError,
            connectTimeout: Long = default._connectTimeout,
            address: String = default._address,
            port: Int = default._port,
            networkNumber: Byte = default._networkNumber,
            stationNumber: Byte = default._stationNumber,
            moduleNumber: Short = default._moduleNumber,
            multiDropStationNumber: Byte = default._multiDropStationNumber,
            monitoringTimer: Short = default._monitoringTimer
        ): SlmpControllerConfiguration {

            val instance = SlmpControllerConfiguration()

            with(instance) {
                _address = address
                _port = port
                _connectTimeout = connectTimeout
                _waitAfterConnectError = waitAfterConnectError
                _waitAfterReadError = waitAfterReadError
                _waitAfterWriteError = waitAfterWriteError
                _networkNumber = networkNumber
                _stationNumber = stationNumber
                _moduleNumber = moduleNumber
                _multiDropStationNumber = multiDropStationNumber
                _multiDropStationNumber = multiDropStationNumber
                _monitoringTimer = monitoringTimer
            }
            return instance
        }
    }
}