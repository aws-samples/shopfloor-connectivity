
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.config

import com.amazonaws.sfc.metrics.MetricsConfiguration.Companion.CONFIG_METRICS
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.google.gson.annotations.SerializedName

/**
 * Configuration for an input source which could be an in-process instance or a reader that uses
 * IPC from a server that implements the protocol
 */
@ConfigurationClass
open class ProtocolAdapterConfiguration : Validate {

    @SerializedName(BaseConfiguration.CONFIG_DESCRIPTION)
    @Suppress("PropertyName")
    protected var _description = ""

    /**
     * Description of the source
     */
    val description: String
        get() = _description

    @SerializedName(CONFIG_PROTOCOL_ADAPTER_TYPE)
    @Suppress("PropertyName")
    protected var _protocolAdapterType: String? = null

    /**
     * Type of the protocol
     */
    val protocolAdapterType: String?
        get() = _protocolAdapterType


    @SerializedName(CONFIG_PROTOCOL_ADAPTER_SERVER)
    @Suppress("PropertyName")
    protected var _protocolAdapterServer: String? = null

    /**
     *  Server for protocol server
     */
    val protocolAdapterServer: String?
        get() = _protocolAdapterServer

    @SerializedName(CONFIG_METRICS)
    protected var _metrics: MetricsSourceConfiguration? = null

    val metrics: MetricsSourceConfiguration?
        get() = _metrics

    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    // Validates configuration
    override fun validate() {

        ConfigurationException.check(
            protocolAdapterType != null,
            "$CONFIG_PROTOCOL_ADAPTER_TYPE must be specified",
            CONFIG_PROTOCOL_ADAPTER_TYPE,
            this
        )

    }

    companion object {
        const val CONFIG_PROTOCOL_ADAPTER_TYPE = "AdapterType"
        const val CONFIG_PROTOCOL_ADAPTER_SERVER = "AdapterServer"

        protected val default = ProtocolAdapterConfiguration()

        fun create(description: String = default._description,
                   adapterType: String? = default._protocolAdapterType,
                   metrics: MetricsSourceConfiguration? = default._metrics,
                   adapterServer: String? = default._protocolAdapterServer): ProtocolAdapterConfiguration =
            createAdapterConfiguration(
                description = description,
                adapterType = adapterType,
                metrics = metrics,
                adapterServer = adapterServer)


        @JvmStatic
        protected inline fun <reified T : ProtocolAdapterConfiguration> createAdapterConfiguration(description: String,
                                                                                                   adapterType: String?,
                                                                                                   metrics: MetricsSourceConfiguration?,
                                                                                                   adapterServer: String?): T {

            val parameterLessConstructor = T::class.java.constructors.firstOrNull { it.parameters.isEmpty() }
            assert(parameterLessConstructor != null)
            val instance = parameterLessConstructor!!.newInstance() as T

            with(instance) {
                _description = description
                _protocolAdapterType = adapterType
                _metrics = metrics
                _protocolAdapterServer = adapterServer
            }
            return instance
        }

    }

}