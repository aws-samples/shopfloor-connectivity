// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awssitewise.config

import com.amazonaws.sfc.awssitewise.SiteWiseDataType
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseTargetConfiguration.Companion.ID_REGEX
import com.amazonaws.sfc.awssitewise.config.AwsSiteWiseTargetConfiguration.Companion.ID_REGEX_STR
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

    val propertyID: String?
        get() = _propertyID


    @SerializedName(CONFIG_PROPERTY_NAME)
    private var _propertyName: String? = null

    val propertyName: String?
        get() = _propertyName


    @SerializedName(CONFIG_PROPERTY_ALIAS)
    private var _propertyAlias: String? = null

    val propertyAlias: String?
        get() = _propertyAlias

    @SerializedName(CONFIG_PROPERTY_EXTERNAL_ID)
    private var _propertyExternalID: String? = null
    val propertyExternalID: String?
        get() = _propertyExternalID

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

    @SerializedName(CONFIG_WARN_IF_NOT_PRESENT)
    private var _warnIfNotPresent: Boolean = true
    val warnIfNotPresent: Boolean
        get() = _warnIfNotPresent

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

    val asString
        get() = if (_propertyID != null) _propertyID
        else if (_propertyName != null) _propertyName
        else if (_propertyExternalID != null) _propertyExternalID
        else _propertyAlias


    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    override fun validate() {

        if (validated) return

        validatePropertyID()

        validateDataPath()
        validateTimestampPath()
        validated = true

    }

    private fun validatePropertyID() {
        ConfigurationException.check(
            ((listOf(_propertyID, _propertyAlias, _propertyName, _propertyExternalID).count { !it.isNullOrEmpty() }) != 0),
            "$CONFIG_PROPERTY_ID, $CONFIG_PROPERTY_NAME, $CONFIG_PROPERTY_EXTERNAL_ID or $CONFIG_PROPERTY_ALIAS must be specified",
            "$CONFIG_PROPERTY_ID,$CONFIG_PROPERTY_ALIAS,$CONFIG_PROPERTY_EXTERNAL_ID,$CONFIG_PROPERTY_ALIAS",
            this)

        ConfigurationException.check((listOf(_propertyID, _propertyAlias, _propertyExternalID, _propertyName).count { !it.isNullOrEmpty() } == 1),
            "Only one of $CONFIG_PROPERTY_ID, $CONFIG_PROPERTY_NAME or $CONFIG_PROPERTY_ALIAS must be specified",
            "$CONFIG_PROPERTY_ID,$CONFIG_PROPERTY_ALIAS,$CONFIG_PROPERTY_ALIAS",
            this)


        if (_propertyID != null) {
            ConfigurationException.check(
                ID_REGEX.matches(_propertyID!!), "$CONFIG_PROPERTY_ID \"$_propertyID\" is not a valid identifier as it does not match the specifier $ID_REGEX_STR", CONFIG_PROPERTY_ID, this)
        }
    }

    private fun validateDataPath() {
        ConfigurationException.check(
            (!_dataPath.isNullOrEmpty()), "$CONFIG_DATA_PATH must be specified", CONFIG_DATA_PATH, this)

        ConfigurationException.check(
            (dataPath != null), "$CONFIG_DATA_PATH \"$_dataPath\" is not a valid JmesPath expression", CONFIG_DATA_PATH, this)
    }

    private fun validateTimestampPath() {
        if (!_timestampPath.isNullOrEmpty()) {

            ConfigurationException.check(
                (timestampPath != null), "$CONFIG_TIMESTAMP_PATH \"$_timestampPath\" is not a valid JmesPath expression", CONFIG_TIMESTAMP_PATH, this)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SiteWiseAssetPropertyConfiguration

        return _propertyID == other._propertyID
    }

    override fun hashCode(): Int {
        return (_propertyID ?: _propertyAlias).hashCode()
    }

    override fun toString(): String {
        return _propertyID ?: _propertyAlias ?: ""
    }

    companion object {

        const val CONFIG_PROPERTY_ID = "PropertyId"
        const val CONFIG_PROPERTY_NAME = "PropertyName"
        const val CONFIG_PROPERTY_ALIAS = "PropertyAlias"
        const val CONFIG_PROPERTY_EXTERNAL_ID = "PropertyExternalId"
        private const val CONFIG_DATA_TYPE = "DataType"
        private const val CONFIG_DATA_PATH = "DataPath"
        private const val CONFIG_WARN_IF_NOT_PRESENT = "WarnIfNotPresent"
        const val CONFIG_TIMESTAMP_PATH = "TimestampPath"


        private val jmespath by lazy {
            JmesPathExtended.create()
        }

        // Caching compiled JMESPath queries
        private val cachedJmespathQueries = mutableMapOf<String, Expression<Any>?>()

        private val default = SiteWiseAssetPropertyConfiguration()

        fun create(propertyId: String? = default._propertyID,
                   propertyName: String? = default._propertyName,
                   propertyAlias: String? = default._propertyAlias,
                   propertyExternalId: String? = default._propertyExternalID,
                   dataType: SiteWiseDataType = default._dataType,
                   dataPath: String? = default._dataPath,
                   warnIfNotPresent: Boolean = default._warnIfNotPresent,
                   timestampPath: String? = default.timestampPathStr): SiteWiseAssetPropertyConfiguration {

            val instance = SiteWiseAssetPropertyConfiguration()
            with(instance) {
                _propertyID = propertyId
                _propertyName = propertyName
                _propertyAlias = propertyAlias
                _propertyExternalID = propertyExternalId
                _dataType = dataType
                _dataPath = dataPath
                _warnIfNotPresent = warnIfNotPresent
                _timestampPath = timestampPath
            }
            return instance
        }

        fun getExpression(path: String?): Expression<Any>? {
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

    }

}