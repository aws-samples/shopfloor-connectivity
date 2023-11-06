
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.config

import com.amazonaws.sfc.crypto.CertificateConfiguration.Companion.CONFIG_CERT_DEFAULT_EXPIRATION_WARNING_PERIOD
import com.amazonaws.sfc.crypto.CertificateConfiguration.Companion.CONFIG_CERT_EXPIRATION_WARNING_PERIOD
import com.amazonaws.sfc.service.ServerConnectionType
import com.google.gson.annotations.SerializedName
import java.io.File

/**
 * Base class for configuration of IPC source or target address
 */
@ConfigurationClass
open class ServerConfiguration : Validate {


    @SerializedName(CONFIG_SERVER_PORT)
    private var _port: Int? = null

    /**
     * Port number used by IPC server
     */
    val port: Int
        get() = _port ?: 0

    @SerializedName(CONFIG_SERVER_ADDRESS)
    private var _address = DEFAULT_SERVER_ADDRESS

    /**
     * IP address of IPC server
     */
    val address: String
        get() = _address

    /**
     * String representation of address and port
     */
    val addressStr
        get() = "$address:$port"

    @SerializedName(CONFIG_CLIENT_CERTIFICATE)
    private var _clientCertificate: String? = null

    /**
     * Client Certificate file to use for SSL connection with server
     */
    val clientCertificate: File?
        get() = if (_clientCertificate != null) File(_clientCertificate!!) else null

    @SerializedName(CONFIG_CLIENT_PRIVATE_KEY)
    private var _clientPrivateKey: String? = null

    /**
     * Client private key file to use for SSL connection with server
     */
    val clientPrivateKey: File?
        get() = if (_clientPrivateKey != null) File(_clientPrivateKey!!) else null


    @SerializedName(CONFIG_CA_CERTIFICATE)
    private var _caCertificate: String? = null

    /**
     * CA certificate file to use for SSL connection with server
     */
    val caCertificate: File?
        get() = if (_caCertificate != null) File(_caCertificate!!) else null

    @SerializedName(CONFIG_SERVER_CERTIFICATE)
    private var _serverCertificate: String? = null

    /**
     * Server Certificate file to use for SSL connection with server
     */
    val serverCertificate: File?
        get() = if (_serverCertificate != null) File(_serverCertificate!!) else null

    @SerializedName(CONFIG_SERVER_PRIVATE_KEY)
    private var _serverPrivateKey: String? = null

    /**
     * Server private key file to use for SSL connection with server
     */
    val serverPrivateKey: File?
        get() = if (_serverPrivateKey != null) File(_serverPrivateKey!!) else null


    @SerializedName(CONFIG_CONNECTION_TYPE)
    private var _serverConnectionType: String = ServerConnectionType.PlainText.name

    val serverConnectionType: ServerConnectionType
        get() = ServerConnectionType.valueOf(_serverConnectionType)

    @SerializedName(CONFIG_CERT_EXPIRATION_WARNING_PERIOD)
    private var _expirationWarningPeriod = CONFIG_CERT_DEFAULT_EXPIRATION_WARNING_PERIOD
    val expirationWarningPeriod: Int
        get() = _expirationWarningPeriod

    @SerializedName(CONFIG_COMPRESSION)
    private var _compression = false
    val compression: Boolean
        get() = _compression

    private var _usedByServer: Boolean = false
    val usedByServer: Boolean
        get() = _usedByServer

