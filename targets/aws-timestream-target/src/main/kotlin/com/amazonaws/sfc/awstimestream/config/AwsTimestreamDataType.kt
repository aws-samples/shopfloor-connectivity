
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


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