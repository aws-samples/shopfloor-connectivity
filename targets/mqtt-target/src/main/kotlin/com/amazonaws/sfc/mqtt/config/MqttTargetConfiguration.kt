// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.mqtt.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.TargetConfiguration
import com.amazonaws.sfc.mqtt.MqttConnectionOptions
import com.amazonaws.sfc.mqtt.MqttConnectionType
import com.google.gson.annotations.SerializedName
import java.io.File
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@ConfigurationClass
class MqttTargetConfiguration : TargetConfiguration() {

    @SerializedName(CONFIG_QOS)
    private var _qos: Int = QOS_DEFAULT
    val qos
        get() = _qos

    @SerializedName(CONFIG_TOPIC_NAME)
    private var _topicName: String? = null
    val topicName: String?
        get() = _topicName

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

    @SerializedName(MqttConnectionOptions.CONFIG_MQTT_END_POINT)
    private var _endPoint: String = ""
    val endPoint: String by lazy {

        val hasPort = Regex("""(.+):(\d+)${'$'}$""")
        val m = hasPort.find(_endPoint)
        val s = if (m == null) _endPoint else {
            if (_port == null) {
                _port = m.groups[1]!!.value.toInt()
            }
            m.groups[0]?.value.toString()
        }

        val hasProtocol = Regex("""^w+://""")
        if (hasProtocol.matches(_endPoint)) {
            _endPoint
        } else
            when (connection) {
                MqttConnectionType.SSL -> "ssl://$s"
                MqttConnectionType.MUTUAL -> "ssl://$s"
                MqttConnectionType.PLAINTEXT -> "tcp://$s"
            }
    }

    @SerializedName(MqttConnectionOptions.CONFIG_MQTT_PORT)
    private var _port: Int? = null
    val port: Int?
        get() = _port

    @SerializedName(MqttConnectionOptions.CONFIG_MQTT_CONNECTION)
    private var _connection: MqttConnectionType = MqttConnectionOptions.DEFAULT_MQTT_CONNECTION

    val connection: MqttConnectionType
        get() = _connection


    @SerializedName(MqttConnectionOptions.CONFIG_MQTT_CERTIFICATE)
    private var _certificate: String? = null
    val certificate: File?
        get() = if (_certificate != null) File(_certificate!!) else null

    @SerializedName(MqttConnectionOptions.CONFIG_MQTT_PRIVATE_KEY)
    private var _privateKey: String? = null
    val privateKey: File?
        get() = if (_privateKey != null) File(_privateKey!!) else null

    @SerializedName(MqttConnectionOptions.CONFIG_MQTT_ROOT_CA)
    private var _rootCA: String? = null
    val rootCA: File?
        get() = if (_rootCA != null) File(_rootCA!!) else null

    @SerializedName(MqttConnectionOptions.CONFIG_MQTT_SSL_SERVER_CERT)
    private var _sslServerCert: String? = null
    val sslServerCert: File?
        get() = if (_sslServerCert != null) File(_sslServerCert!!) else null

    @SerializedName(MqttConnectionOptions.CONFIG_MQTT_USERNAME)
    private var _username: String? = null
    val username: String?
        get() = _username

    @SerializedName(MqttConnectionOptions.CONFIG_MQTT_PASSWORD)
    private var _password: String? = null
    val password: String?
        get() = _password

    @SerializedName(MqttConnectionOptions.CONFIG_MQTT_CONNECT_TIMEOUT)
    private var _connectTimeout = MqttConnectionOptions.DEFAULT_CONNECT_TIMEOUT
    val connectTimeout: Duration = _connectTimeout.toDuration(DurationUnit.SECONDS)

    val mqttConnectionOptions: MqttConnectionOptions by lazy {
        MqttConnectionOptions.create(
            endPoint = _endPoint,
            port = _port,
            connection = _connection,
            username = _username,
            password = _password,
            certificate = _certificate,
            privateKey = _privateKey,
            rootCA = _rootCA,
            sslServerCert = _sslServerCert,
            connectTimeout = _connectTimeout
        )
    }

    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return
        super.validate()
        validateQos()
        mqttConnectionOptions.validate()
        checkRequiredSettings()
        validated = true
    }

    @Throws(ConfigurationException::class)
    private fun validateQos() {
        ConfigurationException.check(
            _qos in 0..2,
            "$CONFIG_QOS must have value 0, 1 or 2",
            CONFIG_QOS,
            this
        )
    }

    // Checks if al required attributes are set
    private fun checkRequiredSettings() {
        ConfigurationException.check(
            !_topicName.isNullOrEmpty(),
            "$CONFIG_TOPIC_NAME for MQTT target must be set",
            CONFIG_TOPIC_NAME,
            this
        )

    }

    companion object {

        private const val CONFIG_TOPIC_NAME = "TopicName"
        private const val CONFIG_PUBLISH_TIMEOUT = "PublishTimeout"
        private const val DEFAULT_PUBLISH_TIMEOUT = 10
        const val DEFAULT_WAIT_AFTER_CONNECT_ERROR = 10
        private const val CONFIG_WAIT_AFTER_CONNECT_ERROR = "WaitAfterConnectError"
        private const val CONFIG_QOS = "Qos"
        private const val QOS_DEFAULT = 0
        private const val CONFIG_CONNECT_RETRIES = "ConnectRetries"
        private const val CONNECT_RETRIES_DEFAULT = 10

        private val default = MqttTargetConfiguration()

        fun create(
            endpoint: String = default._endPoint,
            port: Int? = default._port,
            connection: MqttConnectionType = default._connection,
            username: String? = default._username,
            password: String? = default._password,
            certificate: String? = default._certificate,
            privateKey: String? = default._privateKey,
            rootCA: String? = default._rootCA,
            sslServerCert: String? = default._sslServerCert,
            connectTimeout: Int = default._connectTimeout,
            topicName: String? = default._topicName,
            qos : Int = default._qos
        ): MqttTargetConfiguration {


            val instance = MqttTargetConfiguration()
            with(instance) {
                _endPoint = endpoint
                _port = port
                _connection = connection
                _username = username
                _password = password
                _certificate = certificate
                _privateKey = privateKey
                _rootCA = rootCA
                _sslServerCert = sslServerCert
                _connectTimeout = connectTimeout
                _topicName = topicName
                _qos = qos
            }
            return instance
        }
    }
}
