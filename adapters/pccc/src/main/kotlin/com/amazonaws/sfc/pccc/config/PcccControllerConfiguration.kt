/*
 *
 *
 *     Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *      Licensed under the Amazon Software License (the "License"). You may not use this file except in
 *      compliance with the License. A copy of the License is located at :
 *
 *      http://aws.amazon.com/asl/
 *
 *      or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 *      language governing permissions and limitations under the License.
 *
 *
 *
 */

package com.amazonaws.sfc.pccc.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.TcpConfiguration
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Suppress("MemberVisibilityCanBePrivate")
@ConfigurationClass
class PcccControllerConfiguration : TcpConfiguration, Validate {

    @SerializedName(CONFIG_CONNECT_PATH)
    private var _connectPath: PcccConnectPathConfig? = null
    val connectPathConfig = _connectPath

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
    private var _readTimeout: Long = DEFAULT_CONNECT_TIMEOUT_MS
    val readTimeout: Duration
        get() = _readTimeout.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_ADDRESS)
    private var _address: String = ""
    override val address: String
        get() = _address

    @SerializedName(CONFIG_PORT)
    private var _port: Int = DEFAULT_PCCC_PORT
    override val port: Int
        get() = _port

    @SerializedName(CONFIG_OPTIMIZE_READS)
    private var _optimizeReads: Boolean = true
    val optimizeReads: Boolean
        get() = _optimizeReads

    @SerializedName(CONFIG_READ_MAX_GAP)
    private var _readMaxGap: Int = DEFAULT_MAX_GAP
    val readMaxGap: Int
        get() = _readMaxGap


    companion object {

        const val CONFIG_CONNECT_PATH = "ConnectPath"
        private const val CONFIG_ADDRESS = "Address"
        private const val CONFIG_PORT = "Port"
        private const val CONFIG_CONNECT_TIMEOUT = "ConnectTimeout"
        private const val CONFIG_READ_TIMEOUT = "ReadTimeout"
        private const val CONFIG_WAIT_AFTER_CONNECT_ERROR = "WaitAfterConnectError"
        private const val CONFIG_WAIT_AFTER_READ_ERROR = "WaitAfterReadError"
        private const val CONFIG_WAIT_AFTER_WRITE_ERROR = "WaitAfterWriteError"
        private const val CONFIG_OPTIMIZE_READS = "OptimizeReads"
        private const val CONFIG_READ_MAX_GAP = "MaxGap"

        const val DEFAULT_PCCC_PORT = 44818
        const val DEFAULT_MAX_GAP = 32

        const val DEFAULT_CONNECT_TIMEOUT_MS = 10000L
        const val DEFAULT_WAIT_AFTER_READ_ERROR = 10000L
        const val DEFAULT_WAIT_AFTER_WRITE_ERROR = 10000L
        const val DEFAULT_WAIT_AFTER_CONNECT_ERROR = 10000L

        private val default = PcccControllerConfiguration()

        fun create(
            connectPath: PcccConnectPathConfig? = default._connectPath,
            waitAfterConnectError: Long = default._waitAfterConnectError,
            waitAfterReadError: Long = default._waitAfterReadError,
            waitAfterWriteError: Long = default._waitAfterWriteError,
            connectTimeout: Long = default._connectTimeout,
            address: String = default._address,
            port: Int = default._port,
            optimizeReads: Boolean = default._optimizeReads,
            readMaxGap: Int = default._readMaxGap
        ): PcccControllerConfiguration {

            val instance = PcccControllerConfiguration()

            with(instance) {
                _address = address
                _port = port
                _optimizeReads = optimizeReads
                _readMaxGap = readMaxGap
                _connectPath = connectPath
                _waitAfterConnectError = waitAfterConnectError
                _waitAfterReadError = waitAfterReadError
                _waitAfterWriteError = waitAfterWriteError
                _connectTimeout = connectTimeout
            }
            return instance
        }
    }

    override fun validate() {
        ConfigurationException.check(
            _address.isNotEmpty(),
            "$CONFIG_ADDRESS for PCCC controller must be set",
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