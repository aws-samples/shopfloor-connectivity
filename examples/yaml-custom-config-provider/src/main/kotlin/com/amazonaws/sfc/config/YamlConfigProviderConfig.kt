/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.SPDX-License-Identifier: MIT-0
 */

package com.amazonaws.sfc.config

import com.google.gson.annotations.SerializedName
import java.io.File

@ConfigurationClass
class YamlConfigProviderConfig : Validate {
    @SerializedName(CONFIG_YAML_FILE)
    private var _yamlConfigFile: String? = null

    val yamlConfigFile: File?
        get() = if (!_yamlConfigFile.isNullOrEmpty()) _yamlConfigFile?.let { File(it) } else null


    private var _validated = false

    override fun validate() {
        if (validated) return

        ConfigurationException.check(
            !_yamlConfigFile.isNullOrEmpty(),
            "$CONFIG_YAML_FILE is not set to name of YAML config file",
            CONFIG_YAML_FILE, this
        )

        ConfigurationException.check(yamlConfigFile!!.exists(),
            "$CONFIG_YAML_FILE ${yamlConfigFile!!.absolutePath} does not exist",
            CONFIG_YAML_FILE,
            this
        )

        ConfigurationException.check(yamlConfigFile!!.canRead(),
            "$CONFIG_YAML_FILE can not read ${yamlConfigFile!!.absolutePath} ",
            CONFIG_YAML_FILE,
            this
        )
    }

    override var validated: Boolean
        get() = _validated
        set(value) {
            _validated = value
        }

    companion object {
        const val CONFIG_YAML_FILE = "YamlConfigFile"
    }


}