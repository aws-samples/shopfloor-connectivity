
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awsiot.mqtt.config

import com.amazonaws.sfc.awsiot.mqtt.config.AwsIotWriterConfiguration.Companion.AWS_IOT_MQTT_TARGET
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_TARGETS
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.TargetConfiguration
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.google.gson.annotations.SerializedName
import software.amazon.awssdk.crt.mqtt.QualityOfService
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

/**
 * MQTT topic target configuration
 */
@ConfigurationClass
class AwsIotTargetConfiguration : TargetConfiguration() {


    @SerializedName(CONFIG_TOPIC_NAME)
    /**
     * Name of the topic
     */
    private var _topicName: String? = null
    val topicName: String?
        get() = _topicName

    @SerializedName(CONFIG_END_POINT)
    private var _endpoint: String = ""

    /**
     * IoT broker endpoint
     */
    val endpoint: String
        get() = _endpoint

    @SerializedName(CONFIG_CERTIFICATE)
    private var _certificate: String = ""

    /**
     * Certificate file
     */
    val certificate: String
        get() = _certificate

    @SerializedName(CONFIG_KEY)
    private var _key: String = ""

    /**
     * Key file
     */
    val key: String
        get() = _key

    @SerializedName(CONFIG_ROOT_CA)
    private var _rootCA: String = ""

    /**
     * Root Certificate file
     */
    val rootCA: String
        get() = _rootCA

    @SerializedName(CONFIG_QOS)
    private var _qos: Int = QOS_DEFAULT

    /**
     * QoS
     */
    val qos: QualityOfService
        get() = when (_qos) {
            0 -> QualityOfService.AT_MOST_ONCE
            1 -> QualityOfService.AT_LEAST_ONCE
            2 -> QualityOfService.EXACTLY_ONCE
            else -> QualityOfService.AT_MOST_ONCE
        }

    @SerializedName(CONFIG_CONNECT_TIMEOUT)
    private var _connectTimeout = DEFAULT_CONNECT_TIMEOUT

    val connectTimeout: Long = _connectTimeout

    @SerializedName(CONFIG_PUBLISH_TIMEOUT)
    private var _publishTimeout = DEFAULT_PUBLISH_TIMEOUT

    val publishTimeout: Long = _publishTimeout

    /**
     * Validates the configuration
     * @throws ConfigurationException
     */
    @Throws(ConfigurationException::class)
    override fun validate() {
        if (validated) return

        super.validate()
        checkRequiredSettings()
        checkQos()
        validated = true

    }

    private fun checkQos() {
        ConfigurationException.check(
            (_qos in 0..2),
            "QoS must be 0, 1 or 2",
            CONFIG_QOS, this
        )
    }

    // Checks if al required attributes are set
    private fun checkRequiredSettings() {

        val requiredProperties = this::class.memberProperties
            .filter { it.visibility == KVisibility.PUBLIC }
            .filter { it.name in requiredSettings }

        requiredProperties.forEach { p ->

            val v = p.call(this) as? String

            ConfigurationException.check(
                !(v.isNullOrEmpty()),
                "${p.name} for MQTT target must be set",
                CONFIG_TARGETS,
                this
            )
        }
    }

    companion object {

        private const val CONFIG_TOPIC_NAME = "TopicName"
        private const val CONFIG_END_POINT = "Endpoint"
        private const val CONFIG_CERTIFICATE = "Certificate"
        private const val CONFIG_KEY = "Key"
        private const val CONFIG_ROOT_CA = "RootCA"
        private const val CONFIG_QOS = "Qos"
        private const val QOS_DEFAULT = 0
        private const val CONFIG_CONNECT_TIMEOUT = "ConnectTimeout"
        private const val DEFAULT_CONNECT_TIMEOUT = 10000L
        private const val CONFIG_PUBLISH_TIMEOUT = "PublishTimeout"
        private const val DEFAULT_PUBLISH_TIMEOUT = 10000L

        val requiredSettings = listOf(
            CONFIG_TOPIC_NAME,
            CONFIG_END_POINT,
            CONFIG_CERTIFICATE,
            CONFIG_ROOT_CA,
            CONFIG_KEY)

        private val default = AwsIotTargetConfiguration()

        fun create(topicName: String? = default._topicName,
                   endpoint: String = default._endpoint,
                   certificate: String = default._certificate,
                   key: String = default._key,
                   rootCA: String = default._rootCA,
                   qos: Int = default._qos,
                   connectTimeout: Long = default._connectTimeout,
                   publishTimeout: Long = default._publishTimeout,
                   description: String = default._description,
                   active: Boolean = default._active,
                   template: String? = default._template,
                   targetServer: String? = default.server,
                   metrics: MetricsSourceConfiguration = default._metrics,
                   credentialProviderClient: String? = default._credentialProvideClient): AwsIotTargetConfiguration {

            val instance = createTargetConfiguration<AwsIotTargetConfiguration>(description = description,
                active = active,
                targetType = AWS_IOT_MQTT_TARGET,
                template = template,
                targetServer = targetServer,
                metrics = metrics,
                credentialProviderClient = credentialProviderClient) as AwsIotTargetConfiguration

            with(instance) {
                _topicName = topicName
                _endpoint = endpoint
                _certificate = certificate
                _key = key
                _rootCA = rootCA
                _qos = qos
                _connectTimeout = connectTimeout
                _publishTimeout = publishTimeout
                _description = description
                _active = active
            }
            return instance
        }
    }
}
