// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sfc.awssitewise.config

import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.system.DateTime.systemDateTime
import com.google.gson.annotations.SerializedName
import io.burt.jmespath.Expression

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
    private var _assetModelName: String = "${TEMPLATE_TARGET}-${TEMPLATE_SCHEDULE}-${TEMPLATE_SOURCE}-model"
    val assetModelName: String
        get() = _assetModelName

    @SerializedName(CONFIG_ASSET_MODEL_DESCRIPTION)
    private var _assetModelDescription: String = DEFAULT_ASSET_MODEL_DESCRIPTION
    val assetModelDescription: String
        get() = _assetModelDescription

    @SerializedName(CONFIG_ASSET_PROPERTY_NAME)
    private var _assetPropertyName: String = TEMPLATE_CHANNEL
    val assetPropertyName: String
        get() = _assetPropertyName

    @SerializedName(CONFIG_ASSET_PROPERTY_METADATA_UNIT_NAME)
    private var _assetPropertyMetadataUnitName: String = DEFAULT_ASSET_PROPERTY_METADATA_UNIT_NAME
    val assetPropertyMetadataUnitName: String
        get() = _assetPropertyMetadataUnitName


    fun renderAssetName(target: String, schedule: String, source: String,  metadata: Map<String, String>?): String = renderTemplate(assetName, target, schedule, source, metadata)

    fun renderAssetDescription(target: String, schedule: String, source: String,  metadata: Map<String, String>?): String = renderTemplate(assetDescription, target, schedule, source, metadata,true)

    fun renderAssetModelName(target: String, schedule: String, source: String,  metadata: Map<String, String>?): String = renderTemplate(assetModelName, target, schedule,  source, metadata)

    fun renderAssetModelDescription(target: String, schedule: String, source: String,  metadata: Map<String, String>?): String =
        renderTemplate(assetModelDescription, target, schedule, source, metadata, true)

    fun renderAssetPropertyName(target: String, schedule: String, source: String, channel: String,  metadata: Map<String, String>?): String =
        renderTemplate(assetPropertyName, target, schedule, source, channel, metadata)


    companion object {
        private const val CONFIG_ASSET_NAME = "AssetName"
        private const val CONFIG_ASSET_DESCRIPTION = "AssetDescription"
        private const val CONFIG_ASSET_MODEL_DESCRIPTION = "AssetModelDescription"
        private const val CONFIG_ASSET_MODEL_NAME = "AssetModelName"
        private const val CONFIG_ASSET_PROPERTY_NAME = "AssetPropertyName"
        private const val CONFIG_ASSET_PROPERTY_METADATA_UNIT_NAME = "AssetPropertyMetadataUnitName"

        private const val TEMPLATE_PRE_POSTFIX = "%"
        private const val TEMPLATE_SCHEDULE = "${TEMPLATE_PRE_POSTFIX}Schedule${TEMPLATE_PRE_POSTFIX}"
        private const val TEMPLATE_SOURCE = "${TEMPLATE_PRE_POSTFIX}Source${TEMPLATE_PRE_POSTFIX}"
        private const val TEMPLATE_TARGET = "${TEMPLATE_PRE_POSTFIX}Target${TEMPLATE_PRE_POSTFIX}"
        private const val TEMPLATE_CHANNEL = "${TEMPLATE_PRE_POSTFIX}Channel${TEMPLATE_PRE_POSTFIX}"
        private const val TEMPLATE_DATETIME = "${TEMPLATE_PRE_POSTFIX}DateTime${TEMPLATE_PRE_POSTFIX}"

        private const val DEFAULT_ASSET_NAME = "${TEMPLATE_TARGET}-${TEMPLATE_SCHEDULE}-${TEMPLATE_SOURCE}"
        private const val DEFAULT_ASSET_DESCRIPTION = "Asset for target $TEMPLATE_TARGET, schedule $TEMPLATE_SCHEDULE, source $TEMPLATE_SOURCE"
        private const val DEFAULT_ASSET_MODEL_DESCRIPTION = "Asset model  for target $TEMPLATE_TARGET, schedule $TEMPLATE_SCHEDULE, source $TEMPLATE_SOURCE"

        private const val DEFAULT_ASSET_PROPERTY_METADATA_UNIT_NAME = "Unit"

        private val default = AwsSiteWiseAssetCreationConfiguration()

        fun create(
            assetModelName: String = default._assetModelName,
            assetName: String = default._assetName,
            assetPropertyName: String = default._assetPropertyName
        ): AwsSiteWiseAssetCreationConfiguration {

            val instance = AwsSiteWiseAssetCreationConfiguration()
            with(instance) {
                _assetModelName = assetModelName
                _assetName = assetName
                _assetPropertyName = assetPropertyName
            }
            return instance
        }

        fun renderTemplate(
            template: String,
            target: String,
            schedule: String,
            source: String,
            metadata: Map<String, String>?,
            useDateTime: Boolean = true
        ): String {
            var s = template
                .replace(TEMPLATE_SCHEDULE, schedule.replace(TEMPLATE_PRE_POSTFIX, ""))
                .replace(TEMPLATE_SOURCE, source.replace(TEMPLATE_PRE_POSTFIX, ""))
                .replace(TEMPLATE_TARGET, target.replace(TEMPLATE_PRE_POSTFIX, ""))
            if (useDateTime) {
                s = s.replace(TEMPLATE_DATETIME, systemDateTime().toString())
            }
            if (metadata != null) {
                for (entry in metadata) {
                    s = s.replace("${TEMPLATE_PRE_POSTFIX}entry.key${TEMPLATE_PRE_POSTFIX}", entry.value.replace(TEMPLATE_PRE_POSTFIX, ""))
                }
            }
            return s
        }

        fun renderTemplate(
            template: String,
            target: String,
            schedule: String,
            source: String,
            channel: String,
            metadata: Map<String, String>?,
            useDateTime: Boolean = true
        ): String {
            return renderTemplate(template, target, schedule, source, metadata, useDateTime)
                .replace(TEMPLATE_CHANNEL, channel.replace(TEMPLATE_PRE_POSTFIX, ""))
        }
    }

}