/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.modbus.tcp.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Configuration for source Modbus TCP source device
 */

@ConfigurationClass
class ModbusTcpDeviceConfiguration : Validate {

    @SerializedName(CONFIG_ADDRESS)
    private var _address = ""

    /**
     * IP address of the Modbus TCP source server
     */
    val address: String
        get() = _address

    @SerializedName(CONFIG_PORT)
    private var _port = DEFAULT_PORT

    /**
     * Port used by the Modbus TCP source server
     */
    val port: Int
        get() = _port

    @SerializedName(CONFIG_DEVICE_ID)
    private var _deviceID: Int? = null

    /**
     * ID of modbus device
     */
    val deviceID: Int?
        get() = _deviceID

    @SerializedName(CONFIG_REQUEST_DEPTH)
    private var _requestDepth = DEFAULT_REQUEST_DEPTH

    /**
     * Max number of requests that can be sent to the Modbus TCP source server before receiving a request
     */
    val requestDepth: UShort
        get() = _requestDepth

    @SerializedName(CONFIG_CONNECT_TIMEOUT)
    private var _connectTimeout: Long = DEFAULT_CONNECT_TIMEOUT_MS

    /**
     * Timeout for connecting to the Modbus TCP source server
     */
    val connectTimeout: Duration
        get() = _connectTimeout.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_WAIT_AFTER_CONNECT_ERROR)
    private var _waitAfterConnectError: Long = DEFAULT_WAIT_AFTER_CONNECT_ERROR

    /**
     * Time to wait after failed to connect to the Modbus TCP source server
     */
    val waitAfterConnectError: Duration
        get() = _waitAfterConnectError.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_WAIT_AFTER_READ_ERROR)
    private var _waitAfterReadError: Long = DEFAULT_WAIT_AFTER_READ_ERROR

    /**
     * Time to wait after error reading from the Modbus TCP source server
     */
    val waitAfterReadError: Duration
        get() = _waitAfterReadError.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_WAIT_AFTER_WRITE_ERROR)
    private var _waitAfterWriteError: Long = DEFAULT_WAIT_AFTER_WRITE_ERROR

    /**
     * Time to wait after error writing to the Modbus TCP source server
     */
    val waitAfterWriteError: Duration
        get() = _waitAfterWriteError.toDuration(DurationUnit.MILLISECONDS)


    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    /**
     * Validates the configuration
     * @throws ConfigurationException
     */
    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return
        validateAddress()
        validatePort()
        validateRequestDepth()
        validateConnectTimeout()
        validateWaitAfterConnect()
        validateWaitAfterReadError()
        validateWaitAfterWriteError()
        validated = true

    }

    // Tests for shortest period
    private fun Duration.atLeast(milliseconds: Long) =
        this >= milliseconds.toDuration(DurationUnit.MILLISECONDS)

    // Validates time to wait after write error
    private fun validateWaitAfterWriteError() =
        ConfigurationException.check(
            (waitAfterWriteError.atLeast(milliseconds = 1000)),
            "Wait after write error $CONFIG_WAIT_AFTER_WRITE_ERROR must be 1000 (milliseconds) or longer",
            CONFIG_WAIT_AFTER_WRITE_ERROR,
            this
        )

    // Validates time to wait after read error
    private fun validateWaitAfterReadError() =
        ConfigurationException.check(
            (waitAfterReadError.atLeast(milliseconds = 1000)),
            "Wait after read error $CONFIG_WAIT_AFTER_READ_ERROR must be 1000 (millisecond) or longer",
            CONFIG_WAIT_AFTER_READ_ERROR,
            this
        )

    // Validates time to wait after connect error
    private fun validateWaitAfterConnect() =
        ConfigurationException.check(
            (waitAfterConnectError.atLeast(milliseconds = 1000)),
            "Wait after connect error $CONFIG_WAIT_AFTER_CONNECT_ERROR  must be 1000 (millisecond) or longer",
            CONFIG_WAIT_AFTER_CONNECT_ERROR,
            this
        )

    // Validates connection timeout period
    private fun validateConnectTimeout() =
        ConfigurationException.check(
            (connectTimeout.atLeast(milliseconds = 1000)),
            "Connect timeout $CONFIG_CONNECT_TIMEOUT must be 1000 (millisecond) or longer",
            CONFIG_CONNECT_TIMEOUT,
            this
        )

    // Validates request depth
    private fun validateRequestDepth() =
        ConfigurationException.check(
            (requestDepth >= 1u), "$CONFIG_REQUEST_DEPTH must be 1 or more",
            CONFIG_REQUEST_DEPTH,
            this
        )

    // Validates port number
    private fun validatePort() =
        ConfigurationException.check(
            (port > 0),
            "$port is not a valid port number",
            CONFIG_PORT,
            this
        )

    // Validates IP address for Modbus TCP source server
    private fun validateAddress() =
        ConfigurationException.check(
            (address.isNotEmpty()), "Address of TCP source server can not be empty",
            CONFIG_ADDRESS,
            this
        )

    companion object {
        const val DEFAULT_PORT = 502
        const val DEFAULT_CONNECT_TIMEOUT_MS = 10000L

        const val DEFAULT_WAIT_AFTER_READ_ERROR = 10000L
        const val DEFAULT_WAIT_AFTER_WRITE_ERROR = 10000L
        const val DEFAULT_WAIT_AFTER_CONNECT_ERROR = 10000L
        const val DEFAULT_REQUEST_DEPTH: UShort = 1u

        private const val CONFIG_ADDRESS = "Address"
        private const val CONFIG_PORT = "Port"
        private const val CONFIG_DEVICE_ID = "DeviceId"
        private const val CONFIG_REQUEST_DEPTH = "RequestDepth"
        private const val CONFIG_CONNECT_TIMEOUT = "ConnectTimeout"
        private const val CONFIG_WAIT_AFTER_CONNECT_ERROR = "WaitAfterConnectError"
        private const val CONFIG_WAIT_AFTER_READ_ERROR = "WaitAfterReadError"
        private const val CONFIG_WAIT_AFTER_WRITE_ERROR = "WaitAfterWriteError"

        private val default = ModbusTcpDeviceConfiguration()

        fun create(address: String = default._address,
                   port: Int = default._port,
                   deviceId: Int? = default._deviceID,
                   requestDepth: UShort = default._requestDepth,
                   connectTimeout: Long = default._connectTimeout,
                   waitAfterConnectError: Long = default._waitAfterConnectError,
                   waitAfterReadError: Long = default._waitAfterReadError,
                   waitAfterWriteError: Long = default._waitAfterWriteError): ModbusTcpDeviceConfiguration {

            val instance = ModbusTcpDeviceConfiguration()
            @Suppress("DuplicatedCode")
            with(instance) {
                _address = address
                _port = port
                _deviceID = deviceId
                _requestDepth = requestDepth
                _connectTimeout = connectTimeout
                _waitAfterConnectError = waitAfterConnectError
                _waitAfterReadError = waitAfterReadError
                _waitAfterWriteError = waitAfterWriteError
            }
            return instance
        }


    }

}