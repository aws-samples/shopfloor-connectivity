/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.awsiot

import com.amazonaws.sfc.config.*
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CERTIFICATE
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CERTIFICATES_AND_KEYS_BY_FILE_REFERENCE
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_PRIVATE_KEY
import com.amazonaws.sfc.data.JsonHelper
import com.amazonaws.sfc.log.Logger.Companion.HIDDEN_VALUE
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class AwsIotCredentialProviderClientConfiguration : ClientConfiguration(), Validate {

    @SerializedName(IOT_CREDENTIAL_ENDPOINT)
    private var _iotCredentialEndpoint: String? = null

    val iotCredentialEndpoint: String
        get() = _iotCredentialEndpoint ?: greengrassConfig?.iotCredEndpoint ?: ""

    @SerializedName(ROLE_ALIAS)
    private var _roleAlias: String? = null

    val roleAlias: String
        get() = _roleAlias ?: greengrassConfig?.iotRoleAlias ?: ""

    @SerializedName(THING_NAME)
    private var _thingName: String? = null

    val thingName: String
        get() = _thingName ?: greengrassConfig?.thingName ?: ""

    @SerializedName(EXPIRY_CLOCK_SKEW_SECONDS)
    private var _expiryClockSkewSeconds: Int = DEFAULT_EXPIRY_CLOCK_SKEW_SECONDS
    val expiryClockSkewSeconds
        get() = _expiryClockSkewSeconds

    @SerializedName(SKIP_CREDENTIALS_EXPIRY_CHECK)
    private var _skipCredentialsExpiryCheck: Boolean = false
    val skipCredentialsExpiryCheck
        get() = _skipCredentialsExpiryCheck

    @SerializedName(CONFIG_CERTIFICATES_AND_KEYS_BY_FILE_REFERENCE)
    private var _certificatesAndKeysByFileReference: Boolean = false

    private val certificatesAndKeysByFileReference: Boolean
        get() = _certificatesAndKeysByFileReference

    /**
     * Returns the client configuration in JSON format. If the byFileRef flag is set then
     * all key and certificates are passed by filename, otherwise the raw data from the
     * certificate and key files is used.
     * @return String
     */
    fun asConfigurationMap(): Map<String, Any?> {

        // Fields that are always included
        val map = mutableMapOf(
            IOT_CREDENTIAL_ENDPOINT to iotCredentialEndpoint,
            ROLE_ALIAS to roleAlias,
            THING_NAME to thingName,
            SKIP_CREDENTIALS_EXPIRY_CHECK to skipCredentialsExpiryCheck,
            EXPIRY_CLOCK_SKEW_SECONDS to expiryClockSkewSeconds,
            CONFIG_PROXY to proxy
        )

        if (certificatesAndKeysByFileReference) {
            // Include names of the files
            map[CONFIG_CERTIFICATE] = deviceCertificateFile?.toString()
            map[CONFIG_PRIVATE_KEY] = privateKeyFile?.toString()
            map[CONFIG_ROOT_CA] = rootCaFile?.toString()
        } else {
            // Ensure data is loaded and include content of the files
            loadKeyAndCertificates()
            map["${CONFIG_CERTIFICATE}Bytes"] = deviceCertificateBytes
            map["${CONFIG_PRIVATE_KEY}Bytes"] = privateKeyBytes
            map["${CONFIG_ROOT_CA}Bytes"] = rootCABytes
        }

        return map
    }


    /**
     * Validates configuration
     * @throws ConfigurationException
     */
    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return

        super.validate()

        ConfigurationException.check(
            iotCredentialEndpoint.isNotEmpty(),
            "CredentialClient.IotCredentialEndpoint must be specified",
            "CredentialClient.IotCredentialEndpoint",
            this
        )

        ConfigurationException.check(roleAlias.isNotEmpty(), "CredentialClient.RoleAlias must be specified", "CredentialClient.RoleAlias", this)
        ConfigurationException.check(thingName.isNotEmpty(), "CredentialClient.ThingName must be specified", "CredentialClient.ThingName", this)

        ConfigurationException.check((hasCertificateData()), "No certificate data for credential client", "Client.Certificate", this)
        ConfigurationException.check((hasPrivateKeyData()), "No private key data for credential client", "Client.PrivateKey", this)
        ConfigurationException.check((hasRootCaData()), "No Root CA data for credential client", "Client.RootCA", this)


        validateClientCertificateAndKeyFiles()


        validated = true

    }


    private fun validateClientCertificateAndKeyFiles() {

        if (certificatesAndKeysByFileReference) return

        if (deviceCertificateFile != null) {
            ConfigurationException.check(
                deviceCertificateFile!!.exists(),
                "Client certificate \"$deviceCertificateFile\" does not exist",
                "Client.Certificate",
                this
            )
        }

        if (privateKeyFile != null) {
            ConfigurationException.check(
                (privateKeyFile?.exists() == true),
                "Private key \"$privateKeyFile\" does not exist",
                "Client.Key",
                this
            )
        }

        if (rootCaFile != null) {
            ConfigurationException.check(
                (rootCaFile?.exists() == true),
                "RootCa \"$rootCaFile\" does not exist",
                "Client.RootCa",
                this
            )
        }
    }

    override fun toString(): String {
        val hidden = "[$HIDDEN_VALUE]"
        return JsonHelper.gsonExtended().toJson(
            mapOf(
                IOT_CREDENTIAL_ENDPOINT to this.iotCredentialEndpoint,
                ROLE_ALIAS to this.roleAlias,
                THING_NAME to this.thingName,
                EXPIRY_CLOCK_SKEW_SECONDS to this.expiryClockSkewSeconds,
                SKIP_CREDENTIALS_EXPIRY_CHECK to this.skipCredentialsExpiryCheck,
                CONFIG_CERTIFICATE to if (this.deviceCertificateFile != null) this.deviceCertificateFile?.absolutePath else "null",
                "${CONFIG_CERTIFICATE}Bytes" to if (this.deviceCertificateBytes?.isEmpty() == true) "[]" else hidden,
                CONFIG_PRIVATE_KEY to if (this.privateKeyFile != null) this.privateKeyFile?.absolutePath else "null",
                "${CONFIG_PRIVATE_KEY}Bytes" to if (this.privateKeyBytes?.isEmpty() == true) "[]" else hidden,
                CONFIG_ROOT_CA to if (this.rootCaFile != null) this.rootCaFile?.absolutePath else "null",
                "${CONFIG_ROOT_CA}Bytes" to if (this.rootCABytes?.isEmpty() == true) "[]" else hidden
            )
        )
    }


    companion object {
        const val IOT_CREDENTIAL_ENDPOINT = "IotCredentialEndpoint"
        const val ROLE_ALIAS = "RoleAlias"
        const val THING_NAME = "ThingName"
        const val EXPIRY_CLOCK_SKEW_SECONDS = "ExpiryClockSkewSeconds"
        const val DEFAULT_EXPIRY_CLOCK_SKEW_SECONDS = 300
        const val SKIP_CREDENTIALS_EXPIRY_CHECK = "SkipCredentialsExpiryCheck"

        private val default = AwsIotCredentialProviderClientConfiguration()

        fun create(iotCredentialEndpoint: String? = default._iotCredentialEndpoint,
                   roleAlias: String? = default._roleAlias,
                   thingName: String? = default._thingName,
                   expiryClockSkewSeconds: Int = default._expiryClockSkewSeconds,
                   skipCredentialsExpiryCheck: Boolean = default._skipCredentialsExpiryCheck,
                   certificatesAndKeysByFileReference: Boolean = default._certificatesAndKeysByFileReference,
                   certificate: String? = default._certificate,
                   certificateBytes: ByteArray? = default._deviceCertificateBytes,
                   privateKey: String? = default._privateKey,
                   privateKeyBytes: ByteArray? = default._privateKeyBytes,
                   rootCa: String? = default._rootCa,
                   rootCaBytes: ByteArray? = default._rootCABytes,
                   proxy: ClientProxyConfiguration? = default._proxy,
                   greenGrassDeploymentPath: String? = default._greengrassDeploymentPath): AwsIotCredentialProviderClientConfiguration {

            val instance = AwsIotCredentialProviderClientConfiguration()

            @Suppress("DuplicatedCode")
            with(instance) {
                _iotCredentialEndpoint = iotCredentialEndpoint
                _roleAlias = roleAlias
                _thingName = thingName
                _expiryClockSkewSeconds = expiryClockSkewSeconds
                _skipCredentialsExpiryCheck = skipCredentialsExpiryCheck
                _certificatesAndKeysByFileReference = certificatesAndKeysByFileReference
                _certificate = certificate
                _deviceCertificateBytes = certificateBytes
                _privateKey = privateKey
                _privateKeyBytes = privateKeyBytes
                _rootCa = rootCa
                _rootCABytes = rootCaBytes
                _proxy = proxy
                _greengrassDeploymentPath = greenGrassDeploymentPath
            }
            return instance
        }


    }


}