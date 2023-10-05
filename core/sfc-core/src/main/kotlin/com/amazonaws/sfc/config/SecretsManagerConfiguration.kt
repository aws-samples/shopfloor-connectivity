/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CERTIFICATES_AND_KEYS_BY_FILE_REFERENCE
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CREDENTIAL_PROVIDER_CLIENT
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_PRIVATE_KEY
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_REGION
import com.amazonaws.sfc.secrets.CloudSecretConfiguration
import com.amazonaws.sfc.system.homeDirectory
import com.google.gson.annotations.SerializedName
import software.amazon.awssdk.regions.Region
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

@ConfigurationClass
class SecretsManagerConfiguration : AwsServiceConfig, Validate {

    @SerializedName(CONFIG_CLOUD_SECRETS)
    private var _cloudSecrets: List<CloudSecretConfiguration>? = null
    var cloudSecrets: List<CloudSecretConfiguration>?
        get() = _cloudSecrets
        set(value) {
            _cloudSecrets = value
        }

    @SerializedName(CONFIG_PRIVATE_KEY)
    private var _privateKeyFilePath: String? = null
    val privateKeyFilePath: String
        get() = privateKeyFilePathOrNull ?: Path(storedSecretsDirectory, DEFAULT_PRIVATE_KEY_FILE).absolutePathString()

    val privateKeyFilePathOrNull: String?
        get() {
            if (_privateKeyFilePath != null) {
                return _privateKeyFilePath as String
            }
            if (greengrassConfig != null) {
                return greengrassConfig!!.privateKeyPath
            }
            return null
        }


    @SerializedName(CONFIG_PRIVATE_KEY + CONFIG_BYTES_SUFFIX)
    private var _privateKeyBytes: ByteArray? = null

    val privateKeyBytes: ByteArray?
        get() {
            if (_privateKeyBytes == null) {
                _privateKeyBytes = File(privateKeyFilePath).readBytes()
            }
            return _privateKeyBytes
        }

    @SerializedName(CONFIG_CERTIFICATES_AND_KEYS_BY_FILE_REFERENCE)
    private var _certificatesAndKeysByFileReference: Boolean = false

    private val certificatesAndKeysByFileReference: Boolean
        get() = _certificatesAndKeysByFileReference

    @SerializedName(CONFIG_CREATE_PRIVATE_KEY_IF_NO_EXIST)
    private var _createPrivateKeyIfNotExist: Boolean = true
    val createPrivateKeyIfNotExist: Boolean
        get() = _createPrivateKeyIfNotExist

    @SerializedName(CONFIG_SECRETS_FILE)
    private var _storedSecretsFilePath: String = ""
    val storedSecretsFilePath: String
        get() = _storedSecretsFilePath

    @SerializedName(CONFIG_SECRETS_DIR)
    private var _storedSecretsDirectory: String? = null
    val storedSecretsDirectory: String
        get() = _storedSecretsDirectory ?: DEFAULT_SECRETS_DIR


    @SerializedName(CONFIG_CREDENTIAL_PROVIDER_CLIENT)
    private var _credentialProvideClient: String? = null

    override val credentialProviderClient: String?
        get() = _credentialProvideClient

    @SerializedName(CONFIG_REGION)
    private var _region: String? = null

    override val region: Region?
        get() = if (_region.isNullOrEmpty()) null else Region.of(_region!!.lowercase())

    /**
     * GreenGrass2 deployment path
     */
    @SerializedName(ClientConfiguration.CONFIG_GREENGRASS_DEPLOYMENT_PATH)
    private var _greengrassDeploymentPath: String? = null

    val greengrassConfig: GreenGrass2Configuration? by lazy {
        if (_greengrassDeploymentPath != null) GreenGrass2Configuration.load(_greengrassDeploymentPath!!) else null
    }


    private var validatedFlag = false
    override var validated
        get() = validatedFlag
        set(value) {
            validatedFlag = value
        }

