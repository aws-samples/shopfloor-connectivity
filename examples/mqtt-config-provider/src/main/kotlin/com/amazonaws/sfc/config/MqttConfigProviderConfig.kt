// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.mqtt.MqttConnectionType
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.mqtt.MqttConnectionOptions
import com.google.gson.annotations.SerializedName


@ConfigurationClass
class MqttConfigProviderConfig : Validate, MqttConnectionOptions() {

    @SerializedName(CONFIG_TOPIC_NAME)

    private var _topicName: String = ""
    val topicName: String
        get() = _topicName


    @SerializedName(CONFIG_LOCAL_CONFIG_FILE)
    private var _localConfigFile: String? = null
    val localConfigFile: String?
        get() = _localConfigFile


    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return
        super.validated
        checkRequiredSettings()

        validated = true
    }

    private fun checkRequiredSettings() {
        ConfigurationException.check(
            _topicName.isNotEmpty(),
            "$CONFIG_TOPIC_NAME for MQTT target must be set",
            CONFIG_TOPIC_NAME,
            this
        )

        ConfigurationException.check(
            localConfigFile != null,
            "$CONFIG_LOCAL_CONFIG_FILE for MQTT target must be set",
            CONFIG_LOCAL_CONFIG_FILE,
            this
        )
    }


    companion object {

        private const val CONFIG_TOPIC_NAME = "TopicName"
        private const val CONFIG_LOCAL_CONFIG_FILE = "LocalConfigFile"


        private val default = MqttConfigProviderConfig()

        fun create(
            topicName: String = default._topicName,
            endpoint: String = default._endpoint,
            port: Int? = default._port,
            connection: MqttConnectionType = default._connection,
            username: String? = default._username,
            password: String? = default._password,
            certificate: String? = default._certificate,
            privateKey: String? = default._privateKey,
            rootCA: String? = default._rootCA,
            sslServerCert: String? = default._sslServerCert,
            localConfigFile: String? = default._localConfigFile,
            connectTimeout: Int = default._connectTimeout
        ): MqttConfigProviderConfig {


            val instance = MqttConfigProviderConfig()
            with(instance) {
                _topicName = topicName
                _endpoint = endpoint
                _port = port
                _connection = connection
                _username = username
                _password = password
                _certificate = certificate
                _privateKey = privateKey
                _rootCA = rootCA
                _sslServerCert = sslServerCert
                _localConfigFile = localConfigFile
                _connectTimeout = connectTimeout
            }
            return instance
        }
    }
}
