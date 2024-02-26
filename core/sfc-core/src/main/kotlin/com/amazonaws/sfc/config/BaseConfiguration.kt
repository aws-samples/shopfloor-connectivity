// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.config

import com.amazonaws.sfc.awsiot.AwsIotCredentialProviderClientConfiguration
import com.amazonaws.sfc.log.LogLevel
import com.amazonaws.sfc.secrets.CloudSecretConfiguration
import com.google.gson.annotations.SerializedName


interface HasCredentialClients {
    val awsCredentialServiceClients: Map<String, AwsIotCredentialProviderClientConfiguration>
}

interface HasSecretsManager : HasCredentialClients {

    val secretsManagerConfiguration: SecretsManagerConfiguration?

    val hasConfiguredSecrets: Boolean
        get() = ((secretsManagerConfiguration != null) && (!secretsManagerConfiguration!!.cloudSecrets.isNullOrEmpty()))

    val configuredCloudSecrets: List<CloudSecretConfiguration>
        get() = secretsManagerConfiguration?.cloudSecrets ?: emptyList()

    val secretPlaceholderNames: Set<String>?
        get() = secretsManagerConfiguration?.placeholderNames
}

/**
 * Base configuration class
 */
@ConfigurationClass
open class BaseConfiguration : Validate, HasSecretsManager {

    @SerializedName(CONFIG_NAME)
    @Suppress("PropertyName")
    protected var _name = ""

    /**
     * Name of the configuration
     */
    val name: String
        get() = _name

    @SerializedName(CONFIG_VERSION)
    @Suppress("PropertyName")
    protected var _version = ""

    /**
     * Version of the configuration
     */
    val version: String
        get() = _version

    @SerializedName(CONFIG_AWS_VERSION)
    @Suppress("PropertyName")
    protected var _awsVersion: String? = null

    /**
     * Configuration format version
     */
    val awsVersion: String?
        get() = _awsVersion

    @SerializedName(CONFIG_DESCRIPTION)
    @Suppress("PropertyName")
    protected var _description = ""

    /**
     * Description of the configuration
     */
    val description: String
        get() = _description

    @SerializedName(CONFIG_SCHEDULES)
    @Suppress("PropertyName")
    protected var _schedules: List<ScheduleConfiguration> = arrayListOf()

    /**
     * All configured schedules
     * @see ScheduleConfiguration
     */
    val schedules: List<ScheduleConfiguration>
        get() = _schedules

    @SerializedName(CONFIG_LOG_LEVEL)
    @Suppress("PropertyName")
    protected var _logLevel: LogLevel? = null

    /**
     * Log output level
     * @see LogLevel
     */
    val logLevel: LogLevel
        get() = _logLevel ?: LogLevel.INFO

    @SerializedName(CONFIG_META_DATA)
    @Suppress("PropertyName")
    protected var _metadata = emptyMap<String, String>()

    /**
     * Metadata, which are constant values, that will be added as constant values
     */
    val metadata: Map<String, String>
        get() = _metadata

    @SerializedName(CONFIG_ELEMENT_NAMES)
    @Suppress("PropertyName")
    protected var _elementNames = ElementNamesConfiguration.DEFAULT_TAG_NAMES

    /**
     * Names for value, timestamp ans metadata elements
     * @see ElementNamesConfiguration
     */
    val elementNames: ElementNamesConfiguration
        get() = _elementNames


    @SerializedName(CONFIG_TARGET_SERVERS)
    @Suppress("PropertyName")
    protected var _targetServers: Map<String, ServerConfiguration> = emptyMap()

    /**
     * All configured target IPC servers
     */
    val targetServers: Map<String, ServerConfiguration>
        get() = _targetServers


    @SerializedName(CONFIG_TARGET_TYPES)
    @Suppress("PropertyName")
    protected var _targetTypes: Map<String, InProcessConfiguration> = emptyMap()

    /**
     * All configured target types
     */
    val targetTypes: Map<String, InProcessConfiguration>
        get() = _targetTypes


    @SerializedName(CONFIG_PROTOCOL_SERVERS)
    @Suppress("PropertyName")
    protected var _protocolAdapterServers: Map<String, ServerConfiguration> = emptyMap()


    /**
     * All configured protocol source IPC servers
     */
    val protocolAdapterServers: Map<String, ServerConfiguration>
        get() = _protocolAdapterServers


