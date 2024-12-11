// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
//

package com.amazonaws.sfc.crypto

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName
import java.io.File

@ConfigurationClass
open class TlsConfiguration() : Validate {

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


    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    override fun validate() {

        if (validated) return

        ConfigurationException.check(
            !_certificate.isNullOrEmpty(),
            "$CONFIG_CERTIFICATE for $CONFIG_TLS_SSL must be set",
            CONFIG_CERTIFICATE,
            this
        )

        ConfigurationException.check(
            !_privateKey.isNullOrEmpty(),
            "$CONFIG_PRIVATE_KEY for $CONFIG_TLS_SSL must be set",
            CONFIG_PRIVATE_KEY,
            this
        )

        ConfigurationException.check(
            !_rootCA.isNullOrEmpty(),
            "$CONFIG_ROOT_CA for $CONFIG_TLS_SSL must be set",
            CONFIG_ROOT_CA,
            this
        )


        if (_certificate != null && !File(_certificate!!).exists()) {
            validated = false
            throw ConfigurationException("$CONFIG_CERTIFICATE \"$_certificate\" file does not exist", CONFIG_CERTIFICATE, this)
        }

        if (_rootCA != null && !File(_rootCA!!).exists()) {
            validated = false
            throw ConfigurationException("$CONFIG_ROOT_CA \"$_rootCA\" file does not exist", CONFIG_ROOT_CA, this)
        }

        if (_privateKey != null && !File(_privateKey!!).exists()) {
            validated = false
            throw ConfigurationException("$CONFIG_PRIVATE_KEY \"$_privateKey\" file does not exist", CONFIG_PRIVATE_KEY, this)
        }

        if (_sslServerCert != null && !File(_sslServerCert!!).exists()) {
            validated = false
            throw ConfigurationException("$CONFIG_SSL_SERVER_CERT \"$_sslServerCert\" file does not exist", CONFIG_SSL_SERVER_CERT, this)
        }


        validated = true


    }

    companion object {
        const val CONFIG_CERTIFICATE = "Certificate"
        const val CONFIG_PRIVATE_KEY = "PrivateKey"
        const val CONFIG_ROOT_CA = "RootCA"
        const val CONFIG_SSL_SERVER_CERT = "SslServerCertificate"
        const val CONFIG_TLS_SSL = "Tls"

        private val default = TlsConfiguration()

    fun create(
        certificate: String? = default._certificate,
        privateKey: String? = default._privateKey,
        rootCA: String? = default._rootCA,

    ): TlsConfiguration {
        val instance = TlsConfiguration()
        with(instance) {
            _certificate = certificate
            _privateKey = privateKey
            _rootCA = rootCA

        }
        return instance
    }


    }

}