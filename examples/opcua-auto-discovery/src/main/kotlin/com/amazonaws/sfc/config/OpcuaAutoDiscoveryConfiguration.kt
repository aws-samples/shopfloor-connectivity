package com.amazonaws.sfc.config

import com.google.gson.annotations.SerializedName
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


typealias OpcuaDiscoveryProviderConfig = Map<String, List<OpcuaDiscoveryNodeConfiguration>>

@ConfigurationClass
// Config class just to load the config provider, loading a full adapter config might
// cause validating of the config to fail as the provider (due to source with no channel)
// before source channels are added by this provider.
class OpcuaAutoDiscoveryConfiguration : Validate {

    @SerializedName(CONFIG_AUTO_DISCOVERY)

    private val _autoDiscoveryConfig: OpcuaDiscoveryProviderConfig = emptyMap()

    val autoDiscoveryProviderConfig
        get() = _autoDiscoveryConfig


    @SerializedName(CONFIG_WAIT_FOR_RETRY)
    private var _waitForRetry: Long = CONFIG_DEFAULT_WAIT_BEFORE_RETRY
    val waitForRetry: Duration

        get() = _waitForRetry.toDuration(DurationUnit.MILLISECONDS)

    private var _validated = false

    override fun validate() {
        if (validated) return


        ConfigurationException.check(
            _autoDiscoveryConfig.isNotEmpty(),
            "$CONFIG_AUTO_DISCOVERY does not contain any source entries",
            CONFIG_AUTO_DISCOVERY,
            this
        )

        _autoDiscoveryConfig.values.forEach { nodes: List<OpcuaDiscoveryNodeConfiguration> ->
            nodes.forEach { node -> node.validate() }
        }

        validated = true
    }

    override var validated: Boolean
        get() = _validated
        set(value) {
            _validated = value
        }

    companion object {
        const val CONFIG_AUTO_DISCOVERY = "AutoDiscovery"
        const val CONFIG_NODES_TO_DISCOVER = "NodeTypesToDiscover"
        const val CONFIG_DISCOVERY_DEPTH = "DiscoveryDepth"
        const val CONFIG_EXCLUSIONS = "Exclusions"
        const val CONFIG_INCLUSIONS = "Inclusions"
        const val CONFIG_WAIT_FOR_RETRY = "WaitForRetry"
        const val CONFIG_DEFAULT_WAIT_BEFORE_RETRY = 60000L

    }

}