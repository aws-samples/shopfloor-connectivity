// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.config

import com.amazonaws.sfc.config.BaseConfiguration.Companion.CONFIG_TUNING
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class ConfigWithTuningConfiguration : Validate {
    @SerializedName(CONFIG_TUNING)
    private var _tuningConfiguration: TuningConfiguration = TuningConfiguration()

    val tuningConfiguration: TuningConfiguration
        get() = _tuningConfiguration

    override fun validate() {
    }

    override var validated: Boolean
        get() = true
        set(value) {}

}