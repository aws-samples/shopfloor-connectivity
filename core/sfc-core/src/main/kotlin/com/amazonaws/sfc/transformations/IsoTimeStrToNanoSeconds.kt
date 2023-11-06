
// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0


package com.amazonaws.sfc.transformations

import com.amazonaws.sfc.config.ConfigurationClass
import com.google.gson.JsonObject
import kotlin.time.Duration
import kotlin.time.DurationUnit

@ConfigurationClass
@TransformerOperator(["IsoTimeStrToNanoSeconds"])
class IsoTimeStrToNanoSeconds : TransformationImpl<Nothing>() {

    @TransformerMethod
    fun apply(target: String?): Double? =

        if (target == null) null else
            try {
                Duration.parseIsoString(target).toDouble(DurationUnit.NANOSECONDS)
            } catch (_: IllegalArgumentException) {
                null
            }

    companion object {
        fun fromJson(o: JsonObject): TransformationOperator = TransformationOperatorNoOperand.fromJson<IsoTimeStrToNanoSeconds>(o)
        fun create() = IsoTimeStrToNanoSeconds()
    }
}