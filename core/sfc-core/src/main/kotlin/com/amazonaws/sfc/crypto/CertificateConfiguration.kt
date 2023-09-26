/*
 Copyright (c) 2020. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.crypto

import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CERTIFICATE
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_PRIVATE_KEY
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.SelfSignedCertificateConfig
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName
import kotlin.io.path.Path
import kotlin.io.path.extension

@ConfigurationClass
class CertificateConfiguration : Validate {

    @SerializedName(CONFIG_CERTIFICATE)
    private var _certificatePath: String? = null
    val certificatePath: String?
        get() = _certificatePath

    @SerializedName(CONFIG_PRIVATE_KEY)
    private var _keyPath: String? = null
    val keyPath: String?
        get() = _keyPath

    @SerializedName(CONFIG_CERT_ALIAS)
    private var _alias: String = CONFIG_CERT_DEFAULT_ALIAS
    val alias: String
        get() = _alias

    @SerializedName(CONFIG_CERT_PASSWORD)
    private var _password: String? = null
    val password: String?
        get() = _password

    @SerializedName(CONFIG_CERT_SELF_SIGNED_CERTIFICATE)
    private var _selfSignedCertificateConfig: SelfSignedCertificateConfig? = null
    val selfSignedCertificateConfig
        get() = _selfSignedCertificateConfig

    @SerializedName(CONFIG_CERT_FILE_FORMAT)
    private var _format: CertificateFormat? = null
    val format: CertificateFormat
        get() = _format ?: certificateFileFormatFromName()

    @SerializedName(CONFIG_CERT_EXPIRATION_WARNING_PERIOD)
    private var _expirationWarningPeriod = CONFIG_CERT_DEFAULT_EXPIRATION_WARNING_PERIOD
    val expirationWarningPeriod: Int
        get() = _expirationWarningPeriod

    fun certificateFileFormatFromName(): CertificateFormat {
        val certificatePath = _certificatePath ?: return CertificateFormat.Unknown
        var ext = Path(certificatePath).extension.lowercase()
        // strip off possibly used extension for certificate files
        if (listOf("cer", "cert", "crt").contains(ext)) {
            ext = Path(certificatePath.dropLast(ext.length + 1)).extension.lowercase()
        }

        return when (ext) {
            CertificateFormat.Pem.ext -> CertificateFormat.Pem
            CertificateFormat.Pkcs12.ext -> CertificateFormat.Pkcs12
            else -> CertificateFormat.Unknown
        }
    }

    private var _validated = false

    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    override fun validate() {
        selfSignedCertificateConfig?.validate()

        validateCertificateFormat()
        validateCertificateName()

        validated = true
    }

    private fun validateCertificateFormat() {
        ConfigurationException.check(
            format != CertificateFormat.Unknown,
            "$CONFIG_CERT_FILE_FORMAT not set to a valid format ${CertificateFormat.Pem} or ${CertificateFormat.Pkcs12} and format could not be determined from the certificate file name $CONFIG_CERTIFICATE \"${_certificatePath}\"",
            CONFIG_CERT_FILE_FORMAT,
            this
        )
    }

    private fun validateCertificateName() {
        ConfigurationException.check(
            format == CertificateFormat.Pem || !_keyPath.isNullOrEmpty(),
            "$CONFIG_PRIVATE_KEY must be set to a file to create or read the private key from",
            CONFIG_PRIVATE_KEY,
            this
        )
    }

    companion object {

        private val default = CertificateConfiguration()

        @Suppress("unused")
        fun create(certificate: String? = default._certificatePath,
                   key: String? = default._keyPath,
                   alias: String = default._alias,
                   password: String? = default._password,
                   validationExpirationWarningPeriod: Int = default._expirationWarningPeriod,
                   selfSignedCertificateConfig: SelfSignedCertificateConfig? = default._selfSignedCertificateConfig,
                   usePkcs: CertificateFormat? = default._format) {

            val instance = CertificateConfiguration()
            with(instance) {
                _certificatePath = certificate
                _keyPath = key
                _alias = alias
                _expirationWarningPeriod = validationExpirationWarningPeriod
                _password = password
                _selfSignedCertificateConfig = selfSignedCertificateConfig
                _format = usePkcs

            }
        }

        private const val CONFIG_CERT_PASSWORD = "Password"
        private const val CONFIG_CERT_SELF_SIGNED_CERTIFICATE = "SelfSignedCertificate"
        private const val CONFIG_CERT_FILE_FORMAT = "Format"
        private const val CONFIG_CERT_ALIAS = "Alias"
        private const val CONFIG_CERT_DEFAULT_ALIAS = "alias"
        const val CONFIG_CERT_EXPIRATION_WARNING_PERIOD = "ExpirationWarningPeriod"
        const val CONFIG_CERT_DEFAULT_EXPIRATION_WARNING_PERIOD = 30
    }
}