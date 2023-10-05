/*
 Copyright (c) 2023. Amazon.com, Inc. or its affiliates. All Rights Reserved.
 Licensed under the Amazon Software License (the "License"). You may not use this file except in
 compliance with the License. A copy of the License is located at :

     http://aws.amazon.com/asl/

 or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES  OR CONDITIONS OF ANY KIND, express or implied. See the License for the specific
 language governing permissions and limitations under the License.

 */

package com.amazonaws.sfc.awstimestream.config

import com.amazonaws.sfc.awstimestream.config.AwsTimestreamWriterConfiguration.Companion.getExpression
import com.amazonaws.sfc.config.ConfigurationClass
import com.amazonaws.sfc.config.ConfigurationException
import com.amazonaws.sfc.config.Validate
import com.google.gson.annotations.SerializedName
import io.burt.jmespath.Expression
import software.amazon.awssdk.services.timestreamwrite.model.MeasureValueType


@ConfigurationClass
class AwsTimestreamRecordConfiguration : Validate {

    @SerializedName(CONFIG_RECORD_MEASURE_NAME)
    private var _measureName: String? = null

    val measureName: String
        get() = _measureName ?: ""

    @SerializedName(CONFIG_RECORD_MEASURE_VALUE_PATH)
    private var _measureValuePath: String? = null

    val measureValuePathStr: String?
        get() {
            return _measureValuePath
        }

    val measureValuePath: Expression<Any>?
        get() {
            return getExpression(_measureValuePath)
        }

    @SerializedName(CONFIG_RECORD_MEASURE_VALUE_TYPE)
    private var _measureDataType: AwsTimestreamDataType? = null

    val measureValueType: MeasureValueType
        get() = _measureDataType?.timestreamType ?: MeasureValueType.UNKNOWN_TO_SDK_VERSION


    @SerializedName(CONFIG_RECORD_MEASURE_TIME_PATH)
    private var _measureTimePath: String? = null

    val measureTimePathStr: String?
        get() {
            return _measureTimePath
        }

    val measureTimePath: Expression<Any>?
        get() {
            return getExpression(_measureTimePath)
        }

    @SerializedName(CONFIG_RECORD_DIMENSIONS)
    private var _dimensions: List<AwsTimestreamDimensionConfiguration> = emptyList()

    val dimensions
        get() = _dimensions

    private var _validated = false
    override var validated
        get() = _validated
        set(value) {
            _validated = value
        }

    override fun validate() {
        if (validated) return
        validateMeasureName()
        validateMeasureValuePath()
        validateMeasureTimePath()
        validateMeasureDataType()
        validateDimensions()
        validated = true
    }

    private fun validateMeasureName() {
        ConfigurationException.check(
            (measureName.isNotBlank()),
            "$CONFIG_RECORD_MEASURE_NAME must be specified",
            CONFIG_RECORD_MEASURE_NAME,
            this
        )
    }

    private fun validateMeasureValuePath() {

        ConfigurationException.check(
            (!_measureValuePath.isNullOrEmpty()),
            "$CONFIG_RECORD_MEASURE_VALUE_PATH must be specified",
            CONFIG_RECORD_MEASURE_VALUE_PATH,
            this
        )

        ConfigurationException.check(
            (measureValuePath != null),
            "$CONFIG_RECORD_MEASURE_VALUE_PATH  \"$_measureValuePath\" is not a valid JmesPath expression",
            CONFIG_RECORD_MEASURE_VALUE_PATH,
            this
        )
    }

    private fun validateMeasureTimePath() {
        if (!_measureTimePath.isNullOrEmpty()) {

            ConfigurationException.check(
                (measureTimePath != null),
                "$CONFIG_RECORD_MEASURE_TIME_PATH \"$_measureTimePath\" is not a valid JmesPath expression",
                CONFIG_RECORD_MEASURE_TIME_PATH,
                this
            )
        }
    }

    private fun validateDimensions() {

        ConfigurationException.check(
            (_dimensions.size <= 128),
            "A maximum of 128 dimensions can be configured for a record",
            CONFIG_RECORD_DIMENSIONS,
            this
        )

        dimensions.forEach {
            it.validate()
        }

    }

    private fun validateMeasureDataType() {

        ConfigurationException.check(
            (measureValueType != MeasureValueType.UNKNOWN_TO_SDK_VERSION),
            "$CONFIG_RECORD_MEASURE_VALUE_TYPE \"$_measureDataType\" is not a valid Timestream datatype, valid types are ${TIMESTREAM_VALID_TYPES.joinToString()}",
            CONFIG_RECORD_MEASURE_VALUE_TYPE,
            this
        )
    }

    override fun toString(): String {
        return "{AwsTimestreamRecordConfiguration(measureName='$measureName', " +
               "measureValuePath=$measureValuePathStr, " +
               "measureDataType=$measureValueType, " +
               "measureTimePath=$measureTimePathStr, " +
               "dimensions=$dimensions}"
    }


    companion object {

        private const val CONFIG_RECORD_MEASURE_NAME = "MeasureName"
        private const val CONFIG_RECORD_MEASURE_VALUE_PATH = "MeasureValuePath"
        private const val CONFIG_RECORD_MEASURE_TIME_PATH = "MeasureTimePath"
        private const val CONFIG_RECORD_MEASURE_VALUE_TYPE = "MeasureValueType"
        private const val CONFIG_RECORD_DIMENSIONS = "MetricDimensions"

        private val default = AwsTimestreamRecordConfiguration()

        fun create(measureName: String? = default._measureName,
                   measureValuePath: String? = default._measureValuePath,
                   measureValueType: AwsTimestreamDataType? = default._measureDataType,
                   measureTimePath: String? = default._measureTimePath,
                   dimensions: List<AwsTimestreamDimensionConfiguration> = default._dimensions): AwsTimestreamRecordConfiguration {

            val instance = AwsTimestreamRecordConfiguration()
            with(instance) {
                _measureName = measureName
                _measureValuePath = measureValuePath
                _measureDataType = measureValueType
                _measureTimePath = measureTimePath
                _dimensions = dimensions
            }
            return instance
        }
    }


}