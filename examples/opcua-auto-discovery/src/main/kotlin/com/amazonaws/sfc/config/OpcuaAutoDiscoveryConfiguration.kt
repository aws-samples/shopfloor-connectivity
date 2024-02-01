// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.config

import com.google.gson.annotations.SerializedName


@ConfigurationClass
// Config class just to load the config provider, loading a full adapter config might
// cause validating of the config to fail as the provider (due to source with no channel)
// before source channels are added by this provider.
class OpcuaAutoDiscoveryConfiguration : Validate {

    @SerializedName(CONFIG_AUTO_DISCOVERY)
    private val _autoDiscoveryConfig: AutoDiscoveryProviderConfiguration? = null

    val autoDiscoveryProviderConfig
        get() = _autoDiscoveryConfig

    private var _validated = false

    override fun validate() {
        if (validated) return

        ConfigurationException.check(
            _autoDiscoveryConfig != null,
            "$CONFIG_AUTO_DISCOVERY does not contain any source entries",
            CONFIG_AUTO_DISCOVERY,
            this
        )
        _autoDiscoveryConfig?.validate()

        validated = true
    }

    override var validated: Boolean
        get() = _validated
        set(value) {
            _validated = value
        }

    companion object {
        const val CONFIG_AUTO_DISCOVERY = "AutoDiscovery"
    }

}