package com.amazonaws.sfc.mqtt

import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName
import java.io.File
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

open class MqttConnectionOptions : Validate {


    @SerializedName(CONFIG_BROKER_ADDRESS)
    private var _address = ""

    @SerializedName(CONFIG_MQTT_END_POINT)
    protected var _endPoint: String = ""
    val endPoint: String by lazy {

        if (_endPoint.isEmpty()) _endPoint = _address

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
                "${MqttConnectionProtocol.SSL.protocolPrefix}_endPoint"
            }
        }
    }

    val protocol: MqttConnectionProtocol? by lazy {
        MqttConnectionProtocol.fromAddress(endPoint)
    }

    @SerializedName(CONFIG_MQTT_PORT)
    protected

    var _port: Int? = null
    val port: Int?
        get() = _port

    @SerializedName(CONFIG_MQTT_CERTIFICATE)
    protected var _certificate: String? = null
    val certificate: File?
        get() = if (_certificate != null) File(_certificate!!) else null

    @SerializedName(CONFIG_MQTT_PRIVATE_KEY)
    protected var _privateKey: String? = null
    val privateKey: File?
        get() = if (_privateKey != null) File(_privateKey!!) else null

    @SerializedName(CONFIG_MQTT_ROOT_CA)
    protected var _rootCA: String? = null
    val rootCA: File?
        get() = if (_rootCA != null) File(_rootCA!!) else null

    @SerializedName(CONFIG_MQTT_SSL_SERVER_CERT)
    protected var _sslServerCert: String? = null
    val sslServerCert: File?
        get() = if (_sslServerCert != null) File(_sslServerCert!!) else null

    @SerializedName(CONFIG_MQTT_USERNAME)
    protected var _username: String? = null
    val username: String?
        get() = _username

    @SerializedName(CONFIG_MQTT_PASSWORD)
    protected var _password: String? = null
    val password: String?
        get() = _password

    @SerializedName(CONFIG_MQTT_CONNECT_TIMEOUT)
    protected var _connectTimeout = DEFAULT_CONNECT_TIMEOUT
    val connectTimeout: Duration = _connectTimeout.toDuration(DurationUnit.SECONDS)

    val address
        get() = endPoint.substringAfter("://")

    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    override fun validate() {
        validateProtocol()
        checkProtocolRequirements()
        checkRequiredSettings()



        validated = true
    }

    private fun validateProtocol() {
        if (MqttConnectionProtocol.validValues.none { _endPoint.startsWith(it) } && (_endPoint.contains("""^\w+://"""))) {
            throw ConfigurationException(
                "${_endPoint.substringBefore("://")}:// of $CONFIG_MQTT_END_POINT is not a valid protocol, valid protocols are ${MqttConnectionProtocol.validValues}",
                CONFIG_MQTT_END_POINT, this
            )
        }
    }

    private fun checkProtocolRequirements() {
        when (protocol) {
            MqttConnectionProtocol.TCP -> checkTcpRequirements()
            MqttConnectionProtocol.SSL -> checkSslRequirements()
            null -> throw ConfigurationException("No protocol specified for MQTT connection end point", CONFIG_MQTT_END_POINT, this)
        }
    }

    private fun checkTcpRequirements() {
        checkEndPointProtocol()
    }


    private fun checkSslRequirements() {
        checkEndPointProtocol()
        checkIfSetAndExist(rootCA, CONFIG_MQTT_ROOT_CA)
        checkIfSetAndExist(certificate, CONFIG_MQTT_CERTIFICATE)
        checkIfSetAndExist(privateKey, CONFIG_MQTT_PRIVATE_KEY)
        sslServerCertMustExistIfConfigured()
    }

    private fun checkEndPointProtocol() {
        ConfigurationException.check(
            endPoint.startsWith(protocol?.protocolPrefix ?: ""),
            "$CONFIG_MQTT_END_POINT must start with ${MqttConnectionProtocol.TCP.protocolPrefix}",
            CONFIG_MQTT_END_POINT,
            this
        )
    }


    private fun checkIfSetAndExist(f: File?, n: String) {
        ConfigurationException.check(
            f != null && f.exists(),
            "$n ${f?.absolutePath} does not exist}}",
            n,
            this
        )
    }

    private fun sslServerCertMustExistIfConfigured() {
        ConfigurationException.check(
            sslServerCert == null || sslServerCert?.exists() == true,
            "$CONFIG_MQTT_SSL_SERVER_CERT ${sslServerCert?.absolutePath} does not exist",
            CONFIG_MQTT_SSL_SERVER_CERT,
            this
        )
    }


    // Checks if al required attributes are set
    private fun checkRequiredSettings() {
        ConfigurationException.check(
            endPoint.isNotEmpty() || _address.isNotEmpty(),
            "$CONFIG_MQTT_END_POINT for MQTT target must be set",
            CONFIG_MQTT_END_POINT,
            this
        )

        ConfigurationException.check(
            port != null,
            "$CONFIG_MQTT_PORT for MQTT target must be set",
            CONFIG_MQTT_PORT,
            this
        )
    }

    companion object {
        const val CONFIG_MQTT_END_POINT = "EndPoint"
        const val CONFIG_MQTT_CERTIFICATE = "Certificate"
        const val CONFIG_MQTT_PRIVATE_KEY = "PrivateKey"
        const val CONFIG_MQTT_ROOT_CA = "RootCA"
        const val CONFIG_MQTT_SSL_SERVER_CERT = "SslServerCertificate"
        const val CONFIG_MQTT_CONNECT_TIMEOUT = "ConnectTimeout"
        const val CONFIG_MQTT_USERNAME = "Username"
        const val CONFIG_MQTT_PASSWORD = "Password"
        const val CONFIG_MQTT_PORT = "Port"

        // included as this was setting has been renamed to CONFIG_END_POINT
        const val CONFIG_BROKER_ADDRESS = "Address"

        const val DEFAULT_CONNECT_TIMEOUT = 10

        private val default = MqttConnectionOptions()

        fun create(
            endPoint: String = default._endPoint,
            port: Int? = default._port,
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
                _endPoint = endPoint
                _port = port
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