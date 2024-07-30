// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.awssitewise.config


import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.annotations.SerializedName

@ConfigurationClass
class AwsSiteWiseAssetCreationConfiguration {


    @SerializedName(CONFIG_ASSET_NAME)
    private var _assetName: String = DEFAULT_ASSET_NAME
    val assetName: String
        get() = _assetName

    @SerializedName(CONFIG_ASSET_DESCRIPTION)
    private var _assetDescription: String = DEFAULT_ASSET_DESCRIPTION
    val assetDescription: String
        get() = _assetDescription


    @SerializedName(CONFIG_ASSET_MODEL_NAME)
    private var _assetModelName: String = DEFAULT_ASSET_MODEL_NAME
    val assetModelName: String
        get() = _assetModelName

    @SerializedName(CONFIG_ASSET_MODEL_EXTERNAL_ID)
    private var _assetModelExternalID: String? = null
    val assetModelExternalID: String?
        get() = _assetModelExternalID

    @SerializedName(CONFIG_ASSET_EXTERNAL_ID)
    private var _assetExternalID: String? = null
    val assetExternalID: String?
        get() = _assetExternalID

    @SerializedName(CONFIG_ASSET_MODEL_DESCRIPTION)
    private var _assetModelDescription: String = DEFAULT_ASSET_MODEL_DESCRIPTION
    val assetModelDescription: String
        get() = _assetModelDescription

    @SerializedName(CONFIG_ASSET_MODEL_TAGS)
    private var _assetModelTags: Map<String, String> = emptyMap()
    val assetModelTags: Map<String, String>
        get() = _assetModelTags

    @SerializedName(CONFIG_ASSET_PROPERTY_NAME)
    private var _assetPropertyName: String = TEMPLATE_CHANNEL
    val assetPropertyName: String
        get() = _assetPropertyName

    @SerializedName(CONFIG_ASSET_PROPERTY_ALIAS)
    private var _assetPropertyAlias: String? = null
    val assetPropertyAlias: String?
        get() = _assetPropertyAlias

    @SerializedName(CONFIG_ASSET_MODEL_PROPERTY_EXTERNAL_ID)
    private var _assetModelPropertyExternalID: String? = null
    val assetModelPropertyExternalID: String?
        get() = _assetModelPropertyExternalID

    @SerializedName(CONFIG_ASSET_PROPERTY_METADATA_UNIT_NAME)
    private var _assetPropertyMetadataUnitName: String = DEFAULT_ASSET_PROPERTY_METADATA_UNIT_NAME
    val assetPropertyMetadataUnitName: String
        get() = _assetPropertyMetadataUnitName

    @SerializedName(CONFIG_ASSET_TIMESTAMP)
    private var _assetTimestamp: AssetTimestamp = DEFAULT_ASSET_TIMESTAMP
    val assetTimestamp: AssetTimestamp
        get() = _assetTimestamp

    @SerializedName(CONFIG_ASSET_TAGS)
    private var _assetTags: Map<String, String> = emptyMap()
    val assetTags: Map<String, String>
        get() = _assetTags


