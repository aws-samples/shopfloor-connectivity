/*

Copyright (c) 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved. 
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.awssitewise.config

import com.amazonaws.sfc.awssitewise.SiteWiseDataType
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.amazonaws.sfc.data.JmesPathExtended
import com.amazonaws.sfc.data.JmesPathExtended.escapeJMesString
import com.google.gson.annotations.SerializedName
import io.burt.jmespath.Expression

@ConfigurationClass
class SiteWiseAssetPropertyConfiguration : Validate {

    @SerializedName(CONFIG_PROPERTY_ID)
    private var _propertyID: String? = null

    /**
     * ID of the property
     */
    val propertyID: String?
        get() = _propertyID

    @SerializedName(CONFIG_PROPERTY_ALIAS)
    private var _propertyAlias: String? = null

    /**
     * Alias of the property
     */
    val propertyAlias: String?
        get() = _propertyAlias

    @SerializedName(CONFIG_DATA_TYPE)
    private var _dataType: SiteWiseDataType = SiteWiseDataType.UNSPECIFIED

    val dataType: SiteWiseDataType
        get() {
            return _dataType
        }

    @SerializedName(CONFIG_DATA_PATH)
    private var _dataPath: String? = null

    val dataPathStr: String?
        get() {
            return _dataPath
        }

    val dataPath: Expression<Any>?
        get() {
            return getExpression(_dataPath)
        }

    @SerializedName(CONFIG_TIMESTAMP_PATH)
    private var _timestampPath: String? = null

    val timestampPathStr: String?
        get() {
            return _timestampPath
        }

    val timestampPath: Expression<Any>?
        get() {
            return getExpression(_timestampPath)
        }


    private fun getExpression(path: String?): Expression<Any>? {
        if (path.isNullOrEmpty()) {
            return null
        }

        val p: String = escapeJMesString(path)
        if (!cachedJmespathQueries.containsKey(p)) {
            cachedJmespathQueries[p] = try {
                jmespath.compile(if (p.startsWith("@.")) p else "@.$p")
            } catch (e: Throwable) {
                null
            }
        }
        return cachedJmespathQueries[p]
    }

    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    override fun validate() {

        if (validated) return

        ConfigurationException.check(
            (listOf(_propertyID, _propertyAlias).count { !it.isNullOrEmpty() } == 1),
            "$CONFIG_PROPERTY_ID or $CONFIG_PROPERTY_ALIAS must be specified",
            "$CONFIG_PROPERTY_ID,$CONFIG_PROPERTY_ALIAS",
            this
        )

        validateDataPath()
        validateTimestampPath()
        validated = true

    }

    private fun validateDataPath() {
        ConfigurationException.check(
            (!_dataPath.isNullOrEmpty()),
            "$CONFIG_DATA_PATH must be specified",
            CONFIG_DATA_PATH,
            this
        )

        ConfigurationException.check(
            (dataPath != null),
            "$CONFIG_DATA_PATH \"$_dataPath\" is not a valid JmesPath expression",
            CONFIG_DATA_PATH,
            this
        )
    }

    private fun validateTimestampPath() {
        if (!_timestampPath.isNullOrEmpty()) {

            ConfigurationException.check(
                (timestampPath != null),
                "$CONFIG_TIMESTAMP_PATH \"$_timestampPath\" is not a valid JmesPath expression",
                CONFIG_TIMESTAMP_PATH,
                this
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SiteWiseAssetPropertyConfiguration

        if (_propertyID != other._propertyID) return false

        return true
    }

    override fun hashCode(): Int {
        return (_propertyID ?: _propertyAlias).hashCode()
    }

    override fun toString(): String {
        return _propertyID ?: _propertyAlias ?: ""
    }

    companion object {

        private const val CONFIG_PROPERTY_ID = "PropertyId"
        private const val CONFIG_PROPERTY_ALIAS = "PropertyAlias"
        private const val CONFIG_DATA_TYPE = "DataType"
        private const val CONFIG_DATA_PATH = "DataPath"
        private const val CONFIG_TIMESTAMP_PATH = "TimestampPath"


        private val jmespath by lazy {
            JmesPathExtended.create()
        }

        // Caching compiled JMESPath queries
        private val cachedJmespathQueries = mutableMapOf<String, Expression<Any>?>()

        private val default = SiteWiseAssetPropertyConfiguration()

        fun create(propertyId: String? = default._propertyID,
                   propertyAlias: String? = default._propertyAlias,
                   dataType: SiteWiseDataType = default._dataType,
                   dataPath: String? = default._dataPath,
                   timestampPath: String? = default.timestampPathStr): SiteWiseAssetPropertyConfiguration {

            val instance = SiteWiseAssetPropertyConfiguration()
            with(instance) {
                _propertyID = propertyId
                _propertyAlias = propertyAlias
                _dataType = dataType
                _dataPath = dataPath
                _timestampPath = timestampPath
            }
            return instance
        }

    }

}