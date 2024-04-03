
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject
import java.time.Instant

@ConfigurationClass
@TransformerOperator(["IsoDateTimeStrToEpocMilliSeconds"])
class IsoDateTimeStrToEpocMilliSeconds : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: String?): Long? =

        if (target == null) null else
            try {
                Instant.parse(target).toEpochMilli()
            } catch (_: IllegalArgumentException) {
                null
            }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<IsoDateTimeStrToEpocMilliSeconds>(o)
        fun create() = IsoDateTimeStrToEpocMilliSeconds()
    }
}