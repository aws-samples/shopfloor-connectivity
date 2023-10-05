/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.config

import com.amazonaws.sfc.awsiot.GreenGrass2Configuration
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_BYTES_SUFFIX
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CERTIFICATE
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_PRIVATE_KEY
import com.google.gson.annotations.SerializedName
import java.io.File

@ConfigurationClass
open class ClientConfiguration : Validate {
    @SerializedName(CONFIG_CERTIFICATE)
    @Suppress("PropertyName")
    protected var _certificate: String? = null

    /**
     * Device certificate
     */
    val deviceCertificateFile: File? by lazy {
        if (_certificate != null) File(_certificate!!)
        else if (greengrassConfig != null) File(greengrassConfig!!.certificateFilePath)
        else null
    }

    @SerializedName(CONFIG_CERTIFICATE + CONFIG_BYTES_SUFFIX)
    @Suppress("PropertyName")
    protected var _deviceCertificateBytes: ByteArray? = null

    val deviceCertificateBytes: ByteArray?
        get() {
            if (_deviceCertificateBytes == null) {
                _deviceCertificateBytes = deviceCertificateFile?.readBytes()
            }
            return _deviceCertificateBytes
        }

    fun hasCertificateData(): Boolean {
        return deviceCertificateFile != null || deviceCertificateBytes != null
    }

    @SerializedName(CONFIG_PRIVATE_KEY)
    @Suppress("PropertyName")
    protected var _privateKey: String? = null

    /**
     * Device private key
     */
    val privateKeyFile: File? by lazy {
        if (_privateKey != null) File(_privateKey!!)
        else if (greengrassConfig != null) File(greengrassConfig!!.privateKeyPath)
        else null
    }

    @SerializedName(CONFIG_PRIVATE_KEY + CONFIG_BYTES_SUFFIX)
    @Suppress("PropertyName")
    protected var _privateKeyBytes: ByteArray? = null

    val privateKeyBytes: ByteArray?
        get() {
            if (_privateKeyBytes == null) {
                _privateKeyBytes = privateKeyFile?.readBytes()
            }
            return _privateKeyBytes
        }

    fun hasPrivateKeyData(): Boolean {
        return privateKeyFile != null || _privateKeyBytes != null
    }

    @SerializedName(CONFIG_ROOT_CA)
    @Suppress("PropertyName")
    protected var _rootCa: String? = null

    /**
     * Root CA
     */
    val rootCaFile: File? by lazy {
        if (_rootCa != null) File(_rootCa!!)
        else if (greengrassConfig != null) File(greengrassConfig!!.rootCaPath)
        else null
    }

    @SerializedName(CONFIG_ROOT_CA + CONFIG_BYTES_SUFFIX)
    @Suppress("PropertyName")
    protected var _rootCABytes: ByteArray? = null

    val rootCABytes: ByteArray?
        get() {
            if (_rootCABytes == null) {
                _rootCABytes = rootCaFile?.readBytes()
            }
            return _rootCABytes
        }

    fun hasRootCaData(): Boolean {
        return rootCaFile != null || rootCABytes != null
    }


    fun loadKeyAndCertificates() {
        privateKeyBytes
        deviceCertificateBytes
        rootCABytes
    }


    /**
     * Proxy
     */
    @SerializedName(CONFIG_PROXY)
    @Suppress("PropertyName")
    protected var _proxy: ClientProxyConfiguration? = null

    val proxy: ClientProxyConfiguration?
        get() = _proxy ?: greengrassConfig?.proxy

    /**
     * GreenGrass2 deployment path
     */
    @SerializedName(CONFIG_GREENGRASS_DEPLOYMENT_PATH)
    @Suppress("PropertyName")
    protected var _greengrassDeploymentPath: String? = null

    protected val greengrassConfig: GreenGrass2Configuration? by lazy {
        if (_greengrassDeploymentPath != null) GreenGrass2Configuration.load(_greengrassDeploymentPath!!) else null
    }

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
        val checked = listOf(deviceCertificateFile, privateKeyFile, rootCaFile)
        ConfigurationException.check(checked.all { it == null } || checked.all { it != null },
            "Client key, Certificate and RootCa must be specified all or none",
            "Client",
            this
        )

        checked.filterNotNull().forEach {
            ConfigurationException.check(
                it.exists(),
                "Client configuration file ${it.absolutePath} does not exists or is not accessible",
                "Client",
                this
            )
        }

        validated = true
    }


    fun hasMTLSRequiredCertificatesAndKey(): Boolean =
        (deviceCertificateFile != null && privateKeyFile != null && rootCaFile != null) ||
        (deviceCertificateBytes != null && privateKeyBytes != null && rootCABytes != null)

    companion object {
        const val CONFIG_ROOT_CA = "RootCa"
        const val CONFIG_PROXY = "Proxy"
        const val CONFIG_GREENGRASS_DEPLOYMENT_PATH = "GreenGrassDeploymentPath"

        private val default = ClientConfiguration()

        fun create(certificate: String? = default._certificate,
                   certificateBytes: ByteArray? = default._deviceCertificateBytes,
                   privateKey: String? = default._privateKey,
                   privateKeyBytes: ByteArray? = default._privateKeyBytes,
                   rootCa: String? = default._rootCa,
                   rootCaBytes: ByteArray? = default._rootCABytes,
                   proxy: ClientProxyConfiguration? = default._proxy,
                   greenGrassDeploymentPath: String? = default._greengrassDeploymentPath): ClientConfiguration {

            val instance = ClientConfiguration()

            @Suppress("DuplicatedCode")
            with(instance) {
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