    @SerializedName(CONFIG_PROTOCOL_ADAPTER_TYPES)
    @Suppress("PropertyName")
    protected var _protocolTypes: Map<String, InProcessConfiguration> = emptyMap()


    /**
     * All configured target types
     */
    val protocolAdapterTypes: Map<String, InProcessConfiguration>
        get() = _protocolTypes


    @SerializedName(CONFIG_AWS_IOT_CREDENTIAL_PROVIDER_CLIENTS)
    @Suppress("PropertyName")
    protected var _awsIoTCredentialProviderClients =
        mapOf<String, AwsIotCredentialProviderClientConfiguration>()
    override val awsCredentialServiceClients
        get() = _awsIoTCredentialProviderClients

    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    @SerializedName(CONFIG_SECRETS_MANGER)
    @Suppress("PropertyName")
    protected var _secretsManagerConfiguration: SecretsManagerConfiguration? = null
    override val secretsManagerConfiguration
        get() = _secretsManagerConfiguration

    @SerializedName(CONFIG_TUNING)
    protected var _tuningConfiguration = TuningConfiguration()

    val tuningConfiguration: TuningConfiguration
        get() = _tuningConfiguration


    /**
     * Validates the configuration
     */
    override fun validate() {
        if (validated) return

        validateVersion()

        schedules.forEach { it.validate() }
        targetTypes.values.forEach { it.validate() }
        targetServers.values.forEach { it.validate() }

        protocolAdapterServers.values.forEach { it.validate() }

        secretsManagerConfiguration?.validate()

        if (secretsManagerConfiguration != null) {
            ConfigurationException.check(
                (secretsManagerConfiguration?.credentialProviderClient == null
                        || _awsIoTCredentialProviderClients[secretsManagerConfiguration?.credentialProviderClient] != null),
                "$CONFIG_SECRETS_MANGER, $CONFIG_CREDENTIAL_PROVIDER_CLIENT \"secretsManagerConfiguration?.credentialProviderClient\" is not configured, " +
                        "configured clients are ${_awsIoTCredentialProviderClients.keys}",
                "$CONFIG_SECRETS_MANGER.$CONFIG_CREDENTIAL_PROVIDER_CLIENT",
                secretsManagerConfiguration
            )
        }

        validated = true
    }

    // check configuration for if the version is compatible with handled versions in this version of the SFC
    private fun validateVersion() =
        ConfigurationException.check(
            (awsVersion == null) || (AWS_CONFIG_VERSIONS.contains(awsVersion)),
            "AWSVersion must be any of ${AWS_CONFIG_VERSIONS.joinToString()}",
            "AWSVersion"
        )


