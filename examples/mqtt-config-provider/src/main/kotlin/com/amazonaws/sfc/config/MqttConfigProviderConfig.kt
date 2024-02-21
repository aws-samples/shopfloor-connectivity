// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_TARGETS
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Connection
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName
import java.io.File


@ConfigurationClass
class MqttConfigProviderConfig : Validate {

    @SerializedName(CONFIG_TOPIC_NAME)

    private var _topicName: String = ""
    val topicName: String
        get() = _topicName

    @SerializedName(CONFIG_END_POINT)
    private var _endpoint: String = ""

    val endpoint: String by lazy {

        val hasPort = Regex("""(.+):(\d+)${'$'}$""")
        val m = hasPort.find(_endpoint)
        val s = if (m == null) _endpoint else {
            if (_port == null) {
                _port = m.groups[1]!!.value.toInt()
            }
            m.groups[0]?.value.toString()
        }

        val hasProtocol = Regex("""^w+://""")
        if (hasProtocol.matches(_endpoint)) {
            _endpoint
        } else
            when (connection) {
                Connection.SSL -> "ssl://$s"
                Connection.MUTUAL -> "ssl://$s"
                Connection.PLAINTEXT -> "tcp://$s"
            }
    }

    @SerializedName(CONFIG_PORT)
    private var _port: Int? = null

    val port: Int?
        get() = _port


    @SerializedName(CONFIG_CERTIFICATE)
    private var _certificate: String? = null

    val certificate: File?
        get() = if (_certificate != null) File(_certificate!!) else null

    @SerializedName(CONFIG_PRIVATE_KEY)
    private var _privateKey: String? = null

    val privateKey: File?
        get() = if (_privateKey != null) File(_privateKey!!) else null

    @SerializedName(CONFIG_ROOT_CA)
    private var _rootCA: String? = null

    val rootCA: File?
        get() = if (_rootCA != null) File(_rootCA!!) else null

    @SerializedName(CONFIG_SSL_SERVER_CERT)
    private var _sslServerCert: String? = null

    val sslServerCert: File?
        get() = if (_sslServerCert != null) File(_sslServerCert!!) else null

    @SerializedName(CONFIG_CONNECTION)
    private var _connection: Connection = DEFAULT_CONNECTION

    val connection: Connection
        get() = _connection

    @SerializedName(CONFIG_USERNAME)
    private var _username: String? = null

    val username: String?
        get() = _username


    @SerializedName(CONFIG_PASSWORD)
    private var _password: String? = null

    val password: String?
        get() = _password

    @SerializedName(CONFIG_TARGETS)
    private var _targets: String? = null

    val targets: String?
        get() = _targets

    @SerializedName(CONFIG_LOCAL_CONFIG_FILE)
    private var _localConfigFile: String? = null
    val localConfigFile: String?
        get() = _localConfigFile


    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    @SerializedName(CONFIG_CONNECT_TIMEOUT)
    private var _connectTimeout = DEFAULT_CONNECT_TIMEOUT

    val connectTimeout: Int = _connectTimeout


    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return
        checkRequiredSettings()
        checkConnectionRequirements()

        validated = true
    }

    private fun checkConnectionRequirements() {
        when (_connection) {
            Connection.PLAINTEXT -> planTextRequirements()
            Connection.SSL -> checkServerSideSslRequirements()
            Connection.MUTUAL -> checkMutualTlsRequirements()
        }
    }

    private fun checkMutualTlsRequirements() {
        checkPrefix(_connection, "ssl")
        checkIfSetAndExist(rootCA, CONFIG_ROOT_CA)
        checkIfSetAndExist(certificate, CONFIG_CERTIFICATE)
        checkIfSetAndExist(privateKey, CONFIG_PRIVATE_KEY)
        sslServerCertMustExistIfConfigured()
    }

    private fun checkServerSideSslRequirements() {
        checkPrefix(_connection, "ssl")
        sslServerCertMustExistIfConfigured()
    }

    private fun planTextRequirements() {
        checkPrefix(_connection, "tcp")
    }

    private fun checkIfSetAndExist(f: File?, n: String) {
        ConfigurationException.check(
            f != null && f.exists(),
            "$n must be set and exist for connection $connection}",
            n,
            this
        )
    }

    private fun sslServerCertMustExistIfConfigured() {
        ConfigurationException.check(
            sslServerCert == null || sslServerCert?.exists() == true,
            "$CONFIG_SSL_SERVER_CERT must exist for connection ${Connection.MUTUAL}",
            CONFIG_SSL_SERVER_CERT,
            this
        )
    }

    private fun checkPrefix(c: Connection, s: String) {
        ConfigurationException.check(
            endpoint.startsWith("$s://"),
            "$CONFIG_END_POINT must start with $s:// for connection ${c}",
            CONFIG_END_POINT,
            this
        )
    }


    // Checks if al required attributes are set
    private fun checkRequiredSettings() {
        ConfigurationException.check(
            endpoint.isNotEmpty(),
            "$CONFIG_END_POINT for MQTT target must be set",
            CONFIG_END_POINT,
            this
        )
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

        ConfigurationException.check(
            port != null,
            "$CONFIG_PORT for MQTT target must be set",
            CONFIG_PORT,
            this
        )
    }

    companion object {

        private const val CONFIG_TOPIC_NAME = "TopicName"
        private const val CONFIG_END_POINT = "EndPoint"
        private const val CONFIG_CERTIFICATE = "Certificate"
        private const val CONFIG_PRIVATE_KEY = "PrivateKey"
        private const val CONFIG_ROOT_CA = "RootCA"
        private const val CONFIG_SSL_SERVER_CERT = "SslServerCertificate"
        private const val CONFIG_LOCAL_CONFIG_FILE = "LocalConfigFile"
        private const val CONFIG_CONNECT_TIMEOUT = "ConnectTimeout"
        private const val CONFIG_USERNAME = "Username"
        private const val CONFIG_PASSWORD = "Password"
        private const val CONFIG_CONNECTION = "Connection"
        private const val CONFIG_PORT = "Port"

        private const val DEFAULT_CONNECT_TIMEOUT = 30
        private val DEFAULT_CONNECTION = Connection.PLAINTEXT


        val requiredSettings = listOf(
            "topicName",
            "endpoint",
            "port",
            "localConfigFile"
        )

        private val default = MqttConfigProviderConfig()

        fun create(
            topicName: String = default._topicName,
            endpoint: String = default._endpoint,
            port: Int? = default._port,
            connection: Connection = default._connection,
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
