package com.amazonaws.sfc.mqtt

import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.annotations.SerializedName
import java.io.File

open class MqttConnectionOptions {
    @SerializedName(CONFIG_END_POINT)
    protected var _endpoint: String = ""
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
                MqttConnectionType.SSL -> "ssl://$s"
                MqttConnectionType.MUTUAL -> "ssl://$s"
                MqttConnectionType.PLAINTEXT -> "tcp://$s"
            }
    }

    @SerializedName(CONFIG_PORT)
    protected var _port: Int? = null
    val port: Int?
        get() = _port

    @SerializedName(CONFIG_CONNECTION)
    protected var _connection: MqttConnectionType = DEFAULT_CONNECTION

    val connection: MqttConnectionType
        get() = _connection


    @SerializedName(CONFIG_CERTIFICATE)
    protected var _certificate: String? = null
    val certificate: File?
        get() = if (_certificate != null) File(_certificate!!) else null

    @SerializedName(CONFIG_PRIVATE_KEY)
    protected var _privateKey: String? = null
    val privateKey: File?
        get() = if (_privateKey != null) File(_privateKey!!) else null

    @SerializedName(CONFIG_ROOT_CA)
    protected var _rootCA: String? = null
    val rootCA: File?
        get() = if (_rootCA != null) File(_rootCA!!) else null

    @SerializedName(CONFIG_SSL_SERVER_CERT)
    protected var _sslServerCert: String? = null
    val sslServerCert: File?
        get() = if (_sslServerCert != null) File(_sslServerCert!!) else null

    @SerializedName(CONFIG_USERNAME)
    protected var _username: String? = null
    val username: String?
        get() = _username

    @SerializedName(CONFIG_PASSWORD)
    protected var _password: String? = null
    val password: String?
        get() = _password
    private var _validated = false
    var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    @SerializedName(CONFIG_CONNECT_TIMEOUT)
    protected var _connectTimeout = DEFAULT_CONNECT_TIMEOUT
    val connectTimeout: Int = _connectTimeout

    private fun checkConnectionRequirements() {
        when (_connection) {
            MqttConnectionType.PLAINTEXT -> planTextRequirements()
            MqttConnectionType.SSL -> checkServerSideSslRequirements()
            MqttConnectionType.MUTUAL -> checkMutualTlsRequirements()
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
            "$CONFIG_SSL_SERVER_CERT must exist for connection ${MqttConnectionType.MUTUAL}",
            CONFIG_SSL_SERVER_CERT,
            this
        )
    }

    private fun checkPrefix(c: MqttConnectionType, s: String) {
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
            port != null,
            "$CONFIG_PORT for MQTT target must be set",
            CONFIG_PORT,
            this
        )}

    companion object{
        private const val CONFIG_END_POINT = "EndPoint"
        private const val CONFIG_CERTIFICATE = "Certificate"
        private const val CONFIG_PRIVATE_KEY = "PrivateKey"
        private const val CONFIG_ROOT_CA = "RootCA"
        private const val CONFIG_SSL_SERVER_CERT = "SslServerCertificate"
        private const val CONFIG_CONNECT_TIMEOUT = "ConnectTimeout"
        private const val CONFIG_USERNAME = "Username"
        private const val CONFIG_PASSWORD = "Password"
        private const val CONFIG_CONNECTION = "Connection"
        private const val CONFIG_PORT = "Port"

        private const val DEFAULT_CONNECT_TIMEOUT = 30
        private val DEFAULT_CONNECTION = MqttConnectionType.PLAINTEXT
        private val default = MqttConnectionOptions()

        fun create(
            endpoint: String = default._endpoint,
            port: Int? = default._port,
            connection: MqttConnectionType = default._connection,
            username: String? = default._username,
            password: String? = default._password,
            certificate: String? = default._certificate,
            privateKey: String? = default._privateKey,
            rootCA: String? = default._rootCA,
            sslServerCert: String? = default._sslServerCert,
            connectTimeout: Int = default._connectTimeout
        ): MqttConnectionOptions {


            val instance = MqttConnectionOptions()
            with(instance) {
                _endpoint = endpoint
                _port = port
                _connection = connection
                _username = username
                _password = password
                _certificate = certificate
                _privateKey = privateKey
                _rootCA = rootCA
                _sslServerCert = sslServerCert
                _connectTimeout = connectTimeout
            }
            return instance
        }


    }
}