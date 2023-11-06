
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


@file:Suppress("unused")

package com.amazonaws.sfc.s7.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.ProtocolAdapterConfiguration
import com.amazonaws.sfc.metrics.MetricsSourceConfiguration
import com.amazonaws.sfc.s7.config.S7Configuration.Companion.S7_ADAPTER
import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Suppress("unused")
@ConfigurationClass
class S7AdapterConfiguration : ProtocolAdapterConfiguration() {

    @SerializedName(CONFIG_S7_CONTROLLERS)
    private var _controllers = mapOf<String, S7ControllerConfiguration>()

    val controllers: Map<String, S7ControllerConfiguration>
        get() = _controllers

    @SerializedName(CONFIG_WAIT_AFTER_ERROR)
    private var _waitAfterErrors: Int = CONFIG_DEFAULT_WAIT_AFTER_ERROR

    val waitAfterErrors: Duration
        get() = _waitAfterErrors.toDuration(DurationUnit.MILLISECONDS)

    override fun validate() {
        if (validated) return
        super.validate()

        ConfigurationException.check(
            controllers.isNotEmpty(),
            "No $CONFIG_S7_CONTROLLERS configured for adapter",
            CONFIG_S7_CONTROLLERS,
            this)

        controllers.values.forEach { controller -> controller.validate() }

        validated = true

    }

    companion object {

        private const val CONFIG_S7_CONTROLLERS = "Controllers"

        private val default = S7AdapterConfiguration()


        fun create(controllers: Map<String, S7ControllerConfiguration> = default._controllers,
                   description: String = default._description,
                   waitAfterErrors: Int = default._waitAfterErrors,
                   metrics: MetricsSourceConfiguration? = default._metrics,
                   adapterServer: String? = default._protocolAdapterServer): S7AdapterConfiguration {

            val instance = createAdapterConfiguration<S7AdapterConfiguration>(
                description = description,
                adapterType = S7_ADAPTER,
                metrics = metrics,
                adapterServer = adapterServer
            )

            with(instance) {
                _controllers = controllers
                _waitAfterErrors = waitAfterErrors
            }
            return instance
        }

        const val CONFIG_WAIT_AFTER_ERROR = "WaitAfterError"
        const val CONFIG_DEFAULT_WAIT_AFTER_ERROR = 10000


    }

}