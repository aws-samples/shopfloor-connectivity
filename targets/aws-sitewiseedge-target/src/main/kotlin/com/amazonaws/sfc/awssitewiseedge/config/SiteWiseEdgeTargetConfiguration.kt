// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awssitewiseedge.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.TargetConfiguration
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.mqtt.MqttConnectionOptions
import com.amazonaws.sfc.mqtt.MqttConnectionProtocol
import com.google.gson.annotations.SerializedName
import java.io.File
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


@ConfigurationClass
class SiteWiseEdgeTargetConfiguration : TargetConfiguration(), Validate {

    @SerializedName(CONFIG_CLIENT_NAME)
    private var _clientName: String = ""
    val clientName: String
        get() = _clientName

    @SerializedName(CONFIG_TOPIC_NAME)
    private var _topicName: String = DEFAULT_TOPIC_NAME
    val topicName: String
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
    protected var _endPoint: String = ""
    val endPoint: String by lazy {

        val hasPort = Regex("""(.+):(\d+)${'$'}$""")
        val m = hasPort.find(_endPoint)
        if (m == null) _endPoint else {
            if (_port == null) {
                _port = m.groups[1]!!.value.toInt()
            }
            m.groups[0]?.value.toString()
        }

        val hasProtocol = MqttConnectionProtocol.validValues.any { _endPoint.startsWith(it) }
        if (hasProtocol) {
            _endPoint
        } else {
            if (certificate != null || privateKey != null || rootCA != null) {
                "${MqttConnectionProtocol.SSL.protocolPrefix}_endPoint"
            } else {
                "${MqttConnectionProtocol.TCP.protocolPrefix}_endPoint"
            }
        }
    }

    @SerializedName(MqttConnectionOptions.CONFIG_MQTT_PORT)
    private var _port: Int? = null
    val port: Int?
        get() = _port

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

    @SerializedName(MqttConnectionOptions.CONFIG_MQTT_VERIFY_HOSTNAME)
    protected var _verifyHostname: Boolean = true
    val verifyHostname
        get() = _verifyHostname

    @SerializedName(CONFIG_BATCH_COUNT)
    private var _batchCount: Int? = null
    val batchCount
        get() = _batchCount?:0

    @SerializedName(CONFIG_BATCH_SIZE)
    private var _batchSize: Int? = null
    val batchSize
        get() = if (_batchSize != null) _batchSize!! * 1024 else 0

    @SerializedName(CONFIG_BATCH_INTERVAL)
    private var _batchInterval: Int? = null
    val batchInterval : Duration
        get() = _batchInterval?.toDuration(DurationUnit.MILLISECONDS) ?: Duration.INFINITE

    val mqttConnectionOptions: MqttConnectionOptions by lazy {
        MqttConnectionOptions.create(
            endPoint = _endPoint,
            port = _port,
            username = _username,
            password = _password,
            certificate = _certificate,
            privateKey = _privateKey,
            rootCA = _rootCA,
            sslServerCert = _sslServerCert,
            connectTimeout = _connectTimeout,
            verifyHostname = _verifyHostname
        )
    }

    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return
        validateClientName()
        validateBufferSize()
        mqttConnectionOptions.validate()
        validateTopicName()
        super.validate()
        validated = true
    }

    @Throws(ConfigurationException::class)
    private fun validateClientName() {
        val regex = """[a-zA-Z0-9:_-]{1,128}""".toRegex()
        ConfigurationException.check(
            _clientName.matches(regex),
            "$CONFIG_CLIENT_NAME must have Pattern: [a-zA-Z0-9:_-]+ and be between 1 and 128 characters long",
            CONFIG_CLIENT_NAME,
            this
        )
    }
    @Throws(ConfigurationException::class)
    private fun validateTopicName() {
        ConfigurationException.check(
            _topicName.contains(TEMPLATE_CHANNEL),
            "$CONFIG_TOPIC_NAME must have reference to $TEMPLATE_CHANNEL",
            CONFIG_TOPIC_NAME,
            this
        )
    }

    private fun validateBufferSize(){
        ConfigurationException.check(
            (_batchSize == null || (_batchSize!! >= 0)),
            "$CONFIG_BATCH_SIZE must be greater or equal to 0",
            CONFIG_BATCH_SIZE,
            this)
    }


    companion object {
        const val TEMPLATE_PRE_POSTFIX = "%"
        const val TEMPLATE_SOURCE = "${TEMPLATE_PRE_POSTFIX}source${TEMPLATE_PRE_POSTFIX}"
        const val TEMPLATE_TARGET = "${TEMPLATE_PRE_POSTFIX}target${TEMPLATE_PRE_POSTFIX}"
        const val TEMPLATE_CHANNEL = "${TEMPLATE_PRE_POSTFIX}channel${TEMPLATE_PRE_POSTFIX}"
        private const val DEFAULT_TOPIC_NAME = TEMPLATE_CHANNEL

        private const val CONFIG_CLIENT_NAME = "ClientName"
        private const val CONFIG_TOPIC_NAME = "TopicName"
        private const val CONFIG_PUBLISH_TIMEOUT = "PublishTimeout"
        private const val DEFAULT_PUBLISH_TIMEOUT = 10
        const val DEFAULT_WAIT_AFTER_CONNECT_ERROR = 10
        private const val CONFIG_WAIT_AFTER_CONNECT_ERROR = "WaitAfterConnectError"
        private const val CONFIG_CONNECT_RETRIES = "ConnectRetries"
        private const val CONNECT_RETRIES_DEFAULT = 10
        const val CONFIG_BATCH_SIZE = "BatchSize"
        const val CONFIG_BATCH_COUNT = "BatchCount"
        const val CONFIG_BATCH_INTERVAL = "BatchInterval"

        private val default = SiteWiseEdgeTargetConfiguration()

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
            verifyHostname: Boolean = default._verifyHostname,
            topicName: String = default._topicName,
            clientName: String = default._clientName,
            batchCount : Int? = default.batchCount,
            batchSize : Int? = default._batchSize,
            batchInterval : Int? = default._batchInterval
        ): SiteWiseEdgeTargetConfiguration {

            val instance = SiteWiseEdgeTargetConfiguration()
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
                _verifyHostname = verifyHostname
                _topicName = topicName
                _clientName = clientName
                _batchCount = batchCount
                _batchSize= batchSize
                _batchInterval= batchInterval
            }
            return instance
        }
    }
}
