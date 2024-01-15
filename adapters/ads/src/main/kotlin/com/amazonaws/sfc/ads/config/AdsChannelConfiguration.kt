// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.ads.config

import com.amazonaws.sfc.config.ChannelConfiguration
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.google.gson.annotations.SerializedName


@ConfigurationClass
class AdsChannelConfiguration : ChannelConfiguration() {


    @SerializedName(CONFIG_SYMBOL_NAME)
    private var _symbolName: String = ""
    val symbolName: String
        get() = _symbolName

    override fun validate() {
        ConfigurationException.check(
            _symbolName.isNotEmpty(),
            "$CONFIG_SYMBOL_NAME of ADS channel can not be empty",
            CONFIG_SYMBOL_NAME, this
        )
        validated = true
    }

    companion object {

        private const val CONFIG_SYMBOL_NAME = "SymbolName"

        private val default = AdsChannelConfiguration()

        fun create(
            symbolName: String = default._symbolName,
            name: String? = default._name,
            description: String = default._description,
            transformation: String? = default._transformationID,
            metadata: Map<String, String> = default._metadata,
            changeFilter: String? = default._changeFilterID,
            valueFilter: String? = default._valueFilterID
        ): AdsChannelConfiguration {

            val instance = createChannelConfiguration<AdsChannelConfiguration>(
                name = name,
                description = description,
                transformation = transformation,
                metadata = metadata,
                changeFilter = changeFilter,
                valueFilter = valueFilter
            )

            with(instance) {
                _symbolName = symbolName
            }
            return instance
        }

    }
}








