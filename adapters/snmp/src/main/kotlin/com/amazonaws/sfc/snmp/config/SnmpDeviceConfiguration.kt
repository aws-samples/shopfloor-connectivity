/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.snmp.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName
import org.snmp4j.mp.SnmpConstants
import org.snmp4j.smi.TcpAddress
import org.snmp4j.smi.TransportIpAddress
import org.snmp4j.smi.UdpAddress
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class SnmpDeviceConfiguration : Validate {

    @SerializedName(CONFIG_DEVICE_ADDRESS)
    private var _address = ""
    val address: String
        get() = _address

    @SerializedName(CONFIG_DEVICE_PORT)
    private var _port = DEFAULT_PORT
    val port: Int
        get() = _port

    @SerializedName(CONFIG_WAIT_AFTER_READ_ERROR)
    private var _waitAfterReadError = DEFAULT_WAIT_AFTER_READ_ERROR
    val waitAfterReadError: kotlin.time.Duration
        get() = _waitAfterReadError.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_READ_RETRIES)
    private var _readRetries = DEFAULT_READ_RETRIES
    private val readRetries: Int
        get() = _readRetries

    @SerializedName(CONFIG_COMMUNITY)
    private var _community = DEFAULT_COMMUNITY
    val community: String
        get() = _community

    @SerializedName(CONFIG_NETWORK_PROTOCOL)
    private var _networkProtocol: SnmpNetworkProtocol? = null

    val networkProtocol: SnmpNetworkProtocol
        get() = _networkProtocol ?: DEFAULT_SNMP_NETWORK_PROTOCOL

    val deviceAddress: TransportIpAddress by lazy {
        val addressStr = "${address}/${port}"
        when (networkProtocol) {
            SnmpNetworkProtocol.UDP -> {
                UdpAddress(addressStr)
            }

            SnmpNetworkProtocol.TCP -> {
                TcpAddress(addressStr)
            }
        }
    }

    @SerializedName(CONFIG_SNMP_VERSION)
    private var _snmpVersion: Int = DEFAULT_SNMP_VERSION

    val snmpVersion = when (_snmpVersion) {
        1 -> SnmpConstants.version1
        2 -> SnmpConstants.version2c
        else -> {
            SnmpConstants.version2c
        }
    }

    @SerializedName(CONFIG_TIMEOUT)
    private var _timeout = DEFAULT_TIMEOUT
    val timeout: Long
        get() = if (_timeout > 0) _timeout else DEFAULT_TIMEOUT

    @SerializedName(CONFIG_RETRIES)
    private var _retries = DEFAULT_RETRIES
    val retries: Int
        get() = if (_retries > 0) _retries else 0

    @SerializedName(CONFIG_BATCH_READ_SIZE)
    private var _readBatchSize = DEFAULT_READ_BATCH_SIZE
    val readBatchSize: Int
        get() = _readBatchSize

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
        validateVersion()
        validateBulkSize()
        validateReadRetries()
        validateWaitAfterReadError()
        validated = true
    }

    private fun validateVersion() =
        ConfigurationException.check(
            snmpVersion in listOf(1, 2),
            "Supported SNMP version are 1 and 2",
            CONFIG_SNMP_VERSION,
            this
        )

    private fun validatePort() {
        ConfigurationException.check(
            (port > 0),
            "$CONFIG_DEVICE_PORT must be 1 or higher",
            CONFIG_DEVICE_PORT,
            this
        )
    }

    private fun validateReadRetries() {
        ConfigurationException.check(
            (readRetries > 0),
            "$CONFIG_READ_RETRIES must be 1 or higher",
            CONFIG_READ_RETRIES,
            this
        )
    }

    private fun validateWaitAfterReadError() {
        ConfigurationException.check(
            (waitAfterReadError.inWholeMilliseconds > 0),
            "$CONFIG_WAIT_AFTER_READ_ERROR must be 1 or higher",
            CONFIG_WAIT_AFTER_READ_ERROR,
            this
        )
    }

    private fun validateBulkSize() {
        ConfigurationException.check(
            (readBatchSize > 0),
            "$CONFIG_BATCH_READ_SIZE must be 1 or higher",
            CONFIG_BATCH_READ_SIZE,
            this
        )
    }

    private fun validateAddress() =
        ConfigurationException.check(
            (address.isNotEmpty()),
            "Address of SNMP device can not be empty",
            CONFIG_DEVICE_ADDRESS,
            this
        )

    companion object {
        private const val DEFAULT_PORT = 161
        private const val DEFAULT_TIMEOUT = 10000L
        private const val DEFAULT_READ_BATCH_SIZE = 100
        private const val DEFAULT_RETRIES = 2
        private const val DEFAULT_SNMP_VERSION = 2
        private val DEFAULT_SNMP_NETWORK_PROTOCOL = SnmpNetworkProtocol.UDP
        private const val DEFAULT_COMMUNITY = "public"
        private const val DEFAULT_WAIT_AFTER_READ_ERROR = 1000
        private const val DEFAULT_READ_RETRIES = 3

        private const val CONFIG_DEVICE_ADDRESS = "Address"
        private const val CONFIG_DEVICE_PORT = "Port"
        private const val CONFIG_NETWORK_PROTOCOL = "NetworkProtocol"
        private const val CONFIG_TIMEOUT = "Timeout"
        private const val CONFIG_RETRIES = "Retries"
        private const val CONFIG_BATCH_READ_SIZE = "ReadBatchSize"
        private const val CONFIG_SNMP_VERSION = "SnmpVersion"
        private const val CONFIG_COMMUNITY = "Community"
        private const val CONFIG_WAIT_AFTER_READ_ERROR = "WaitAfterReadError"
        private const val CONFIG_READ_RETRIES = "ReadRetries"


        private val default = SnmpDeviceConfiguration()

        fun create(address: String = default._address,
                   port: Int = default._port,
                   waitAfterReadError: Int = default._waitAfterReadError,
                   readRetries: Int = default._readRetries,
                   community: String = default._community,
                   networkProtocol: SnmpNetworkProtocol? = default._networkProtocol,
                   snmpVersion: Int = default._snmpVersion,
                   timeout: Long = default._timeout,
                   retries: Int = default._retries,
                   readBatchSize: Int = default._readBatchSize): SnmpDeviceConfiguration {

            val instance = SnmpDeviceConfiguration()
            with(instance) {
                _address = address
                _port = port
                _waitAfterReadError = waitAfterReadError
                _readRetries = readRetries
                _community = community
                _networkProtocol = networkProtocol
                _snmpVersion = snmpVersion
                _timeout = timeout
                _retries = retries
                _readBatchSize = readBatchSize
            }
            return instance
        }

    }
}