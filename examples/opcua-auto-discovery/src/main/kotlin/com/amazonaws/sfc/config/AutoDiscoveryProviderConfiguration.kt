// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.config

import com.google.gson.annotations.SerializedName
import java.io.File
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class AutoDiscoveryProviderConfiguration : Validate {

    @SerializedName(BaseConfiguration.CONFIG_SOURCES)
    private var _sources: Map<String, NodeDiscoveryConfigurations> = emptyMap()
    val sources
        get() = _sources

    @SerializedName(CONFIG_WAIT_FOR_RETRY)
    private var _waitForRetry: Long = CONFIG_DEFAULT_WAIT_BEFORE_RETRY
    val waitForRetry: Duration
        get() = _waitForRetry.toDuration(DurationUnit.MILLISECONDS)

    @SerializedName(CONFIG_MAX_RETRIES)
    private var _maxRetries: Int = CONFIG_DEFAULT_MAX_RETRIES
    val maxRetries: Int
        get() = _maxRetries

    @SerializedName(CONFIG_INCLUDE_DESCRIPTION)
    private val _includeDescription: Boolean = true
    val includeDescription
        get() = _includeDescription

    @SerializedName(CONFIG_SAVED_LAST_CONFIG)
    private var _savedLastConfig: String? = null
    val savedLastConfig : File?
        get() = if (_savedLastConfig != null) File(_savedLastConfig!!) else null


    private var _validated = false

    override fun validate() {
        if (validated) return


        ConfigurationException.check(
            _sources.isNotEmpty(),
            "${OpcuaAutoDiscoveryConfiguration.CONFIG_AUTO_DISCOVERY} does not contain any source entries",
            OpcuaAutoDiscoveryConfiguration.CONFIG_AUTO_DISCOVERY,
            this
        )

        _sources.values.forEach { nodes: NodeDiscoveryConfigurations ->
            nodes.forEach { node -> node.validate() }
        }

        validated = true
    }

    override var validated: Boolean
        get() = _validated
        set(value) {
            _validated = value
        }

    companion object{
        const val CONFIG_DISCOVERY_DEPTH = "DiscoveryDepth"
        const val CONFIG_EXCLUSIONS = "Exclusions"
        const val CONFIG_INCLUDE_DESCRIPTION = "IncludeDescription"
        const val CONFIG_INCLUSIONS = "Inclusions"
        const val CONFIG_MAX_RETRIES = "MaxRetries"
        const val CONFIG_NODES_TO_DISCOVER = "DiscoveredNodeTypes"
        const val CONFIG_SAVED_LAST_CONFIG = "SavedLastConfig"
        const val CONFIG_WAIT_FOR_RETRY = "WaitForRetry"

        const val CONFIG_DEFAULT_MAX_RETRIES = 10
        const val CONFIG_DEFAULT_WAIT_BEFORE_RETRY = 60000L
    }


}