    val placeholderNames: Set<String>
        get() {
            return cloudSecrets?.flatMap {
                it.placeholderNames
            }?.toSet() ?: emptySet()
        }

    fun asConfigurationMap(): Map<String, Any?> {

        // Fields that are always included
        val map = mutableMapOf<String, Any?>(
            CONFIG_CERTIFICATES_AND_KEYS_BY_FILE_REFERENCE to certificatesAndKeysByFileReference,
            CONFIG_SECRETS_FILE to secretsFilename,
            CONFIG_SECRETS_DIR to storedSecretsDirectory

        )

        if (cloudSecrets != null) {
            map[CONFIG_CLOUD_SECRETS] = cloudSecrets!!.map { it.asConfigurationMap() }
        }

        if (credentialProviderClient.isNullOrEmpty()) {
            map[CONFIG_CREDENTIAL_PROVIDER_CLIENT] = credentialProviderClient
        }

        if (_region != null) {
            map[CONFIG_REGION] = _region
        }

        if (certificatesAndKeysByFileReference) {
            // Include names of the files
            map[CONFIG_PRIVATE_KEY] = privateKeyFilePath
            map[CONFIG_CREATE_PRIVATE_KEY_IF_NO_EXIST] = createPrivateKeyIfNotExist
        } else {
            map["${CONFIG_PRIVATE_KEY}Bytes"] = privateKeyBytes
        }

        return map
    }

    /**
     * Validates the configuration
     * @throws ConfigurationException
     */
    override fun validate() {
        if (validated) return
        _cloudSecrets?.forEach { it.validate() }
        validated = true
    }

    val secretsFilename: String
        get() = storedSecretsFilePath.ifEmpty { Path(storedSecretsFilePath, DEFAULT_SECRETS_FILE).absolutePathString() }

    companion object {
        private const val DEFAULT_PRIVATE_KEY_FILE = "sfc-secrets-manager-private-key.pem"
        private const val DEFAULT_SECRETS_FILE = "sfc-secrets-manager-secrets"
        const val CONFIG_SECRETS_FILE = "StoredSecretsFile"
        const val CONFIG_SECRETS_DIR = "StoredSecretsDir"
        const val CONFIG_CLOUD_SECRETS = "Secrets"
        const val CONFIG_CREATE_PRIVATE_KEY_IF_NO_EXIST = "CreatePrivateKeyIfNotExist"
        val DEFAULT_SECRETS_DIR: String = homeDirectory


        private val default = SecretsManagerConfiguration()

        fun create(secrets: List<CloudSecretConfiguration>? = default._cloudSecrets,
                   privateKey: String? = default._privateKeyFilePath,
                   privateKeyBytes: ByteArray? = default._privateKeyBytes,
                   certificatesAndKeysByFileReference: Boolean = default._certificatesAndKeysByFileReference,
                   storedSecretsFile: String = default._storedSecretsFilePath,
                   storedSecretsDir: String? = default._storedSecretsDirectory,
                   createPrivateKeyIfNotExist: Boolean = default._createPrivateKeyIfNotExist,
                   credentialProviderClient: String? = default._credentialProvideClient,
                   region: String? = default._region,
                   greenGrassDeploymentPath: String? = default._greengrassDeploymentPath): SecretsManagerConfiguration {

            val instance = SecretsManagerConfiguration()

            @Suppress("DuplicatedCode")
            with(instance) {
                _cloudSecrets = secrets
                _privateKeyFilePath = privateKey
                _privateKeyBytes = privateKeyBytes
                _certificatesAndKeysByFileReference = certificatesAndKeysByFileReference
                _storedSecretsFilePath = storedSecretsFile
                _storedSecretsDirectory = storedSecretsDir
                _createPrivateKeyIfNotExist = createPrivateKeyIfNotExist
                _credentialProvideClient = credentialProviderClient
                _region = region
                _greengrassDeploymentPath = greenGrassDeploymentPath
            }
            return instance
        }


    }

}

