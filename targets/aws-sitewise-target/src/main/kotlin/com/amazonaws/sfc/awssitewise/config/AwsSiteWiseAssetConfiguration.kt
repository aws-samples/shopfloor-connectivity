// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awssitewise.config

import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseTargetConfiguration.Companion.ID_REGEX
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseTargetConfiguration.Companion.ID_REGEX_STR
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class AwsSiteWiseAssetConfiguration : Validate {

    @SerializedName(CONFIG_ASSET_ID)
    private var _assetID: String? = null
    val assetID: String?
        get() = _assetID

    @SerializedName(CONFIG_ASSET_NAME)
    private var _assetName: String? = null
    val assetName: String?
        get() = _assetName


    @SerializedName(CONFIG_ASSET_EXTERNAL_ID)
    private var _assetExternalID: String? = null
    val assetExternalID: String?
        get() = _assetExternalID

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

    val asString
        get() = if (!assetID.isNullOrEmpty()) assetID else if (!assetExternalID.isNullOrEmpty()) assetExternalID else assetName

    /**
     * Validates configuration
     * @throws ConfigurationException
     */
    override fun validate() {
        if (validated) return

        val allPropertiesByAlias = properties.all { it.propertyAlias != null }
        val numberOfSpecifiedAssetIdentifiers = listOf(_assetID, _assetName, _assetExternalID).count { !it.isNullOrEmpty() }

        ConfigurationException.check(
            (numberOfSpecifiedAssetIdentifiers != 0 || allPropertiesByAlias),
            "An $CONFIG_ASSET_ID, $CONFIG_ASSET_EXTERNAL_ID or $CONFIG_ASSET_NAME must be specified, unless all properties of this asset are specified by their alias",
            "$CONFIG_ASSET_ID, $CONFIG_ASSET_NAME",
            this)

        if (allPropertiesByAlias) {
            ConfigurationException.check(
                numberOfSpecifiedAssetIdentifiers == 0,
                "All asset properties for this asset are specified by alias, $CONFIG_ASSET_ID, $CONFIG_ASSET_EXTERNAL_ID and $CONFIG_ASSET_NAME for the asset must be left empty",
                "$CONFIG_ASSET_ID, $CONFIG_ASSET_NAME",
                this
            )
        }

        ConfigurationException.check(
            (numberOfSpecifiedAssetIdentifiers == 1 || allPropertiesByAlias),
            "Just one of $CONFIG_ASSET_ID, $CONFIG_ASSET_EXTERNAL_ID or $CONFIG_ASSET_NAME must be specified",
            "$CONFIG_ASSET_ID, $CONFIG_ASSET_NAME",
            this)

        if (_assetID != null) {
            ConfigurationException.check(
                ID_REGEX.matches(_assetID!!), "$CONFIG_ASSET_ID \"$_assetID\" is not a valid identifier as it does not match the specifier $ID_REGEX_STR", CONFIG_ASSET_ID, this)
        }

        _properties.forEach {
            it.validate()
        }

        validated = true
    }

    companion object {
        const val CONFIG_ASSET_ID = "AssetId"
        const val CONFIG_ASSET_NAME = "AssetName"
        const val CONFIG_ASSET_EXTERNAL_ID = "AssetExternalId"
        private const val CONFIG_PROPERTIES = "Properties"

        private val default = AwsSiteWiseAssetConfiguration()

        fun create(assetId: String? = default._assetID,
                   assetName: String? = default._assetName,
                   assetExternalId: String? = default._assetExternalID,
                   properties: List<SiteWiseAssetPropertyConfiguration> = default._properties): AwsSiteWiseAssetConfiguration {

            val instance = AwsSiteWiseAssetConfiguration()
            with(instance) {
                _assetID = assetId
                _assetExternalID = assetExternalId
                _assetName = assetName
                _properties = properties
            }
            return instance
        }

    }

}