/*
Copyright(c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.secrets

import com.amazonaws.sfc.awsiot.AWSIotException
import com.amazonaws.sfc.config.ClientConfiguration.Companion.CONFIG_GREENGRASS_DEPLOYMENT_PATH
import com.amazonaws.sfc.config.HasSecretsManager
import com.amazonaws.sfc.config.SecretsManagerConfiguration
import com.amazonaws.sfc.config.SecretsManagerConfiguration.Companion.CONFIG_CREATE_PRIVATE_KEY_IF_NO_EXIST
import com.amazonaws.sfc.crypto.*
import com.amazonaws.sfc.log.Logger
import com.amazonaws.sfc.secrets.CloudSecretConfiguration.Companion.ARN_PATTERN
import com.amazonaws.sfc.util.RetryUtils
import com.amazonaws.sfc.util.buildScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.DurationUnit
import kotlin.time.toDuration


class SecretsManager(
    private val secretClient: AwsSecretsManagerService,
    securityService: SecurityService,
    private val secretsFile: EncryptedSecretsFile,
    private val logger: Logger) {

    private val className = this::class.java.simpleName

    private var crypter: Crypter? = null
    private val nameToArnMap = ConcurrentHashMap<String, String>()
    private val cache = ConcurrentHashMap<String, GetSecretValueResponse>()

    val scope = buildScope("SecretsManager")

    val initialized: Deferred<SecretCryptoException?> = scope.async {
        loadCrypter(securityService)
    }

    private var _configuredSecrets = listOf<CloudSecretConfiguration>()
    val configuredSecrets
        get() = _configuredSecrets

    constructor(secretClient: AwsSecretsManagerService, configuration: SecretsManagerConfiguration, logger: Logger) :
            this(secretClient = secretClient,
                securityService = createSecurityService(configuration, logger),
                secretsFile = EncryptedSecretsFile(getSecretsFilename(configuration, logger)),
                logger = logger)


    @Suppress("unused")
    fun getSecretOrNull(secretId: String, versionId: String? = null, versionStage: String? = null): GetSecretValueResponse? =
        try {
            getSecret(secretId, versionId, versionStage)
        } catch (e: SecretManagerSecretNotFoundException) {
            null
        }

    fun getSecret(secretId: String, versionId: String? = null, versionStage: String? = null): GetSecretValueResponse {

        val log = logger.getCtxLoggers(className, "getSecret")

        log.trace("Getting value for secret $secretId, version id  $versionId, version stage $versionStage")

        val arn = getArnForSecretId(secretId)

        if (!versionId.isNullOrEmpty() && !versionStage.isNullOrEmpty()) {
            throw SecretManagerException("Both versionId and Stage are set")
        }

        return when {
            (!(versionId.isNullOrEmpty())) -> {
                cache[arn + versionId] ?: throw SecretManagerException("Version Id \"$versionId\" not found for secret \"$secretId\"")
            }

            (!versionStage.isNullOrEmpty()) -> {
                return cache[arn + versionStage] ?: throw SecretManagerException("Version stage \"$versionStage\" not found for secret \"$secretId\"")
            }

            else -> {
                cache[arn + LATEST_LABEL] ?: throw SecretManagerException("Secret \"$secretId\" not found")
            }
        }
    }

    val secrets: Map<String, String>
        get() =
            nameToArnMap.map { it.key to it.value }.toMap()


    /**
     * Syncs secret manager by downloading secrets from cloud and then stores it locally.
     * This is used when configuration changes and secrets have to be re downloaded.
     * @param configuredSecrets List of secrets that are to be downloaded
     * @throws SecretManagerException when there are issues reading/writing to disk
     * @throws InterruptedException if thread is interrupted while running
     */
    suspend fun syncSecretsFromService(configuredSecrets: List<CloudSecretConfiguration>) {
        val log = logger.getCtxLoggers(className, "syncSecretsFromService")
        log.info("Start sync of  secrets from Secrets manager Service")

        try {
            waitForInitialization()
        } catch (e: SecretCryptoException) {
            throw SecretManagerException(e)
        }

        val downloadedSecrets: MutableList<EncryptedSecret> = ArrayList()

        _configuredSecrets = configuredSecrets

        for (secretConfig in configuredSecrets) {

            val labelsToDownload: MutableSet<String> = HashSet()
            if (!secretConfig.labels.isNullOrEmpty()) {
                labelsToDownload.addAll(secretConfig.labels!!)
            }

            labelsToDownload.add(LATEST_LABEL)

            for (label in labelsToDownload) {
                val requestBuilder = GetSecretValueRequest.builder().secretId(secretConfig.id).versionStage(label)
                if (secretConfig.id.isNotEmpty()) {
                    requestBuilder.secretId(secretConfig.id)
                }
                try {
                    val encryptedResult: EncryptedSecret = fetchAndEncryptAWSResponse(requestBuilder.build(), secretConfig.alias)
                    downloadedSecrets.add(encryptedResult)
                } catch (e: Throwable) {
                    if (e is IOException || e is SdkClientException || e is AWSIotException) {
                        val secretFromFile = secretsFile.get(secretConfig.id, label)
                        if (secretFromFile != null) {
                            log.warning("Could not sync secret from cloud, using a local version which may work")
                            try {
                                val decrypted = decrypt(secretFromFile)
                                if (decrypted != null) {
                                    logger.addSecretsValues(setOf(secretValue(decrypted)))
                                }
                                downloadedSecrets.add(secretFromFile)
                                log.trace("Secret configuration is not changed. Loaded from local store")
                            } catch (ex: SecretCryptoException) {
                                e.addSuppressed(ex)
                                throw SecretManagerException("Could not download secret ${secretConfig.id} with label $label from cloud, $e")
                            }
                        } else {
                            throw SecretManagerException("Could not download secret ${secretConfig.id} with label $label")
                        }

                    } else {
                        throw e
                    }
                }
            }
        }
        val secretsDocument = EncryptedSecretsDocument()
        secretsDocument.secrets = downloadedSecrets
        secretsFile.saveAll(secretsDocument)
        log.info("${downloadedSecrets.size} secrets stored locally in file ${secretsFile.fileName}")
        loadSecretsFromLocalStore()
    }

    private fun loadSecretsFromLocalStore() {
        val log = logger.getCtxLoggers(className, "loadSecretsFromLocalStore")
        val secrets: List<EncryptedSecret> = secretsFile.getAll().secrets ?: emptyList()
        nameToArnMap.clear()
        cache.clear()
        if (secrets.isNotEmpty()) {
            for (secretResult in secrets) {
                nameToArnMap[secretResult.name] = secretResult.arn
                if (!secretResult.alias.isNullOrEmpty()) {
                    nameToArnMap[secretResult.alias] = secretResult.arn
                }
                storeInCache(secretResult)
            }
        }
        log.info("${secrets.size} secrets loaded from local cache ${secretsFile.fileName}")
    }

    private fun getArnForSecretId(secretId: String): String {

        var arn: String? = secretId

        // normalize name to arn
        if (!ARN_PATTERN.matches(secretId)) {
            arn = nameToArnMap[secretId]
            if (arn == null) {
                throw SecretManagerSecretNotFoundException(secretId, "No secret found with name or alias \"$secretId\"")
            }
        }

        if (!cache.containsKey(arn)) {
            throw SecretManagerSecretNotFoundException(secretId, "No secret with ARN \"$secretId\"")
        }
        return arn.toString()
    }


    private suspend fun loadCrypter(securityService: SecurityService): SecretCryptoException? =
        try {

            val retryConfig = RetryUtils.RetryConfig(
                maxAttempt = Int.MAX_VALUE,
                retryableExceptions = listOf(CryptoServiceUnavailableException::class.java))

            val kp: KeyPair = RetryUtils.runWithRetry(
                retryConfig = retryConfig,
                logger = logger,
                taskDescription = "loadCrypter:getKeyPair")
            {
                securityService.getKeyPair()
            }

            val masterKey: MasterKey = RSAMasterKey.createInstance(kp.public, kp.private)
            val keyChain = KeyChain()
            keyChain.addMasterKey(masterKey)
            this.crypter = Crypter(keyChain)
            null
        } catch (e: SecretCryptoException) {
            e
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            SecretCryptoException(e)
        } catch (e: Exception) {
            SecretCryptoException(e)
        }


    private fun storeInCache(awsSecretResponse: EncryptedSecret) {
        val errLog = logger.getCtxErrorLog(className, "loadSecretsFromLocalStore")
        val decryptedResponse: GetSecretValueResponse? = try {
            decrypt(awsSecretResponse)
        } catch (e: SecretCryptoException) {
            errLog(" Unable to decrypt secret, skip loading in cache \"${awsSecretResponse.arn}\",  ${e.cause}")
            throw SecretManagerException("Cannot load secret from disk", e)
        }
        val secretArn = decryptedResponse!!.arn()
        cache[secretArn] = decryptedResponse
        cache[secretArn + decryptedResponse.versionId()] = decryptedResponse
        for (label in decryptedResponse.versionStages()) {
            cache[secretArn + label] = decryptedResponse
        }
    }


    private fun decrypt(awsSecretResponse: EncryptedSecret): GetSecretValueResponse? {
        var decryptedSecretString: String? = null
        if (awsSecretResponse.encryptedSecretString != null) {
            val decryptedSecret = crypter!!.decrypt(Base64.getDecoder().decode(awsSecretResponse.encryptedSecretString), awsSecretResponse.arn)
            decryptedSecretString = String(decryptedSecret, StandardCharsets.UTF_8)
        }
        var decryptedSecretBinary: SdkBytes? = null
        if (awsSecretResponse.encryptedSecretBinary != null) {
            val decryptedSecret = crypter!!.decrypt(Base64.getDecoder().decode(awsSecretResponse.encryptedSecretBinary), awsSecretResponse.arn)
            decryptedSecretBinary = SdkBytes.fromByteArray(decryptedSecret)
        }
        return GetSecretValueResponse.builder()
            .secretString(decryptedSecretString)
            .secretBinary(decryptedSecretBinary)
            .name(awsSecretResponse.name)
            .arn(awsSecretResponse.arn)
            .createdDate(Instant.ofEpochMilli(awsSecretResponse.createdDate))
            .versionId(awsSecretResponse.versionId)
            .versionStages(awsSecretResponse.versionStages)
            .build()
    }

    private fun fetchAndEncryptAWSResponse(request: GetSecretValueRequest, alias: String?): EncryptedSecret {
        val result: GetSecretValueResponse = secretClient.getSecret(request)
        var encodedSecretString: String? = null
        if (result.secretString() != null) {
            val encryptedSecretString = crypter!!.encrypt(result.secretString().toByteArray(StandardCharsets.UTF_8), result.arn())
            encodedSecretString = Base64.getEncoder().encodeToString(encryptedSecretString)
        }
        var encodedSecretBinary: String? = null
        if (result.secretBinary() != null) {
            val encryptedSecretBinary = crypter!!.encrypt(result.secretBinary().asByteArray(), result.arn())
            encodedSecretBinary = Base64.getEncoder().encodeToString(encryptedSecretBinary)
        }

        logger.addSecretsValues(setOf(secretValue(result)))

        return EncryptedSecret(
            encryptedSecretString = encodedSecretString,
            encryptedSecretBinary = encodedSecretBinary,
            name = result.name(),
            alias = alias,
            arn = result.arn(),
            createdDate = result.createdDate().toEpochMilli(),
            versionId = result.versionId(),
            versionStages = result.versionStages())
    }

    private fun secretValue(result: GetSecretValueResponse) = result.secretString() ?: result.secretBinary().asByteArray().toString()

    private suspend fun waitForInitialization() {
        try {
            withTimeout(10.toDuration(DurationUnit.SECONDS)) {
                val i = initialized.await()
                if (i != null) {
                    throw i
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw SecretCryptoException("Timeout waiting for SecurityManager to initialize")
        }
    }

    companion object {

        private const val LATEST_LABEL = "AWSCURRENT"

        private val className = this::class.java.simpleName

        fun createSecretsManager(configuration: HasSecretsManager, logger: Logger): SecretsManager? {
            val secretsManagerConfiguration = configuration.secretsManagerConfiguration
            if (secretsManagerConfiguration != null && (!secretsManagerConfiguration.cloudSecrets.isNullOrEmpty())) {
                val secretsManagerService = AwsSecretsManagerService(configuration, logger)
                return SecretsManager(secretsManagerService, secretsManagerConfiguration, logger)
            }
            return null
        }

        private fun createPrivateKeyContainer(configuration: SecretsManagerConfiguration, logger: Logger): KeyContainer =

            if (configuration.privateKeyFilePathOrNull != null)
                KeyFileContainer(getPrivateKeyFilename(configuration, logger))
            else
                KeyBytesContainer(configuration.privateKeyBytes ?: ByteArray(0))

        fun createSecurityService(configuration: SecretsManagerConfiguration, logger: Logger): SecurityService =
            SecurityService(privateKey = createPrivateKeyContainer(configuration, logger), logger)

        private fun getPrivateKeyFilename(configuration: SecretsManagerConfiguration, logger: Logger): String {
            val log = logger.getCtxLoggers(className, "getPrivateKeyFilename")
            val filename = configuration.privateKeyFilePath
            log.trace("Used private key filename is \"$filename\"")
            if (!File(filename).exists()) {
                val usingGreengrassSecret = filename == configuration.greengrassConfig?.privateKeyPath
                if (configuration.createPrivateKeyIfNotExist || usingGreengrassSecret) {
                    try {
                        log.info("Creating new private key file \"$filename\"")
                        KeyHelpers.createPKCS8KeyFile(filename)
                        log.info("Private key file \"$filename\" created")
                    } catch (e: Exception) {
                        val msg = "Could not create key file \"$filename\", $e"
                        log.error(msg)
                        throw SecretManagerException(msg)
                    }
                } else {
                    val msg =
                        "Configured file $filename does exist, set $CONFIG_CREATE_PRIVATE_KEY_IF_NO_EXIST to true in configuration, create is manually " +
                        "using  \"openssl genrsa -out $filename 2048\" or set $CONFIG_GREENGRASS_DEPLOYMENT_PATH to use a Greengrass deployed private key"
                    log.error(msg)
                    throw SecretManagerException(msg)
                }
            }
            return filename
        }

        private fun getSecretsFilename(configuration: SecretsManagerConfiguration, logger: Logger): String {
            val trace = logger.getCtxInfoLog(className, "getSecretsFilename")
            val filename = configuration.secretsFilename
            trace("Secrets are stored in file \"$filename\"")
            return filename
        }


    }
}

