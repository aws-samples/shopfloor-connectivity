/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.mqtt.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class MqttBrokerConfiguration : Validate {

    @SerializedName(CONFIG_BROKER_ADDRESS)
    private var _address = ""
    val address: String
        get() = _address

    @SerializedName(CONFIG_BROKER_PORT)
    private var _port = DEFAULT_PORT
    val port: Int
        get() = _port

    @SerializedName(CONFIG_MQTT_PROTOCOL)
    private var _mqttProtocol: MqttProtocol? = null

    val protocol: MqttProtocol
        get() = _mqttProtocol ?: DEFAULT_MQTT_PROTOCOL

    val endPoint
        get() = "$protocol://${address}:${port}"

    @SerializedName(CONFIG_CONNECT_TIMEOUT)
    private var _connectTimeout: Long = DEFAULT_CONNECT_TIMEOUT_MS
    val connectTimeout: Duration
        get() = _connectTimeout.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_WAIT_AFTER_CONNECT_ERROR)
    private var _waitAfterConnectError: Long = DEFAULT_WAIT_AFTER_CONNECT_ERROR
    val waitAfterConnectError: Duration
        get() = _waitAfterConnectError.toDuration(DurationUnit.MILLISECONDS)

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
        validated = true

    }

    private fun atLeastOneSecond(t: Duration) = t >= (1000.toDuration(DurationUnit.MILLISECONDS))


    private fun validateWaitAfterConnectError() =
        ConfigurationException.check(
            atLeastOneSecond(waitAfterConnectError),
            "Wait after connect error $CONFIG_WAIT_AFTER_CONNECT_ERROR must be 1000 (milliseconds) or longer",
            "WaitAfterConnectError",
            this
        )

    private fun validateConnectionTimeout() =
        ConfigurationException.check(
            atLeastOneSecond(connectTimeout),
            "Connect timeout $CONFIG_CONNECT_TIMEOUT must be 1000 (milliseconds) or longer",
            CONFIG_CONNECT_TIMEOUT,
            this
        )

    private fun validatePort() {
        ConfigurationException.check(
            (port > 0),
            "Port must be 1 or higher",
            CONFIG_BROKER_PORT,
            this
        )
    }

    private fun validateAddress() =
        ConfigurationException.check(
            (address.isNotEmpty()),
            "Address of TCP source server can not be empty",
            CONFIG_BROKER_ADDRESS,
            this
        )


    companion object {
        private const val DEFAULT_PORT = 1883
        private const val DEFAULT_CONNECT_TIMEOUT_MS = 10000L
        const val DEFAULT_WAIT_AFTER_CONNECT_ERROR = 10000L
        private val DEFAULT_MQTT_PROTOCOL = MqttProtocol.TCP

        private const val CONFIG_BROKER_ADDRESS = "Address"
        private const val CONFIG_BROKER_PORT = "Port"
        private const val CONFIG_MQTT_PROTOCOL = "MqttProtocol"
        private const val CONFIG_CONNECT_TIMEOUT = "ConnectTimeout"
        private const val CONFIG_WAIT_AFTER_CONNECT_ERROR = "WaitAfterConnectError"

        private val default = MqttBrokerConfiguration()

        fun create(address: String = default._address,
                   port: Int = default._port,
                   mqttProtocol: MqttProtocol? = default._mqttProtocol,
                   connectTimeout: Long = default._connectTimeout,
                   waitAfterConnectError: Long = default._waitAfterConnectError): MqttBrokerConfiguration {

            val instance = MqttBrokerConfiguration()
            with(instance) {
                _address = address
                _port = port
                _mqttProtocol = mqttProtocol
                _connectTimeout = connectTimeout
                _waitAfterConnectError = waitAfterConnectError
            }
            return instance
        }

    }
}