    @SerializedName(CONFIG_HEALTH_PROBE)
    private val _healthProbeConfiguration: HealthProbeConfiguration? = null
    val healthProbeConfiguration: HealthProbeConfiguration?
        get() = _healthProbeConfiguration

    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    /**
     * Validates the configuration
     * @throws ConfigurationException
     */
    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return
        validateServerPort()
        validateServerName()
        validateConnectionType()
        validateKeyAndCertificates()
        validated = true
    }

    private fun validateConnectionType() {
        try {
            ServerConnectionType.valueOf(_serverConnectionType)
        } catch (e: Exception) {
            throw ConfigurationException("$CONFIG_CONNECTION_TYPE \"$_serverConnectionType\" is not a valid connection type, " +
                                         "valid values are ${ServerConnectionType.validValues}",
                CONFIG_CONNECTION_TYPE, this)
        }
    }

    // validates certificate/key file
    private fun validateKeyAndCertificates() {

        when (serverConnectionType) {
            ServerConnectionType.MutualTLS -> checkMutualTlsCertificatesAndKey()
            ServerConnectionType.ServerSideTLS -> checkServerSideTlsCertificatesAndKey()
            else -> return
        }
    }

    private fun checkServerSideTlsCertificatesAndKey() {
        if (!usedByServer) return
        checkIfFileIsSpecifiedAndExists(serverCertificate, CONFIG_SERVER_CERTIFICATE, ServerConnectionType.ServerSideTLS)
        checkIfFileIsSpecifiedAndExists(serverPrivateKey, CONFIG_SERVER_PRIVATE_KEY, ServerConnectionType.ServerSideTLS)
    }

    private fun checkMutualTlsCertificatesAndKey() {

        var checkedFile = if (usedByServer) serverCertificate else clientCertificate
        var where = if (usedByServer) CONFIG_SERVER_CERTIFICATE else CONFIG_CLIENT_CERTIFICATE

        checkIfFileIsSpecifiedAndExists(checkedFile, where, ServerConnectionType.MutualTLS)

        checkedFile = if (usedByServer) serverPrivateKey else clientPrivateKey
        where = if (usedByServer) CONFIG_SERVER_PRIVATE_KEY else CONFIG_CLIENT_PRIVATE_KEY
        checkIfFileIsSpecifiedAndExists(checkedFile, where, ServerConnectionType.MutualTLS)

        checkIfFileIsSpecifiedAndExists(caCertificate, CONFIG_CA_CERTIFICATE, ServerConnectionType.MutualTLS)

    }

    private fun checkIfFileIsSpecifiedAndExists(checkedFile: File?, where: String, connectionType: ServerConnectionType) {
        ConfigurationException.check(checkedFile != null,
            "$where required for connection type $connectionType is not specified",
            where,
            this)

        ConfigurationException.check(
            checkedFile!!.exists(),
            "$where \"${checkedFile.absolutePath}\" does not exist",
            where,
            this
        )
    }

    // Validates server name or address
    private fun validateServerName() =
        ConfigurationException.check(
            address.isNotBlank(),
            "$CONFIG_SERVER_ADDRESS for IPC server name can not be empty",
            CONFIG_SERVER_ADDRESS,
            this
        )

    // Validates server name or address
    private fun validateServerPort() =
        ConfigurationException.check(
            (_port != null),
            "$CONFIG_SERVER_PORT not specified",
            CONFIG_SERVER_PORT,
            this
        )

    companion object {
        const val DEFAULT_SERVER_ADDRESS = "localhost"
        const val CONFIG_CLIENT_CERTIFICATE = "ClientCertificate"
        const val CONFIG_CLIENT_PRIVATE_KEY = "ClientPrivateKey"
        const val CONFIG_SERVER_CERTIFICATE = "ServerCertificate"
        const val CONFIG_SERVER_PRIVATE_KEY = "ServerPrivateKey"
        const val CONFIG_CA_CERTIFICATE = "CaCertificate"
        const val CONFIG_SERVER_ADDRESS = "Address"
        const val CONFIG_SERVER_PORT = "Port"
        const val CONFIG_HEALTH_PROBE = "HealthProbe"
        const val CONFIG_CONNECTION_TYPE = "ConnectionType"
        const val CONFIG_COMPRESSION = "Compression"

        private val default = ServerConfiguration()

        fun create(port: Int? = default._port,
                   address: String = default._address,
                   compression: Boolean = default._compression,
                   clientCertificate: String? = default._clientCertificate,
                   clientPrivateKey: String? = default._clientPrivateKey,
                   serverCertificate: String? = default._serverCertificate,
                   serverPrivateKey: String? = default._serverPrivateKey,
                   caCertificate: String? = default._caCertificate,
                   serverConnectionType: ServerConnectionType = ServerConnectionType.valueOf(default._serverConnectionType),
                   usedByServer: Boolean = default._usedByServer): ServerConfiguration {

            val instance = ServerConfiguration()

            with(instance) {
                _port = port
                _address = address
                _compression = compression
                _clientCertificate = clientCertificate
                _caCertificate = caCertificate
                _clientPrivateKey = clientPrivateKey
                _serverCertificate = serverCertificate
                _serverPrivateKey = serverPrivateKey
                _serverConnectionType = serverConnectionType.name
                _usedByServer = usedByServer
            }
            return instance
        }

    }
}

