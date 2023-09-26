/*
 Copyright (c) 2022. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

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