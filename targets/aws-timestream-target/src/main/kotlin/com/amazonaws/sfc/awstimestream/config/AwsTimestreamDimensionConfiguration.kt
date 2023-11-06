
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.awstimestream.config

import com.amazonaws.sfc.awstimestream.config.AwsTimestreamWriterConfiguration.Companion.getExpression
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName
import io.burt.jmespath.Expression

@ConfigurationClass
class AwsTimestreamDimensionConfiguration : Validate {

    @SerializedName(CONFIG_DIMENSION_NAME)
    private var _dimensionName: String? = null


    val dimensionName: String
        get() = _dimensionName ?: ""


    @SerializedName(CONFIG_DIMENSION_VALUE_PATH)
    private var _dimensionValuePath: String? = null

    val dimensionValuePathStr: String?
        get() {
            return _dimensionValuePath
        }

    val dimensionValuePath: Expression<Any>?
        get() {
            return getExpression(_dimensionValuePath)
        }

    @SerializedName(CONFIG_DIMENSION_VALUE)
    private var _dimensionValue: Any? = null


    val dimensionValue: String?
        get() {
            return _dimensionValue?.toString()
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
            (listOfNotNull(_dimensionValue, _dimensionValuePath).count() == 1),
            "$CONFIG_DIMENSION_VALUE or $CONFIG_DIMENSION_VALUE_PATH must be specified",
            "$CONFIG_DIMENSION_VALUE, $CONFIG_DIMENSION_VALUE_PATH",
            this
        )

        ConfigurationException.check(
            (!_dimensionName.isNullOrEmpty()),
            "$CONFIG_DIMENSION_NAME must be specified",
            CONFIG_DIMENSION_NAME,
            this
        )

        validated = true

    }

    override fun toString(): String {
        return "AwsTimestreamDimensionConfiguration(dimensionName='$dimensionName', dimensionValuePath=$dimensionValuePathStr, dimensionValue='$dimensionValue')"
    }

    companion object {

        private const val CONFIG_DIMENSION_NAME = "DimensionName"
        private const val CONFIG_DIMENSION_VALUE_PATH = "DimensionValuePath"
        private const val CONFIG_DIMENSION_VALUE = "DimensionValue"

        private val default = AwsTimestreamDimensionConfiguration()

        fun create(dimensionName: String? = default._dimensionName,
                   dimensionValuePath: String? = default._dimensionValuePath,
                   dimensionValue: Any? = default._dimensionValue): AwsTimestreamDimensionConfiguration {

            val instance = AwsTimestreamDimensionConfiguration()
            with(instance) {
                _dimensionName = dimensionName
                _dimensionValuePath = dimensionValuePath
                _dimensionValue = dimensionValue
            }
            return instance
        }


    }

}