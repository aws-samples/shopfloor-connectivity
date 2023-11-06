
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awssitewise.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class AwsSiteWiseAssetConfiguration : Validate {

    @SerializedName(CONFIG_ASSET_ID)
    private var _assetID: String? = null

    /**
     * ID of the asset
     */
    val assetID: String
        get() = _assetID ?: ""


    @SerializedName(CONFIG_PROPERTIES)
    private var _properties: List<SiteWiseAssetPropertyConfiguration> = emptyList()

    val properties: List<SiteWiseAssetPropertyConfiguration>
        get() {
            return _properties
        }

    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    /**
     * Validates configuration
     * @throws ConfigurationException
     */
    override fun validate() {
        if (validated) return

        ConfigurationException.check(
            (!_assetID.isNullOrEmpty()),
            "$CONFIG_ASSET_ID must be specified",
            CONFIG_ASSET_ID,
            this
        )
        _properties.forEach {
            it.validate()
        }
        validated = true
    }

    companion object {
        private const val CONFIG_ASSET_ID = "AssetId"
        private const val CONFIG_PROPERTIES = "Properties"

        private val default = AwsSiteWiseAssetConfiguration()

        fun create(assetId: String? = default._assetID,
                   properties: List<SiteWiseAssetPropertyConfiguration> = default._properties): AwsSiteWiseAssetConfiguration {

            val instance = AwsSiteWiseAssetConfiguration()
            with(instance) {
                _assetID = assetId
                _properties = properties
            }
            return instance
        }

    }

}