    companion object {

        val AWS_CONFIG_VERSIONS = listOf("2022-04-02")
        const val WILD_CARD = "*"

        const val CONFIG_ACTIVE = "Active"
        const val CONFIG_AWS_VERSION = "AWSVersion"
        const val CONFIG_BATCH_SIZE = "BatchSize"
        const val CONFIG_CERTIFICATE_VALIDATION = "CertificateValidation"
        const val CONFIG_CHANGE_FILTER = "ChangeFilter"
        const val CONFIG_CHANGE_FILTERS = "ChangeFilters"
        const val CONFIG_CHANNELS = "Channels"
        const val CONFIG_CREDENTIAL_PROVIDER_CLIENT = "CredentialProviderClient"
        const val CONFIG_DESCRIPTION = "Description"
        const val CONFIG_ELEMENT_NAMES = "ElementNames"
        const val CONFIG_ENABLED = "Enabled"
        const val CONFIG_LOG_LEVEL = "LogLevel"
        const val CONFIG_META_DATA = "Metadata"
        const val CONFIG_NAME = "Name"
        const val CONFIG_PROTOCOL_ADAPTERS = "ProtocolAdapters"
        const val CONFIG_PROTOCOL_ADAPTER_TYPES = "AdapterTypes"
        const val CONFIG_PROTOCOL_SERVERS = "AdapterServers"
        const val CONFIG_TUNING = "Tuning"
        const val CONFIG_REGION = "Region"
        const val CONFIG_SCHEDULES = "Schedules"
        const val CONFIG_SECRETS_MANGER = "SecretsManager"
        const val CONFIG_SOURCES = "Sources"
        const val CONFIG_TARGETS = "Targets"
        const val CONFIG_TARGET_SERVERS = "TargetServers"
        const val CONFIG_TARGET_TYPES = "TargetTypes"
        const val CONFIG_TRANSFORMATIONS = "Transformations"
        const val CONFIG_VALUE_FILTER = "ValueFilter"
        const val CONFIG_VALUE_FILTERS = "ValueFilters"
        const val CONFIG_VERSION = "Version"
        const val CONFIG_CERTIFICATE = "CertificateFile"
        const val CONFIG_PRIVATE_KEY = "PrivateKeyFile"
        const val CONFIG_CERTIFICATES_AND_KEYS_BY_FILE_REFERENCE = "CertificatesAndKeysByFileReference"
        const val CONFIG_INTERVAL = "Interval"
        const val CONFIG_BYTES_SUFFIX = "Bytes"


        const val CONFIG_DISABLED_COMMENT = "#"

        const val CONFIG_AWS_IOT_CREDENTIAL_PROVIDER_CLIENTS = "AwsIotCredentialProviderClients"

        private val default = BaseConfiguration()

        fun create(
            name: String = default._name,
            version: String = default._version,
            awsVersion: String? = default._awsVersion,
            description: String = default._description,
            schedules: List<ScheduleConfiguration> = default._schedules,
            logLevel: LogLevel? = default._logLevel,
            metadata: Map<String, String> = default._metadata,
            elementNames: ElementNamesConfiguration = default._elementNames,
            targetServers: Map<String, ServerConfiguration> = default._targetServers,
            targetTypes: Map<String, InProcessConfiguration> = default._targetTypes,
            adapterServers: Map<String, ServerConfiguration> = default._protocolAdapterServers,
            adapterTypes: Map<String, InProcessConfiguration> = default._protocolTypes,
            awsIotCredentialProviderClients: Map<String, AwsIotCredentialProviderClientConfiguration> = default._awsIoTCredentialProviderClients,
            secretsManagerConfiguration: SecretsManagerConfiguration? = default._secretsManagerConfiguration,
            tuningConfiguration: TuningConfiguration = default._tuningConfiguration
        ): BaseConfiguration = createBaseConfiguration(

            name = name,
            version = version,
            awsVersion = awsVersion,
            description = description,
            schedules = schedules,
            logLevel = logLevel,
            metadata = metadata,
            elementNames = elementNames,
            targetServers = targetServers,
            targetTypes = targetTypes,
            adapterServers = adapterServers,
            adapterTypes = adapterTypes,
            awsIotCredentialProviderClients = awsIotCredentialProviderClients,
            secretsManagerConfiguration = secretsManagerConfiguration,
            tuningConfiguration = tuningConfiguration
        )


        @JvmStatic
        protected inline fun <reified T : BaseConfiguration> createBaseConfiguration(
            name: String,
            version: String,
            awsVersion: String?,
            description: String,
            schedules: List<ScheduleConfiguration>,
            logLevel: LogLevel?,
            metadata: Map<String, String>,
            elementNames: ElementNamesConfiguration,
            targetServers: Map<String, ServerConfiguration>,
            targetTypes: Map<String, InProcessConfiguration>,
            adapterServers: Map<String, ServerConfiguration>,
            adapterTypes: Map<String, InProcessConfiguration>,
            awsIotCredentialProviderClients: Map<String, AwsIotCredentialProviderClientConfiguration>,
            secretsManagerConfiguration: SecretsManagerConfiguration?,
            tuningConfiguration: TuningConfiguration = TuningConfiguration()): T {

            val parameterLessConstructor = T::class.java.constructors.firstOrNull { it.parameters.isEmpty() }
            assert(parameterLessConstructor != null)
            val instance = parameterLessConstructor!!.newInstance() as T

            @Suppress("DuplicatedCode")
            with(instance) {
                _name = name
                _version = version
                _awsVersion = awsVersion
                _description = description
                _schedules = schedules
                _logLevel = logLevel
                _metadata = metadata
                _elementNames = elementNames
                _targetServers = targetServers
                _targetTypes = targetTypes
                _protocolAdapterServers = adapterServers
                _protocolTypes = adapterTypes
                _awsIoTCredentialProviderClients = awsIotCredentialProviderClients
                _secretsManagerConfiguration = secretsManagerConfiguration
                _tuningConfiguration = tuningConfiguration
            }

            return instance
        }


    }
}

