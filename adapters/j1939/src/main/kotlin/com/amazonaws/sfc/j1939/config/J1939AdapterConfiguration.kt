
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//


package com.amazonaws.sfc.j1939.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.ProtocolAdapterConfiguration
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.j1939.config.J1939Configuration.Companion.J1939_ADAPTER
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.google.gson.annotations.SerializedName
import java.io.File
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class J1939AdapterConfiguration : ProtocolAdapterConfiguration(), Validate {

    @SerializedName(CONFIG_CANBUS_SOCKETS)
    private var _canSockets : Map<String, J1939AdapterSocketConfiguration> = emptyMap<String, J1939AdapterSocketConfiguration>()

    val canSockets:  Map<String, J1939AdapterSocketConfiguration>
        get() = _canSockets

    @SerializedName(CONFIG_DBC_FILE)
    private var _dbcFile : String? = null
    val dbcFile: File?
        get() = if (_dbcFile != null) File(_dbcFile!!) else null

    @SerializedName(CONFIG_RECEIVED_DATA_CHANNEL_SIZE)
    private var _receivedDataChannelSize = DEFAULT_RECEIVED_DATA_CHANNEL_SIZE
    val receivedDataChannelSize
        get() = _receivedDataChannelSize

    @SerializedName(CONFIG_READ_TIMESTAMP)
    private var _readTimeStamp : Boolean = true
    val readTimeStamp: Boolean
    get() = _readTimeStamp

    @SerializedName(CONFIG_READ_MODE)
    private var _readMode = J1939ReadMode.KEEP_LAST
    val readMode: J1939ReadMode
        get() = _readMode

    @SerializedName(CONFIG_MAX_RETAIN_SIZE)
    private var _maxRetainSize : Int = DEFAULT_MAX_RETAIN_SIZE
    val maxRetainSize : Int
        get() = _maxRetainSize

    @SerializedName(CONFIG_MAX_RETAIN_PERIOD)
    private var _maxRetainPeriod : Int = DEFAULT_MAX_RETAIN_PERIOD
    val maxRetainPeriod : Int
        get() = _maxRetainPeriod

    @SerializedName(CONFIG_WAIT_AFTER_READ_ERROR)
    private var _waitAfterReadErrors: Long = CONFIG_DEFAULT_WAIT_AFTER_READ_ERROR

    val waitAfterReadErrors: Duration
        get() = _waitAfterReadErrors.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_WAIT_AFTER_OPEN_ERROR)
    private var _waitAfterOpenErrors: Long = CONFIG_DEFAULT_WAIT_AFTER_OPEN_ERROR

    val waitAfterOpenErrors: Duration
        get() = _waitAfterOpenErrors.toDuration(DurationUnit.MILLISECONDS)

    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return
        super.validate()
        validateCanSocketName()
        validateDbcFile()
        validated = true
    }

    private fun validateDbcFile() {
        ConfigurationException.check(
            _dbcFile != null,
            "$CONFIG_DBC_FILE must be specified",
            CONFIG_DBC_FILE,
            this)

    }

    private fun validateCanSocketName() {
        ConfigurationException.check(
            !_canSockets.isEmpty(),
            "$CONFIG_CANBUS_SOCKETS must be specified and contain at least one item",
            CONFIG_CANBUS_SOCKETS,
            this)
    }

    companion object {
        const val CONFIG_CANBUS_SOCKETS = "CanSockets"
        const val CONFIG_RECEIVED_DATA_CHANNEL_SIZE = "ReceivedDataChannelSize"
        const val CONFIG_DBC_FILE = "DbcFile"
        const val CONFIG_READ_MODE = "ReadMode"
        const val CONFIG_MAX_RETAIN_SIZE ="MaxRetainSize"
        const val CONFIG_MAX_RETAIN_PERIOD ="MaxRetainPeriod"
        const val CONFIG_READ_TIMESTAMP = "ReadTimestamp"
        const val CONFIG_WAIT_AFTER_READ_ERROR = "WaitAfterReadError"
        const val CONFIG_WAIT_AFTER_OPEN_ERROR = "WaitAfterOpenError"

        const val CONFIG_DEFAULT_WAIT_AFTER_READ_ERROR = 10000L
        const val CONFIG_DEFAULT_WAIT_AFTER_OPEN_ERROR = 60000L
        const val DEFAULT_MAX_RETAIN_PERIOD = 1000 * 60 * 60
        const val DEFAULT_MAX_RETAIN_SIZE = 10000

        const val DEFAULT_RECEIVED_DATA_CHANNEL_SIZE = 1000

        private val default = J1939AdapterConfiguration()

        fun create(canSockets :  Map<String, J1939AdapterSocketConfiguration> = default._canSockets,
                   maxRetainSize : Int = default._maxRetainSize,
                   maxRetainPeriod : Int = default._maxRetainPeriod,
                   readTimeStamp : Boolean = default._readTimeStamp,
                   waitAfterReadErrors : Long = default._waitAfterReadErrors,
                   waitAfterOpenErrors : Long = default._waitAfterOpenErrors,
                   dbcFile : String = default._dbcFile!!,
                   receivedDataChannelSize : Int = default._receivedDataChannelSize,
                   description: String = default._description,
                   metrics: MetricsSourceConfiguration? = default._metrics,
                   readMode: J1939ReadMode = default._readMode,
                   adapterServer: String? = default._protocolAdapterServer): J1939AdapterConfiguration {

            val instance = createAdapterConfiguration<J1939AdapterConfiguration>(
                description = description,
                adapterType = J1939_ADAPTER,
                metrics = metrics,
                adapterServer = adapterServer)

            with(instance) {
                _canSockets = canSockets
                _readMode = readMode
                _dbcFile = dbcFile
                _maxRetainSize = maxRetainSize
                _maxRetainPeriod = maxRetainPeriod
                _readTimeStamp = readTimeStamp
                _waitAfterOpenErrors = waitAfterOpenErrors
                _waitAfterReadErrors = waitAfterReadErrors
                _receivedDataChannelSize = receivedDataChannelSize
            }
            return instance
        }

    }

}


