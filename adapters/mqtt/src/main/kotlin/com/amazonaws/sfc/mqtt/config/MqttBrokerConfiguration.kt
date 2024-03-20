
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.mqtt.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.mqtt.MqttConnectionOptions
import com.amazonaws.sfc.mqtt.MqttConnectionProtocol
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@ConfigurationClass
class MqttBrokerConfiguration : MqttConnectionOptions(), Validate {



    @SerializedName(CONFIG_WAIT_AFTER_CONNECT_ERROR)
    private var _waitAfterConnectError = DEFAULT_WAIT_AFTER_CONNECT_ERROR
    val waitAfterConnectError: Duration
        get() = _waitAfterConnectError.toDuration(DurationUnit.SECONDS)


    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return
        super.validate()
        validateConnectionTimeout()
        validateWaitAfterConnectError()
        validated = true
    }

    private fun atLeastOneSecond(t: Duration) = t >= (1.toDuration(DurationUnit.SECONDS))


    private fun validateWaitAfterConnectError() =
        ConfigurationException.check(
            atLeastOneSecond(waitAfterConnectError),
            "Wait after connect error $CONFIG_WAIT_AFTER_CONNECT_ERROR must be 1 second or longer",
            "WaitAfterConnectError",
            this
        )

    private fun validateConnectionTimeout() =
        ConfigurationException.check(
            atLeastOneSecond(connectTimeout),
            "Connect timeout $CONFIG_MQTT_CONNECT_TIMEOUT must be 1 second or longer",
            CONFIG_MQTT_CONNECT_TIMEOUT,
            this
        )


    companion object {
        const val DEFAULT_WAIT_AFTER_CONNECT_ERROR = 60
        private const val CONFIG_WAIT_AFTER_CONNECT_ERROR = "WaitAfterConnectError"

        private val default = MqttBrokerConfiguration()

        fun create(
            endpoint: String = default._endPoint,
            port: Int? = default._port,
            username: String? = default._username,
            password: String? = default._password,
            certificate: String? = default._certificate,
            privateKey: String? = default._privateKey,
            rootCA: String? = default._rootCA,
            sslServerCert: String? = default._sslServerCert,
            connectTimeout: Int = default._connectTimeout,
            waitAfterConnectError: Int = default._waitAfterConnectError,
        ): MqttConnectionOptions {


            val instance = MqttBrokerConfiguration()
            with(instance) {
                _endPoint = endpoint
                _port = port
                _username = username
                _password = password
                _certificate = certificate
                _privateKey = privateKey
                _rootCA = rootCA
                _sslServerCert = sslServerCert
                _connectTimeout = connectTimeout
                _waitAfterConnectError= waitAfterConnectError
            }
            return instance
        }


    }
}