    companion object {
        const val CONFIG_ASSET_NAME = "AssetName"
        private const val CONFIG_ASSET_DESCRIPTION = "AssetDescription"
        private const val CONFIG_ASSET_MODEL_DESCRIPTION = "AssetModelDescription"
        private const val CONFIG_ASSET_MODEL_NAME = "AssetModelName"
        private const val CONFIG_ASSET_MODEL_EXTERNAL_ID = "AssetModelExternalID"
        const val CONFIG_ASSET_EXTERNAL_ID = "AssetExternalID"
        const val CONFIG_ASSET_PROPERTY_NAME = "AssetPropertyName"
        private const val CONFIG_ASSET_PROPERTY_ALIAS = "AssetPropertyAlias"
        const val CONFIG_ASSET_MODEL_PROPERTY_EXTERNAL_ID = "AssetModelPropertyExternalID"
        private const val CONFIG_ASSET_PROPERTY_METADATA_UNIT_NAME = "AssetPropertyMetadataUnitName"
        private const val CONFIG_ASSET_TIMESTAMP = "AssetPropertyTimestamp"

        const val TEMPLATE_PRE_POSTFIX = "%"
        const val TEMPLATE_SCHEDULE = "${TEMPLATE_PRE_POSTFIX}schedule${TEMPLATE_PRE_POSTFIX}"
        const val TEMPLATE_SOURCE = "${TEMPLATE_PRE_POSTFIX}source${TEMPLATE_PRE_POSTFIX}"
        const val TEMPLATE_TARGET = "${TEMPLATE_PRE_POSTFIX}target${TEMPLATE_PRE_POSTFIX}"
        const val TEMPLATE_CHANNEL = "${TEMPLATE_PRE_POSTFIX}channel${TEMPLATE_PRE_POSTFIX}"
        const val TEMPLATE_DATETIME = "${TEMPLATE_PRE_POSTFIX}datetime${TEMPLATE_PRE_POSTFIX}"
        const val TEMPLATE_UUID =  "${TEMPLATE_PRE_POSTFIX}uuid${TEMPLATE_PRE_POSTFIX}"
        const val TEMPLATE_ASSET_ID =  "${TEMPLATE_PRE_POSTFIX}assetid${TEMPLATE_PRE_POSTFIX}"

        private const val CONFIG_ASSET_MODEL_TAGS = "AssetModelTags"
        private const val CONFIG_ASSET_TAGS = "AssetTags"

        private const val DEFAULT_ASSET_NAME = "${TEMPLATE_TARGET}-${TEMPLATE_SCHEDULE}-${TEMPLATE_SOURCE}"
        private const val DEFAULT_ASSET_DESCRIPTION = "Asset for target $TEMPLATE_TARGET, schedule $TEMPLATE_SCHEDULE, source $TEMPLATE_SOURCE"
        const val DEFAULT_ASSET_MODEL_NAME = "${TEMPLATE_TARGET}-${TEMPLATE_SCHEDULE}-${TEMPLATE_SOURCE}-model"
        private const val DEFAULT_ASSET_MODEL_DESCRIPTION = "Asset model  for target $TEMPLATE_TARGET, schedule $TEMPLATE_SCHEDULE, source $TEMPLATE_SOURCE"

        private const val DEFAULT_ASSET_PROPERTY_METADATA_UNIT_NAME = "Unit"
        private val DEFAULT_ASSET_TIMESTAMP = AssetTimestamp.CHANNEL

        private val default = AwsSiteWiseAssetCreationConfiguration()

        fun create(
            assetModelName: String = default._assetModelName,
            assetName: String = default._assetName,
            assetExternalID : String? = default._assetModelExternalID,
            assetModelPropertyExternalID: String? = default._assetModelPropertyExternalID,
            assetModelTags: Map<String, String> = default._assetModelTags,
            assetTags: Map<String, String> = default._assetTags,
            assetPropertyName: String = default._assetPropertyName,
            assetTimestamp: AssetTimestamp = default._assetTimestamp,
            assetDescription: String = default._assetDescription,
            assetModelDescription: String = default._assetModelDescription,
            assetPropertyMetadataUnitName: String = default._assetPropertyMetadataUnitName
        ): AwsSiteWiseAssetCreationConfiguration {

            val instance = AwsSiteWiseAssetCreationConfiguration()
            with(instance) {
                _assetModelName = assetModelName
                _assetName = assetName
                _assetModelExternalID = assetExternalID
                _assetModelPropertyExternalID = assetModelPropertyExternalID
                _assetModelTags = assetModelTags
                _assetTags = assetTags
                _assetPropertyName = assetPropertyName
                _assetTimestamp = assetTimestamp
                _assetPropertyMetadataUnitName = assetPropertyMetadataUnitName
                _assetDescription = assetDescription
                _assetModelDescription = assetModelDescription
            }
            return instance
        }

    }
}

