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

import com.google.gson.annotations.SerializedName
import software.amazon.awssdk.services.timestreamwrite.model.MeasureValueType

/**
 * Data types supported by timestream, wrapped in SFC type for deserialization
 */
@Suppress("unused", "unused", "unused")
enum class AwsTimestreamDataType(val timestreamType: MeasureValueType) {
    @SerializedName(TIMESTREAM_DATATYPE_DOUBLE)
    DOUBLE(MeasureValueType.DOUBLE),

    @SerializedName(TIMESTREAM_DATATYPE_BIGINT)
    BIGINT(MeasureValueType.BIGINT),

    @SerializedName(TIMESTREAM_DATATYPE_VARCHAR)
    VARCHAR(MeasureValueType.VARCHAR),

    @SerializedName(TIMESTREAM_DATATYPE_BOOLEAN)
    BOOLEAN(MeasureValueType.BOOLEAN)
}

const val TIMESTREAM_DATATYPE_DOUBLE = "DOUBLE"
const val TIMESTREAM_DATATYPE_BIGINT = "BIGINT"
const val TIMESTREAM_DATATYPE_VARCHAR = "VARCHAR"
const val TIMESTREAM_DATATYPE_BOOLEAN = "BOOLEAN"

val TIMESTREAM_VALID_TYPES = listOf(TIMESTREAM_DATATYPE_DOUBLE, TIMESTREAM_DATATYPE_BIGINT, TIMESTREAM_DATATYPE_VARCHAR, TIMESTREAM_DATATYPE_BOOLEAN)