
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.config

import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_ACTIVE
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_CREDENTIAL_PROVIDER_CLIENT
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_DESCRIPTION
import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_TARGETS
import com.amazonaws.sfc.metrics.MetricsConfiguration
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.google.gson.annotations.SerializedName
import java.io.File

/**
 * Base class with minimum set of attributes for a target configuration
 */
@ConfigurationClass
open class TargetConfiguration : Validate {


    @SerializedName(CONFIG_DESCRIPTION)
    @Suppress("PropertyName")
    protected var _description = ""

    /**
     * Description of the target
     */
    val description: String
        get() = _description

    @SerializedName(CONFIG_ACTIVE)
    @Suppress("PropertyName")
    protected var _active = true

    /***
     * Status of the target, if true output is written to the target
     */
    val active: Boolean
        get() = _active

    @SerializedName(CONFIG_TARGET_TYPE)
    @Suppress("PropertyName")
    protected var _targetType: String? = null

    /**
     * Type of the target
     */
    val targetType: String?
        get() = _targetType

    @SerializedName(CONFIG_TARGET_TEMPLATE)
    @Suppress("PropertyName")
    protected var _template: String? = null
    val template: File?
        get() = if (_template != null) _template?.let { File(it) } else null

    @SerializedName(CONFIG_TARGET_SERVER)
    @Suppress("PropertyName")
    protected var _server: String? = null

    /**
     * Target server for IPC target server
     */
    val server: String?
        get() = _server

    @SerializedName(CONFIG_CREDENTIAL_PROVIDER_CLIENT)
    @Suppress("PropertyName")
    protected var _credentialProvideClient: String? = null

    val credentialProviderClient: String?
        get() = _credentialProvideClient


    @SerializedName(CONFIG_TARGETS)
    @Suppress("PropertyName")
    protected var _subTargets: List<String>? = null

    open val subTargets
        get() = _subTargets

    @SerializedName(MetricsConfiguration.CONFIG_METRICS)
    protected var _metrics: MetricsSourceConfiguration = MetricsSourceConfiguration()

    val metrics: MetricsSourceConfiguration
        get() = _metrics


    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }


    override fun validate() {

        if (validated) return

        ConfigurationException.check(
            targetType != null,
            "$CONFIG_TARGET_TYPE must be specified",
            CONFIG_TARGET_TYPE,
            this
        )

        ConfigurationException.check(
            (template == null || template!!.exists()),
            "Target transformation template $template does not exist",
            CONFIG_TARGET_TEMPLATE,
            this
        )

        validated = true
    }


    companion object {
        const val CONFIG_TARGET_TEMPLATE = "Template"
        const val CONFIG_TARGET_TYPE = "TargetType"

        const val CONFIG_TARGET_SERVER = "TargetServer"
        fun create(description: String = "",
                   active: Boolean = true,
                   targetType: String? = null,
                   template: String? = null,
                   targetServer: String? = null,
                   credentialProviderClient: String? = null,
                   targets: List<String>? = null): TargetConfiguration {

            val instance = TargetConfiguration()
            with(instance) {
                _description = description
                _active = active
                _targetType = targetType
                _template = template
                _server = targetServer
                _credentialProvideClient = credentialProviderClient
                _subTargets = targets
            }

            return instance
        }

        @JvmStatic
        protected inline fun <reified T : TargetConfiguration> createTargetConfiguration(description: String = "",
                                                                                         active: Boolean = true,
                                                                                         targetType: String? = null,
                                                                                         template: String? = null,
                                                                                         targetServer: String? = null,
                                                                                         credentialProviderClient: String? = null,
                                                                                         metrics: MetricsSourceConfiguration = MetricsSourceConfiguration(),
                                                                                         targets: List<String>? = null): TargetConfiguration {

            val parameterLessConstructor = T::class.java.constructors.firstOrNull { it.parameters.isEmpty() }
            assert(parameterLessConstructor != null)
            val instance = parameterLessConstructor!!.newInstance() as T

            @Suppress("DuplicatedCode")
            with(instance) {
                _description = description
                _active = active
                _targetType = targetType
                _template = template
                _server = targetServer
                _credentialProvideClient = credentialProviderClient
                _metrics = metrics
                _subTargets = targets
            }

            return instance
        }
    }

}




